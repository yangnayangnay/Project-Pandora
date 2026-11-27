# audit Specification

## Purpose
TBD - created by archiving change backfill-git-history. Update Purpose after archive.

## Requirements

### Requirement: Audit Log Localization
The system SHALL support Chinese display for audit log operation types.

#### Scenario: Display operation type in Chinese
GIVEN an admin views the audit log
WHEN they see the operation type column
THEN the operation type is displayed in Chinese (e.g., "创建", "修改", "删除")

### Requirement: Audit Log Full Coverage
The system SHALL log all sensitive operations across all endpoints.

#### Scenario: Audit log records all operations
GIVEN a user performs any sensitive operation
WHEN the operation completes
THEN an audit log entry is created with operator, type, timestamp, and details

### Requirement: Operator ID Display
The system SHALL display operator ID correctly in audit logs.

#### Scenario: Display operator ID
GIVEN an admin views the audit log
WHEN they see the operator column
THEN the operator ID is displayed as "1" instead of "1.0"
