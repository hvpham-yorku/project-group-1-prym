# PRYM — Revised Architecture Diagram with Integration Test Seams

This is a revised version of the ITR2 architecture diagram. It preserves the same layered structure and adds two things:
1. New components introduced since ITR2 (RatingController, AssociationController, RatingService, AssociationService, new repositories and tables)
2. **Integration test seams** — dashed arrows showing which test class exercises each Service ↔ Database boundary

Integration tests `@Autowired` the Spring Service directly, bypassing the HTTP, Security, and Controller layers entirely, then run against the real PostgreSQL database (`prymdb`). Each dashed arrow is a seam.

---

```mermaid
graph TD

    subgraph FE["Browser  (localhost:5173)"]
        subgraph React["React Frontend  (Vite)"]
            Routes["Routes (React Router)\n/login · /register/buyer · /register/seller\n/buyer/dashboard · /profile · /farmlistingspage\n/farmlisting · /groups · /groups/create · /groups/:id\n/seller/dashboard · /seller/profile-setup"]
            Auth["AuthContext\nuser state · saveUser() · clearUser() · ProtectedRoute"]
            API["API layer  (src/api/)\nauth.js · buyer.js · seller.js · farm.js · groups.js · rating.js\nAll use fetch(..., credentials:'include')"]
        end
    end

    subgraph BE["Spring Boot Backend  (localhost:8080)"]

        subgraph SEC["Security Layer"]
            SF["SessionFilter\nvalidates SESSION_ID cookie → SecurityContext"]
            SC["SecurityConfig\nCORS · CSRF disabled · route auth rules"]
        end

        subgraph CL["Controller Layer"]
            CTRL["AuthController · BuyerController · SellerController\nGroupController · CowController · CowTypeController\nCertificationController · RatingController · AssociationController"]
        end

        subgraph SL["Service Layer"]
            AS["AuthService"]
            BS["BuyerService"]
            SS["SellerService"]
            SES["SessionService"]
            GS["GroupService"]
            CS["CowService / CowTypeService"]
            CERTS["CertificationService"]
            RS["RatingService"]
            ASCS["AssociationService"]
        end

        subgraph RL["Repository Layer  (Spring Data JPA / Hibernate)"]
            UR["UserRepository · SessionRepository"]
            BR["BuyerRepository"]
            SR["SellerRepository"]
            GR["BuyerGroupRepository\nBuyerGroupMemberRepository\nGroupSellerAssociationRepository\nGroupMessageRepository"]
            CR["CowRepository · CowCutRepository\nCowTypeRepository · BuyerMatchRepository"]
            CERTR["CertificationRepository"]
            RR["RatingRepository · RatingCodeRepository"]
        end

    end

    subgraph DB["PostgreSQL Database  (localhost:5432/prymdb)"]
        PG[("prymdb\nusers · buyers · sellers · sessions\ncertifications · cow_types · cows · cow_cuts\nbuyer_groups · buyer_group_members · buyer_matches\ngroup_seller_associations · group_messages\nratings · rating_codes\n— 15 tables, auto-created by Hibernate —")]
    end

    %% ── Normal request flow ──────────────────────────────────────────
    FE -->|"HTTP/JSON + SESSION_ID cookie  (Vite proxy)"| SEC
    SEC --> CL
    CL --> AS & BS & SS & SES & GS & CS & CERTS & RS & ASCS
    AS   --> UR
    BS   --> BR
    SS   --> SR
    SES  --> UR
    GS   --> GR
    CS   --> CR
    CERTS --> CERTR
    RS   --> RR
    ASCS --> GR
    UR & BR & SR & GR & CR & CERTR & RR -->|"SQL / Hibernate"| PG

    %% ── Integration test seams ──────────────────────────────────────
    %% Tests @Autowire the Service directly — no HTTP, no Security,
    %% no Controller. Each seam is the Service ↔ Repository ↔ DB boundary.

    T1(["AuthServiceIntegrationTest"])   -. "Seam 1" .-> AS
    T2(["BuyerServiceIntegrationTest"])  -. "Seam 2" .-> BS
    T3(["SellerServiceIntegrationTest"]) -. "Seam 3" .-> SS
    T4(["GroupServiceIntegrationTest"])  -. "Seam 4" .-> GS
    T5(["RatingServiceIntegrationTest"]) -. "Seam 5" .-> RS
    T6(["CowServiceIntegrationTest"])    -. "Seam 6" .-> CS
```

---

## Integration Test Seam Summary

| Seam | Integration Test Class | Service Under Test | Repositories Exercised |
|------|------------------------|--------------------|------------------------|
| 1 | `AuthServiceIntegrationTest` | `AuthService` | `UserRepository` |
| 2 | `BuyerServiceIntegrationTest` | `BuyerService` | `BuyerRepository` |
| 3 | `SellerServiceIntegrationTest` | `SellerService` | `SellerRepository` |
| 4 | `GroupServiceIntegrationTest` | `GroupService` | `BuyerGroupRepository`, `BuyerGroupMemberRepository` |
| 5 | `RatingServiceIntegrationTest` | `RatingService` | `RatingRepository`, `RatingCodeRepository` |
| 6 | `CowServiceIntegrationTest` | `CowService` / `CowTypeService` | `CowRepository`, `CowCutRepository`, `CowTypeRepository` |

### What each seam tests

**Seam 1 — AuthService ↔ DB**
Verifies that user registration persists to the database, that unique constraints on email and username are enforced by the real DB, that passwords are hashed (not stored in plaintext), and that the full register → login flow works end-to-end.

**Seam 2 — BuyerService ↔ DB**
Verifies that buyer profiles are created and linked to the correct `User` row, that duplicate profiles are rejected, and that profile updates (preferred cuts, phone number) are persisted correctly.

**Seam 3 — SellerService ↔ DB**
Verifies that seller profiles are created, retrieved, and updated correctly in the database, and that `getAllFarms()` reflects newly inserted rows.

**Seam 4 — GroupService ↔ DB**
Verifies that groups are created with the correct name/certifications, that membership is tracked across multiple buyers, that cut-claiming respects per-cut slot limits, and that leaving/deleting groups cascades correctly.

**Seam 5 — RatingService ↔ DB**
Verifies the full generate-code → submit-rating flow against the real DB, that the running average is computed correctly across multiple ratings, that used codes are atomically marked and rejected on reuse, and that duplicate ratings per seller are blocked.

**Seam 6 — CowService ↔ DB**
Verifies that creating a cow auto-generates exactly 22 `CowCut` records (11 cut names × LEFT + RIGHT sides), all starting as `AVAILABLE`, and that `getCowsBySeller` correctly scopes results to a single seller.

---

## Changes from ITR2 Architecture

| Component | Change |
|-----------|--------|
| `RatingController` | Added — handles rating code generation and rating submission |
| `AssociationController` | Added — handles group ↔ seller association lifecycle |
| `RatingService` | Added |
| `AssociationService` | Added |
| `RatingRepository`, `RatingCodeRepository` | Added |
| `GroupSellerAssociationRepository`, `GroupMessageRepository` | Added |
| `ratings`, `rating_codes` tables | Added |
| `group_seller_associations`, `group_messages` tables | Added |
| Table count | 12 → 15 |
