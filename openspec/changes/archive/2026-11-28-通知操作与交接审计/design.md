# Design

## Context

See proposal.md for scope and user stories. NotificationsActivity builds its UI in Java; unread counts and delivery cursors are already account-scoped. The backend already records transactional USER_HANDOVER events, and the admin list already supports that filter, but displays long text with a tooltip.

## Goals / Non-Goals

Goals: accessible toolbar actions, authenticated and retryable bulk read, no accidental consumption of newly arriving messages, readable historical handover records.
Non-goals: APK generation or device installation; changing departure rules, altering historical audit records, adding push providers.

## Decisions

- Use two always-visible toolbar menu actions, with an icon for refresh and text for bulk read. This preserves space without a separate full-width row. All newly introduced Android text uses string resources.
- Add PUT /notification/read-all. Capture the current user's maximum ID, update only unread rows at or below that boundary, and return readThroughId, updatedCount and remaining unreadCount. A single transaction is used; no client-supplied user ID is accepted. Rejected alternative: client-side one-request-per-row, which can partially succeed and miss unloaded rows.
- After success, mark local rows only through that boundary and persist an account-scoped read watermark under the delivery lock before cancelling matching system alerts. The receiver skips old snapshots through that watermark. A restored database resets an obsolete watermark along with an ahead cursor.
- Cancel an old list load when starting bulk read and disable conflicting actions during the operation. Failure never optimistically clears records. Refresh the unread count from the server after confirmation, so concurrently arriving notifications remain visible.
- Parse the existing labelled audit text into display-only fields. Show labelled summaries in the log table, retain original text as fallback and for CSV. Rejected alternative: migrate stored audit text or change the database schema for a presentation improvement.

## Risks / Trade-offs

- Concurrent arrivals → ID boundary plus regression test injecting a later notification between snapshot and update.
- Late delivery response → shared delivery lock and read watermark.
- Legacy audit format differences → conservative parser and original text fallback.
- No device run → compile app and instrumented-test sources only; report device verification as not performed.

## Migration Plan

No schema migration. Update backend before the next mobile installation. Web changes can be released separately. Rollback reverts application files and does not restore or overwrite user data.
