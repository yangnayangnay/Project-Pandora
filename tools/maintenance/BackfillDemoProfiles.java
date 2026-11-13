import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** One-off, explicitly authorized demo data repair. Never loaded by application startup. */
public class BackfillDemoProfiles {
    private static final Map<Long, String> EXPECTED = Map.ofEntries(
            Map.entry(1L, "admin0"), Map.entry(2L, "000001"), Map.entry(3L, "000002"),
            Map.entry(4L, "000003"), Map.entry(5L, "000004"), Map.entry(6L, "000005"),
            Map.entry(7L, "000006"), Map.entry(100L, "000007"), Map.entry(101L, "000008"),
            Map.entry(102L, "000009"), Map.entry(103L, "000010"), Map.entry(104L, "000011"));
    private record Change(long id, String username, String identity, LocalDate birthday) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && !(args.length == 3 && "--apply-demo-profiles".equals(args[2])))
            throw new IllegalArgumentException("Usage: URL [new-backup.zip --apply-demo-profiles]; otherwise preview only");
        if (!args[0].contains("IFEXISTS=TRUE"))
            throw new IllegalArgumentException("IFEXISTS=TRUE required; must not create a replacement database");
        try (Connection db = DriverManager.getConnection(args[0], "sa", "")) {
            var initial = snapshot(db, false);
            verifyAccounts(initial);
            var preview = plan(db, initial);
            System.out.println("PREVIEW: known_demo_accounts=12; accounts_to_fill=" + preview.size());
            if (args.length == 1 || preview.isEmpty()) return;
            Path backup = Path.of(args[1]).toAbsolutePath();
            if (Files.exists(backup)) throw new IllegalStateException("Backup already exists; refusing overwrite");
            Files.createDirectories(backup.getParent());
            try (Statement s = db.createStatement()) {
                s.execute("BACKUP TO '" + backup.toString().replace("'", "''") + "'");
            }
            db.setAutoCommit(false);
            try {
                var before = snapshot(db, true);
                verifyAccounts(before);
                var changes = plan(db, before);
                for (Change change : changes) {
                    try (PreparedStatement p = db.prepareStatement("UPDATE users SET id_card=?,birthday=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND username=? AND deleted=0")) {
                        p.setString(1, change.identity()); p.setDate(2, java.sql.Date.valueOf(change.birthday()));
                        p.setLong(3, change.id()); p.setString(4, change.username());
                        if (p.executeUpdate() != 1) throw new IllegalStateException("Account changed during repair");
                    }
                }
                verifyOnlyPlannedFieldsChanged(before, snapshot(db, false), changes);
                db.commit();
                System.out.println("COMMITTED: filled_accounts=" + changes.size()
                        + "; required_profiles_complete=12/12; admins=1; all_passwords_and_other_user_columns_unchanged=true");
            } catch (Exception e) { db.rollback(); throw e; }
        }
    }

    private static Map<Long, Map<String, String>> snapshot(Connection db, boolean lock) throws SQLException {
        Map<Long, Map<String, String>> rows = new TreeMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM users ORDER BY id" + (lock ? " FOR UPDATE" : ""))) {
            ResultSetMetaData meta = r.getMetaData();
            while (r.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int col = 1; col <= meta.getColumnCount(); col++)
                    row.put(meta.getColumnLabel(col).toLowerCase(Locale.ROOT), r.getString(col));
                rows.put(r.getLong("id"), row);
            }
        }
        return rows;
    }

    private static void verifyAccounts(Map<Long, Map<String, String>> rows) {
        int active = 0, administrators = 0;
        for (var entry : rows.entrySet()) {
            var row = entry.getValue();
            if ("ADMIN".equals(row.get("role"))) administrators++;
            if (!"0".equals(row.get("deleted"))) continue;
            active++;
            if (!Objects.equals(EXPECTED.get(entry.getKey()), row.get("username")))
                throw new IllegalStateException("Unexpected account; this repair is restricted to the confirmed demo dataset");
            for (String required : List.of("username", "real_name", "email", "phone"))
                if (blank(row.get(required))) throw new IllegalStateException("Unexpected missing field: " + required);
        }
        if (active != 12 || administrators != 1 || !"ADMIN".equals(rows.get(1L).get("role"))
                || !"FOUNDER".equals(rows.get(100L).get("role")))
            throw new IllegalStateException("Demo account/administrator preconditions changed");
    }

    private static List<Change> plan(Connection db, Map<Long, Map<String, String>> rows) throws SQLException {
        List<Change> changes = new ArrayList<>();
        Set<String> allocated = new HashSet<>();
        for (var entry : rows.entrySet()) {
            var row = entry.getValue(); long id = entry.getKey();
            if (!"0".equals(row.get("deleted")) || !blank(row.get("id_card")) && !blank(row.get("birthday"))) continue;
            String identity = row.get("id_card");
            LocalDate birthday;
            if (!blank(row.get("birthday"))) birthday = LocalDate.parse(row.get("birthday"));
            else if (!blank(identity)) birthday = LocalDate.parse(identity.substring(6, 14), DateTimeFormatter.BASIC_ISO_DATE);
            else birthday = LocalDate.of(1990 + (int)(id % 12), (int)(id % 12) + 1, (int)(id % 20) + 1);
            if (blank(identity)) {
                // Explicitly fictional address prefix; these are not real identity documents.
                String body = "000000" + birthday.format(DateTimeFormatter.BASIC_ISO_DATE) + String.format(Locale.ROOT, "%03d", id);
                int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
                int sum = 0;
                for (int i = 0; i < body.length(); i++) sum += (body.charAt(i) - '0') * weights[i];
                identity = body + "10X98765432".charAt(sum % 11);
                if (!allocated.add(identity)) throw new IllegalStateException("Duplicate planned test identity");
                try (PreparedStatement p = db.prepareStatement("SELECT id FROM users WHERE UPPER(id_card)=? UNION ALL SELECT id FROM employee_records WHERE UPPER(id_card)=?")) {
                    p.setString(1, identity); p.setString(2, identity);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) throw new IllegalStateException("Test identity already used; no data changed");
                    }
                }
            }
            changes.add(new Change(id, row.get("username"), identity, birthday));
        }
        return changes;
    }

    private static void verifyOnlyPlannedFieldsChanged(Map<Long, Map<String, String>> before,
            Map<Long, Map<String, String>> after, List<Change> changes) {
        if (!before.keySet().equals(after.keySet())) throw new IllegalStateException("Account IDs changed");
        Map<Long, Change> changed = new HashMap<>();
        changes.forEach(c -> changed.put(c.id(), c));
        for (var entry : before.entrySet()) {
            long id = entry.getKey(); var old = entry.getValue(); var current = after.get(id); var change = changed.get(id);
            for (String key : old.keySet()) {
                if (change != null && Set.of("id_card", "birthday", "updated_at").contains(key)) continue;
                if (!Objects.equals(old.get(key), current.get(key))) throw new IllegalStateException("Unexpected field change: " + key);
            }
            for (String key : List.of("id_card", "birthday"))
                if (!blank(old.get(key)) && !Objects.equals(old.get(key), current.get(key)))
                    throw new IllegalStateException("Existing profile data overwritten");
            if (change != null && (!Objects.equals(change.identity(), current.get("id_card"))
                    || !Objects.equals(change.birthday().toString(), current.get("birthday"))))
                throw new IllegalStateException("Profile read-back mismatch");
            if ("0".equals(current.get("deleted")) && (blank(current.get("id_card")) || blank(current.get("birthday"))))
                throw new IllegalStateException("An active demo profile is still incomplete");
        }
        verifyAccounts(after);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
