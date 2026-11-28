# draft Specification

## Purpose
草稿能力支持任务和日志创建过程中的自动保存与恢复。主要功能包括草稿自动保存、草稿恢复对话框、草稿清空、竞态条件处理（创建成功后不错误恢复草稿）。防止用户因意外中断导致数据丢失。

## Requirements

### Requirement: Auto-Save Draft
The system SHALL automatically save drafts during task/log creation.

#### Scenario: Auto-save draft
GIVEN a user is creating a task or log
WHEN they pause during creation
THEN the draft is automatically saved

### Requirement: Draft Restore
The system SHALL support restoring saved drafts.

#### Scenario: Restore draft
GIVEN a user has a saved draft
WHEN they reopen the creation form
THEN the draft restore dialog appears

### Requirement: Draft Clear
The system SHALL support clearing saved drafts.

#### Scenario: Clear draft
GIVEN a user has a saved draft
WHEN they choose to clear the draft
THEN the draft is removed

### Requirement: Race Condition Fix
The system SHALL handle race conditions in draft auto-save.

#### Scenario: Race condition handling
GIVEN a user completes task/log creation
WHEN the creation succeeds
THEN the draft is not incorrectly restored
