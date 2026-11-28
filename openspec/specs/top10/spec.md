# top10 Specification

## Purpose
十大重要工作能力支持重要工作的加权排序与展示。主要功能包括去重、加权排序、自动清理测试脏数据、账号信息条目。为用户提供清晰的重要工作视图。

## Requirements

### Requirement: Top10 Deduplication
The system SHALL deduplicate top 10 important work entries.

#### Scenario: Deduplicate entries
GIVEN duplicate entries exist
WHEN the system processes top 10 list
THEN duplicates are removed

### Requirement: Weighted Sorting
The system SHALL support weighted sorting for top 10 entries.

#### Scenario: Sort by weight
GIVEN a user views top 10 list
WHEN the sorting is applied
THEN entries are sorted by weight

### Requirement: Auto Cleanup
The system SHALL auto-clean test dirty data on startup.

#### Scenario: Startup cleanup
GIVEN the backend starts
WHEN test dirty data exists
THEN the dirty data is automatically cleaned

### Requirement: Account Info Entry
The system SHALL include account info in personal top 10 display.

#### Scenario: Display account info
GIVEN a user views personal top 10
WHEN the display is rendered
THEN account info entries are included
