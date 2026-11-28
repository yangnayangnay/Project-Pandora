## ADDED Requirements

### Requirement: Notification toolbar actions
The notification screen SHALL place refresh at the top right with an adjacent action labelled “一键已读”. Both actions SHALL have accessible labels and touch targets of at least 48dp. Request progress and failures SHALL be visible, and duplicate submissions SHALL be prevented.

#### Scenario: Refresh notifications
- **WHEN** the user taps the top-right refresh action
- **THEN** the list is refreshed without marking any notification as read

#### Scenario: No unread notifications
- **WHEN** the loaded list contains no unread notifications
- **THEN** “一键已读” remains visible and is disabled

### Requirement: Account-scoped bulk read
The system SHALL mark the authenticated user's notifications existing at the beginning of the bulk operation as read, leaving other accounts and notifications arriving after that snapshot unchanged. Repeating the action SHALL be harmless.

#### Scenario: Successful bulk read
- **WHEN** the user taps “一键已读” and the server confirms success
- **THEN** the relevant list records, unread badge, and corresponding system notifications are updated consistently

#### Scenario: Concurrent notification arrival
- **WHEN** a new notification arrives after the bulk operation's snapshot
- **THEN** it remains unread and eligible for a later alert

#### Scenario: Bulk read fails
- **WHEN** the request fails or times out
- **THEN** local read states and the unread badge are not falsely cleared, an error is shown, and retry is available

#### Scenario: Late delivery response
- **WHEN** an older delivery response contains a record already confirmed read by the bulk action
- **THEN** that record does not produce a new system alert
