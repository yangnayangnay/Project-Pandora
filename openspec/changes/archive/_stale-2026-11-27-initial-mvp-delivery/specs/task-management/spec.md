# Task Management Spec

## ADDED Requirements

### Requirement: Task Creation
The system SHALL allow users to create tasks with name, priority, assignee, importance and urgency.

#### Scenario: Create personal task
GIVEN the user is logged in
WHEN they fill in task name and priority and submit
THEN the task is created and appears in their personal task list

#### Scenario: Dispatch task to assignee
GIVEN a superior is logged in
WHEN they fill in all 6 core indicators and specify an assignee
THEN the task is dispatched and the assignee receives it

### Requirement: Task Status Transition
The system SHALL support task status flow with 5 states and role-based transitions:
- PENDING → {IN_PROGRESS, REJECTED}
- IN_PROGRESS → {COMPLETED, REJECTED}
- COMPLETED → {CONFIRMED, REJECTED, IN_PROGRESS}
- CONFIRMED → {} (terminal)
- REJECTED → {PENDING, IN_PROGRESS}

#### Scenario: Start task
GIVEN the assignee has a PENDING task
WHEN they start the task
THEN the status becomes IN_PROGRESS

#### Scenario: Complete task
GIVEN the assignee has an IN_PROGRESS task
WHEN they mark it as completed with a progress note
THEN the status becomes COMPLETED and the dispatcher gets notified

#### Scenario: Confirm completed task
GIVEN the dispatcher has a COMPLETED task
WHEN they confirm it
THEN the status becomes CONFIRMED and the flow ends

#### Scenario: Reject task
GIVEN the dispatcher has a COMPLETED or PENDING or IN_PROGRESS task
WHEN they reject (return) it
THEN the status becomes REJECTED and the assignee gets notified

#### Scenario: Resubmit rejected task
GIVEN the assignee has a REJECTED task
WHEN they resubmit it
THEN the status becomes PENDING or IN_PROGRESS

#### Scenario: REJECTED status display and filter
GIVEN the task list is displayed
WHEN a task has status REJECTED
THEN it is displayed as "已退回" (TaskListAdapter.getStatusDisplayName) and can be filtered via the "已退回" tab (TaskListFragment.setupTabs)

### Requirement: Quadrant View
The system SHALL display tasks in a four-quadrant view based on importance and urgency.

#### Scenario: View quadrant
GIVEN the user has tasks with different importance/urgency
WHEN they switch to quadrant view
THEN tasks are displayed in four quadrants