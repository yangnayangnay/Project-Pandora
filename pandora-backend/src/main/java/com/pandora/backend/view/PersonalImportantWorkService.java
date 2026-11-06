package com.pandora.backend.view;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** The saved work list shared by the personal editor and the mind-map overview. */
@Service
public class PersonalImportantWorkService {
    private final JdbcTemplate db;

    public PersonalImportantWorkService(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> list(long userId) {
        return db.queryForList(
                "SELECT * FROM top10_works WHERE user_id = ? AND work_type = 'PERSONAL_IMPORTANT' " +
                        "AND deleted = 0 ORDER BY rank_order, id LIMIT 10", userId);
    }
}
