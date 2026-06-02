---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Automated tests are REQUIRED for constitution-governed business rules,
including pricing, confidence, expected price, market signal classification,
supply and demand signals, alerts, grading scenarios, trade calculations,
portfolio valuation, and currency conversion. Tests MUST use mocks, fakes,
fixtures, or test doubles for external providers rather than live APIs.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Spring Boot source**: `src/main/java/[base_package]/`
- **Spring Boot tests**: `src/test/java/[base_package]/`
- **Database migrations**: `src/main/resources/db/migration/`
- **Configuration**: `src/main/resources/application.yml` and secure environment
  configuration
- **UI assets/templates**: [document concrete paths chosen in plan.md, such as
  server-rendered templates or frontend assets]
- Use constitution package boundaries: `catalog`, `portfolio`, `pricing`,
  `market_signal`, `alerts`, `grading`, `trade`, `forecasting`, `auth`, and
  `config`

<!--
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.

  The /speckit-tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/

  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create Spring Boot modular monolith structure per implementation plan
- [ ] T002 Initialize Java 21 Spring Boot 3 project with Spring Web, Spring Data
  JPA, Spring Security, PostgreSQL, Flyway, Spring Scheduler, and WebClient
- [ ] T003 [P] Configure formatting, test, and build tooling
- [ ] T004 [P] Establish dark-mode-first financial dashboard UI foundation,
  including typography, compact tables, gain/loss color semantics, panels, and
  analytics-first layout patterns

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust based on your project):

- [ ] T005 Setup PostgreSQL schema and Flyway migrations
- [ ] T006 [P] Implement Spring Security authentication, password hashing, and
  admin authorization
- [ ] T007 [P] Create package boundaries for catalog, portfolio, pricing,
  market_signal, alerts, grading, trade, forecasting, auth, and config
- [ ] T008 Create base entities for catalog items, owned portfolio records,
  source prices, price snapshots, currency conversion, and audit metadata
- [ ] T009 Define provider interfaces/adapters and provider test doubles
- [ ] T010 Configure error handling, scheduling, WebClient timeouts, and secure
  environment-based configuration

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) [MVP]

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US1] Service/business-rule test for [rule] in
  src/test/java/[base_package]/[package]/[Name]ServiceTest.java
- [ ] T012 [P] [US1] Web/API integration test for [user journey] in
  src/test/java/[base_package]/[package]/[Name]ControllerTest.java

### Implementation for User Story 1

- [ ] T013 [P] [US1] Create [Entity1] entity in
  src/main/java/[base_package]/[package]/[Entity1].java
- [ ] T014 [P] [US1] Create [Entity2] entity in
  src/main/java/[base_package]/[package]/[Entity2].java
- [ ] T015 [US1] Implement [Service] in
  src/main/java/[base_package]/[package]/[Name]Service.java (depends on T013,
  T014)
- [ ] T016 [US1] Implement thin controller or scheduled entry point in
  src/main/java/[base_package]/[package]/[Name]Controller.java
- [ ] T017 [US1] Add validation and error handling
- [ ] T018 [US1] Add audit/explanation fields or snapshot persistence where
  constitution rules require it
- [ ] T019 [US1] Apply Modern Financial Dashboard UI direction to any user
  interface for this story, avoiding childish, toy-like, cartoon-heavy, or
  analytics-free presentation

**Checkpoint**: At this point, User Story 1 is expected to be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2

- [ ] T020 [P] [US2] Service/business-rule test for [rule] in
  src/test/java/[base_package]/[package]/[Name]ServiceTest.java
- [ ] T021 [P] [US2] Web/API integration test for [user journey] in
  src/test/java/[base_package]/[package]/[Name]ControllerTest.java

### Implementation for User Story 2

- [ ] T022 [P] [US2] Create [Entity] entity in
  src/main/java/[base_package]/[package]/[Entity].java
- [ ] T023 [US2] Implement [Service] in
  src/main/java/[base_package]/[package]/[Name]Service.java
- [ ] T024 [US2] Implement thin controller or scheduled entry point in
  src/main/java/[base_package]/[package]/[Name]Controller.java
- [ ] T025 [US2] Integrate with User Story 1 components (if needed)
- [ ] T026 [US2] Apply Modern Financial Dashboard UI direction to any user
  interface for this story, avoiding childish, toy-like, cartoon-heavy, or
  analytics-free presentation

**Checkpoint**: At this point, User Stories 1 AND 2 are expected to work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3

- [ ] T027 [P] [US3] Service/business-rule test for [rule] in
  src/test/java/[base_package]/[package]/[Name]ServiceTest.java
- [ ] T028 [P] [US3] Web/API integration test for [user journey] in
  src/test/java/[base_package]/[package]/[Name]ControllerTest.java

### Implementation for User Story 3

- [ ] T029 [P] [US3] Create [Entity] entity in
  src/main/java/[base_package]/[package]/[Entity].java
- [ ] T030 [US3] Implement [Service] in
  src/main/java/[base_package]/[package]/[Name]Service.java
- [ ] T031 [US3] Implement thin controller or scheduled entry point in
  src/main/java/[base_package]/[package]/[Name]Controller.java
- [ ] T032 [US3] Apply Modern Financial Dashboard UI direction to any user
  interface for this story, avoiding childish, toy-like, cartoon-heavy, or
  analytics-free presentation

**Checkpoint**: All user stories are expected to be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX [P] Additional unit and integration tests for uncovered constitution
  rules in src/test/java/[base_package]/
- [ ] TXXX Security hardening
- [ ] TXXX Verify UI preserves dark-mode-first modern financial dashboard style,
  compact financial tables, green/red gain-loss indicators, high information
  density without clutter, and analytics context for card imagery
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but MUST remain independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but MUST remain independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation when they cover
  constitution-governed business rules
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Service/business-rule test for [rule] in src/test/java/[base_package]/[package]/[Name]ServiceTest.java"
Task: "Web/API integration test for [user journey] in src/test/java/[base_package]/[package]/[Name]ControllerTest.java"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] entity in src/main/java/[base_package]/[package]/[Entity1].java"
Task: "Create [Entity2] entity in src/main/java/[base_package]/[package]/[Entity2].java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story MUST be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
