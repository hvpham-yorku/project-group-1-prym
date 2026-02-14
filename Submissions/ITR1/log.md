# PRYM Project Log

## Wiki
For detailed documentation, visit the [Project Wiki](../../wiki)

---

## Meeting Minutes

> **Note:** Minutes were not taken during daily "stand up meetings"

---

### January 15th, 2026

**Agenda:** Decide on project idea (URGENT) and create contract

- Discussed potential ideas: freezer organization, education
- Shayan suggested that we work with Cow Sharing, something he has personal experience with - all agreed on this idea
- Drafted and sent email to professor about the project idea for approval
- Discussed and Katelyn created group contract, will be emailed around for everyone to sign

---

### February 3rd, 2026

**Agenda:** Discuss project and setting up

- Jacob: setting up React front end
- Front end setup achieved – back end still needed

---

### February 5th, 2026

**Agenda:** Discuss project progress

- Harleen could do seller account attribute changing – task reassigned to her
- We need to focus on front-end deliverables to have something the customer can better interact with
- Sufyan will be giving Katelyn reference pages for the libraries used in SecurityConfig file so she can look through some concerns
- Add to profile: username, first + last name, phone #
- Discussing git commits: everyone will make sure to do a lot of small commits instead of one big one (commit every time you get something small working)
- Make sure to be descriptive in pull requests!!

---

### February 9th, 2026

**Agenda:** Discuss project architecture & check in

- Went over architecture basics
- Discussed ITR1 Deliverables
- Went over some security discussions
- User experience diagram created
- Browser page visuals discussion initiated

---

### February 12th, 2026

**Agenda:** Discuss project progress, discuss architecture diagram

- Jacob showing rudimentary design of architecture diagram
- Discussing potential changes to implement in later iterations
- Utilized a whiteboard to illustrate thoughts 
---

### February 13th, 2026

**Agenda:** Discuss project wiki and testing structure

- Went over architecture basics
- Reviewed ITR1 Deliverables
- Discussed user experience
- Added link to wiki to log file and modified log file
- Examined and commented on Jira progress

---

## Rationale behind big decisions

| Decision | Rationale |
|------|-------------|
| Authentication using cookie based session management | we chose to use cookie based management instead of JWT tokens or Spring Security's default session management is that they were easier to implement and we can have better control over them.  |
| Single User table with Role enum (BUYER/SELLER) | we chose this instead of seperate BuyerUser and SellerUser tables because it would reduce the duplicates by a lot, which would allow us to follow DRY coding principles. and the use of Role enum instead of strings prevents typos |
| H2 File-Based database with JPA/Hibernate | since we are still in development, our team chose a file-based storage that requires no PostgreSQL or MySQL. This would make development easier and make sure everyone has an identical database setup  |
| Use of Lombok annotations instead of manual getters/setters/constructors | This is a design choice we made to make the code readable and have less repetitions. | 
| Shared User entity with separate Buyer/Seller profiles | Common user attributes (email, password, first name, last name, phone) are stored in a single User table, while role specific data (shopName/shopAddress for sellers, preferredCuts/quantity for buyers) are stored in separate Buyer and Seller tables linked by userId. This follows normalization principles and avoids duplicating common fields across roles. |

---

## Concerns

- Our main concern was understanding the stack we are using as fast as possible, and we are well on track to do so. 

---

## Changes On Plan

- Instead of two-page setup where second page is name, phone # AND preferences, in later iterations create a separate questionnaire for users on a different page to make it less confusing

---

## Task Assignments

| Task | Assigned To |
|------|-------------|
| Team Jira Setup + Maintenance | Jacob |
| PRD Update | Sufyan |
| README File | Sufyan |
| Project Map Design | Katelyn |
| Implement Basic UI Vue Server | Jacob |
| Implement Basic Database Backend | Sufyan |
| Buyer Account Creation | Sufyan |
| Buyer/Seller Login | Sufyan |
| Basic UI Decorations | Jacob |
| Buyer Profile Modification | Shayan |
| Seller Account Creation | Sufyan |
| Farm Listings | Katelyn |
| Basic Tests | Jacob |
| Mock Farm Listing Data | Katelyn |
| Mock Seller Account Data | Harleen |
| Mock Buyer Account Data | Shayan |
| Seller Profile Modifications | Harleen |
| Authentication and Security Files | Sufyan |
| Architecture Diagram | Jacob |

---

## Development Tasks Per User Story
| User Story | Details |
|------|-------------|
| Buyer/Seller login |  POST /api/auth/login - email and password validation, create SESSION_ID cookie (7 day expiry), return user object with role |
| Buyer account creation|  POST /api/auth/register/buyer - Creates User with BUYER role, validates unique email/username, hashes password, auto-creates session, returns user data |
| Seller account creation |  POST /api/auth/register/seller - Creates User with SELLER role, validates unique email/username, hashes password, auto-creates session, returns user data  |
| Buyer account modification | temp |
| Seller account modification | temp |
| Farm listings | create basic page to hold listings, link them to another temporary farm listing page, create basic data |



---

## Time Allocated vs Time Spent

| Task | Assigned To | Time Allocated | Time Spent |
|------|-------------|----------------|------------|
| Team Jira Setup + Maintenance | Jacob | | |
| PRD Update | Sufyan | 1 hour | 1 hour |
| README File | Sufyan | 30 minutes | 20 minutes |
| Project Map Design | Katelyn | 1 Day | 1 Day |
| Implement Basic UI Vue Server | Jacob | | |
| Implement Basic Database Backend | Sufyan | 1 day | 1 day |
| Seller Account Creation | Sufyan | 1 day | 0.5 day |
| Buyer Account Creation | Sufyan | 1 day | 0.5 day |
| Buyer/Seller Login | Sufyan | 1.5 days | 1 day |
| Authentication and Security Files | Sufyan | 0.5 day | 0.5 day |
| Basic UI Decorations | Jacob | | |
| Buyer Profile Modification | Shayan | 3 hours | 1 day |
| Farm Listings | Katelyn | 0.5 Days | 3 hours |
| Basic Tests | Jacob | | |
| Mock Farm Listing Data | Katelyn | 30 min | 20 min |
| Mock Seller Account Data | Harleen | 30 min |1.5 hours|
| Mock Buyer Account Data | Shayan | 30 mins | 30 mins|
| Seller Profile Modifications | Harleen | 1 Day |1.5 days |
| Buyer Profile Modification | Shayan |3 hours | 3 hours 30 mins|
| Farm Listings | Katelyn | 1 Day | 3 hours |
| Basic Tests | Jacob | | |
| Mock Farm Listing Data | Katelyn | 30 min | 20 min |
| Mock Seller Account Data | Harleen | | |
| Mock Buyer Account Data | Shayan | 30 mins | 30 mins|
| Seller Profile Modifications | Harleen | 1 day | 1 day |
| Architecture Diagram | Jacob | | |

---
**some of the tasks here were done in parallel, so there are a lot of cases where times overlap.**
**some tasks were similar with minor changes like Buyer login and Seller login, you implement it for one and just copy it for the other while doing minor changes. So the allocated time for the tasks was split up evenly.**
