PRYM Project Log

## Wiki
For detailed documentation, visit the [Project Wiki](../../wiki)

---

## Meeting Minutes

> **Note:** Minutes were not taken during daily "stand up" meetings
### March 16 2026
Agenda: go over plan for final week and make clarifications

- Change the location of logout button in buyer dashboard
- Phone number should be editable via pencil icon option
- Add filters and search option on farm listings page
- Filters for farm listings page: geographic location, rating, category and sort (ascending/descending) option
- Address -> currently as a String, need to be actual geographic address
- Need to add view reviews on farm listing
- Need to have separate unique id for farms to submit ratings
- Connect sellers to the group chats
- Submit a rating, in the group page, like a pop up display to submit ratings
- Show ratings, farm details on the farm listings page under the farm name

___

### March 23 2026
Agenda: check in and make clarifications

- Attribute for ratings to show ratings on farm listings
- Profile pictures (seller profile photo) and cow lists to farm listings page
- Seller Dashboard -> add cows feature
- Sellers access to group chat
- Fix saved farms
- Fix failing unit tests
- Geographical location for address
___

### March 26 2026
Agenda: final days planning

To do:
- Document API endpoints
- Seller association
- Wiki
- Refactor buyer profile main page
- Lab 5
- Testing (integration, system and unit testing)

___

### March 28 2026
Agenda: lab 5 discussion and more finalization

- Jacob needs to wait for sufyan to commit his extra commentation
- figuring out lab 5 documents 

___

## Rationale behind big decisions

| Decision | Rationale |
|------|-------------|
| Not implementing contract | After discussion with the customer, we found it to be a feature that would not add anything of significance to the app, and therefore deemed in unecessary |
| No group chat settings | This feature seemed unecessary and were not a significant addition to the platform at this stage |


---

## Concerns


---

## Changes On Plan

- Decided not to implement the contract feature
- (and subsequently, users do not need to view the agreement details)
- Decided not to implement group chat settings

---

## Task Assignments

| Task | Assigned To|
|------|------------|
| Fix seller "complete signup" page | Sufyan |
| Group-seller association system | Sufyan |
| Group farm matching | Sufyan |
| Group chat |Sufyan|
| Seller dashboard modification | Sufyan |
| Move logout button to top of page | Jacob |
| Geographic location matching | Jacob |
| Save/bookmark farms | Katelyn |
| View farm details and ratings on listing | Katelyn |
| View farm details and ratings on farm page | Katelyn |
| Submit ratings | Harleen |
| Search bar for farm listings | Harleen |
| Filter farms | Harleen |

---

## Development Tasks Per User Story
| User Story | Details |
|------|-------------|
| Farm listings | save/bookmark farms, see the details in the listing icon (photograph, description, rating, certifications, location), submit ratings for farms |
| Group chat | Set up WebSocket config (STOMP broker, SockJS fallback), created `ChatController` to handle incoming messages, validate sender is a group member, persist messages, and broadcast to group topic; added `GroupMessage` entity and repository; integrated live chat UI into `GroupDetailPage` |
| Group–seller association system | Created full association feature end-to-end: `GroupSellerAssociation` entity, `AssociationStatus` enum, `GroupSellerAssociationRepository`, `AssociationService`, `AssociationController`; created `association.js` frontend API helper; integrated association request panel into `GroupDetailPage` and seller association inbox into `SellerDashboard` |
| Group farm matching | Added `getMatchingFarms` to `GroupService` — finds farms with perfect/partial certification match and sorts results by distance using `DistanceUtil`; added matching farms panel in `GroupDetailPage` with perfect match / partial match tabs |

---

## Time Allocated vs Time Spent

| Task | Assigned To | Time Allocated | Time Spent |
|------|-------------|----------------|------------|
| Group-seller association system | Sufyan | 1 day | 1 day |
| Group farm matching | Sufyan | 1 day | 1 day |
| Seller dashboard modification | Sufyan | 0.5 days | 0.5 days |
| Group chat | Sufyan | 1.5 days | 1 day |
| Move logout button to top of page | Jacob | | |
| Geographic location matching | Jacob | | |
| Save/bookmark farms | Katelyn | 1 day | 0.5 days |
| View farm details and ratings on listing | Katelyn | 1 day | 1.5 days |
| View farm details and ratings on farm page | Katelyn | 1 day | 1.5 days |
| Submit ratings | Harleen | 1 day | 1.5 days |
| Search bar for farm listings | Harleen | 0.5 day | 0.5 day |
| Filter farms | Harleen | 0.5 day | 0.5 day |

---
**some of the tasks here were done in parallel, so there are a lot of cases where times overlap.**
**some tasks were similar with minor changes like Buyer login and Seller login, you implement it for one and just copy it for the other while doing minor changes. So the allocated time for the tasks was split up evenly.**

