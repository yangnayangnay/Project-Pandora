# personnel-audit Specification

## Purpose
TBD - created by archiving change notification-actions-and-handover-audit. Update Purpose after archive.

## Requirements

### Requirement: Visible departure handover audit
The administrator system log SHALL display a departure handover's leaving person, position successor, unfinished-task recipient, and number of transferred tasks with readable labels. Existing handover records SHALL remain visible without a data migration. Non-administrators SHALL NOT gain access to the system log.

#### Scenario: View a handover record
- **WHEN** an administrator views a “离职工作交接” record
- **THEN** the participants and task count are visible in labelled fields rather than only a truncated tooltip

#### Scenario: Separate successors
- **WHEN** the position successor differs from the task recipient
- **THEN** both recipients are shown independently

#### Scenario: Filter or export
- **WHEN** the administrator filters by “离职工作交接” or exports matching records
- **THEN** the corresponding records and full original audit text are retained

#### Scenario: Historical record lacks a field
- **WHEN** a legacy record cannot be fully parsed
- **THEN** its original text is shown and missing information is not invented
