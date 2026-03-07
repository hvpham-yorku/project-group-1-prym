# PRYM Project Log

## Wiki
For detailed documentation, visit the [Project Wiki](../../wiki)

---

## Meeting Minutes

> **Note:** Minutes were not taken during daily "stand up" meetings
### February 23rd, 2026
**Agenda:** Discuss farm listings, preferences, groups, chat, and ratings features.
- Confirmed that each seller has one farm with multiple farm listings, and listings link to the farm page with descriptions.
- Discussed preferences page design, including a cow diagram for meat selection (planned for ITR3) and buttons: Done, Close, and Find Me a Group.
- Decided that buyers can find matched groups based on preferences, search groups by name, or create a group if no matches are found.
- Group chat will be available for members with options to leave the chat, and invite links were discussed.
- Planned rating functionality, allowing buyers to submit ratings and sellers to view ratings for their farm.
  
### March 2nd, 2026

**Agenda** Discuss more formatting
- changed dashboard: seller dashboard now just their farm listing
- confirmed how group chat will be made
- create a cow image with cuts for users to select preferred cuts?
___

## Rationale behind big decisions

| Decision | Rationale |
|------|-------------|
| Remove seller dashboard | We realised that the seller would have no need for a dashboard past looking at their own farm listing, so we just made that their dashboard and will make it editable |
| Migrate from H2 to PostgreSQL | H2 is convenient for development but not suitable for production use. PostgreSQL gives us a proper relational database with real constraints, better data integrity, and a persistent data store that reflects how the app will behave in production |
| Move group chat and farm communication to ITR3 | We lost a team member during reading week, which reduced our capacity. Group chat and farm communication were deprioritized in favour of completing core features (groups, farm listings, profile editing) that are more critical to the app's core functionality |
| Use cookie-based session authentication | Session cookies are simpler to implement and manage compared to JWT tokens for our use case. The server controls session validity, making it easier to invalidate sessions on logout without needing token blacklisting |
| Split tests into unit and integration suites | Unit tests using H2 and Mockito run fast and require no setup, making them useful for development feedback. Integration tests against real PostgreSQL catch issues that only surface with a real database (e.g. constraint violations, JPA mapping errors). Keeping them separate lets developers run unit tests quickly without needing a local database |
| Use dependency injection for database switching | By configuring the datasource through `application.properties`, we can switch between H2 (unit tests), PostgreSQL (integration tests and production) without changing any application code — just one property change |

---

## Concerns

- ITR2: we lost a member during the reading week, so we now need to redistribute user stories and move some to ITR3
- Group chat and farm communication features were planned for ITR2 but had to be moved to ITR3 due to reduced team capacity — this may put pressure on the ITR3 workload
---

## Changes On Plan

- Moved some user stories to ITR3 due to losing a team member during reading week.
- Group chat, farm communication, and related tests were originally planned for ITR2 and have been deferred to ITR3.
- Seller dashboard was removed and replaced with the seller's farm listing page, which now serves as their main view.
- Redistributed remaining ITR2 tasks among the four remaining team members to maintain progress.
---

## Task Assignments

| Task | Assigned To|
|------|------------|
| Show Farms | Katelyn |
| View Farm Listings | Katelyn |
| Create a group | Jacob |
| Show groups with matching preferences | Jacob |
| Search for a group | Jacob |
| View group details | Jacob |
| Join/leave a group | Jacob |
| Jira Setup | Jacob |
| Basic UI decorations | Harleen |
| Farm Listings Integration into dashboard | Katelyn |
| Group creation tests | Jacob |
| General user profile modification | Harleen |
| View Farm details and ratings | Katelyn |
| Farm listing tests | Katelyn |
| Group chat with group members | Sufyan |
| Manage group chat settings | Sufyan |
| Communication with farms | Sufyan |
| Group chat tests | Sufyan |
| Integration tests | Sufyan |
| Migrate to PostgreSQL | Sufyan |

---

## Development Tasks Per User Story
| User Story | Details |
|------|-------------|
| Farm Listings | Integrate with database, UI changes, have listings link to unique pages, create actual farm listing page with all info |
| Groups | Backend: Group model, repository, service, controller; Frontend: create group page, group search, group details page, join/leave functionality, matched groups view |
| User Profile Modification | Allow buyers and sellers to edit profile info (name, phone, category); profile picture display; fix CORS and session handling |
| Farm Details & Ratings | View individual farm page with full details; next iteration we will display ratings submitted by buyers |
| Database Migration | Migrate from H2 to PostgreSQL for production; configure datasource via dependency injection for easy switching |
| Testing | Unit tests (Mockito + MockMvc) for all services and controllers; integration tests against real PostgreSQL with transactional rollback |

---

## Time Allocated vs Time Spent

| Task | Assigned To | Time Allocated | Time Spent |
|------|-------------|----------------|------------|
| Show Farms | Katelyn | 1 Day | 1 Day |
| View Farm Listings | Katelyn | 0.5 Day | 0.5 Day |
| Create a group | Jacob | 1.5 Days | 2 Days |
| Show groups with matching preferences | Jacob | 2 Days | 2 Days |
| Search for a group | Jacob | 1.5 Days | 1.5 Days |
| View group details | Jacob | 1 Days | 1 Days |
| Join/leave a group | Jacob | 0.5 Days | 0.5 Days |
| Jira Setup | Jacob | 0.5 days | 0.5 days|
| Basic UI decorations | Harleen |0.5 Day|0.5 Day|
| Farm Listings Integration into dashboard | Katelyn | 30min | 20min |
| Group creation tests | Jacob | 0.5 Days | 0.5 Days |
| General user profile modification | Harleen |0.5 Day|0.5 Day|
| View Farm details and ratings | Katelyn | 1 Day | 1 Day | 
| Farm listing tests | Katelyn | 1 Day | 0.5 Day |
| Group chat with group members | Sufyan | moved to ITR3 due to plan changes | |
| Manage group chat settings | Sufyan | moved to ITR3 due to plan changes | |
| Communication with farms | Sufyan | moved to ITR3 due to plan changes | |
| Group chat tests | Sufyan | moved to ITR3 due to plan changes | |
| Integration tests | Sufyan | 0.5 days | 0.5 days |
| Migrate to PostgreSQL | Sufyan | 1 day | 1 day |


---
**some of the tasks here were done in parallel, so there are a lot of cases where times overlap.**
**some tasks were similar with minor changes like Buyer login and Seller login, you implement it for one and just copy it for the other while doing minor changes. So the allocated time for the tasks was split up evenly.**
