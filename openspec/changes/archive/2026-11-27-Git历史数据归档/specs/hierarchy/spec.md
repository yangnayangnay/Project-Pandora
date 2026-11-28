# Hierarchy Spec

## ADDED Requirements

### Requirement: Personnel Hierarchy Tree
The system SHALL support tree-shaped personnel hierarchy management.

#### Scenario: View hierarchy tree
GIVEN a user views the personnel hierarchy
WHEN they open the hierarchy view
THEN the tree structure is displayed with expandable nodes

### Requirement: Tree Sorting
The system SHALL support sorting in tree hierarchy view.

#### Scenario: Sort tree nodes
GIVEN a user views the hierarchy tree
WHEN they apply sorting
THEN the tree nodes are sorted and returned in order

### Requirement: Username Field in Child Nodes
The system SHALL include username field in tree child nodes.

#### Scenario: Display username in child nodes
GIVEN a user views the hierarchy tree
WHEN they expand a node with child elements
THEN each child node displays the username field