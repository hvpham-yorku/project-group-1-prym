# ITR2 Release Notes

## New Features

### Buyer Groups
- Buyers can create a group with a name and dietary certifications (Halal, Organic, Kosher, etc.)
- Browse and search available groups
- Join or leave groups
- View group details including member count and claimed cuts
- Interactive cow diagram for selecting preferred meat cuts within a group
- Groups are automatically deleted when the last member leaves

### Farm Listings
- Sellers can list farms with full details (breed, pricing, certifications, description)
- Farm listing pages have unique URLs
- Farm listings are integrated into the seller dashboard
- Buyers can browse and view individual farm pages

### Account Editing
- Buyers and sellers can edit their profile info (name, phone number, category)
- Profile picture display added
- Edit account modal added to both dashboards

---

## Infrastructure

### Database
- Migrated from H2 in-memory database to PostgreSQL
- Schema is auto-managed by Hibernate — no manual SQL required
- `database/README.md` updated with full setup instructions

### Testing
- Unit tests reorganized into `unit/service/` and `unit/controller/` packages
- Integration tests added against real PostgreSQL (`integration/` package)
- Tests use `@Transactional` for automatic rollback — database stays clean
- Coverage: Auth, Buyer, Seller, Group, Cow, CowType, Certification

---

## Bug Fixes
- Fixed CORS issue preventing frontend from communicating with backend
- Fixed `profilePicture` typo in API responses
- Fixed seller category not saving on update
- Fixed farm listings page not displaying data correctly
- Fixed phone number validation issues

---

## Deferred to ITR3
- Group chat
- Farm communication / messaging
- Cow cut selection diagram (interactive UI)
- Farm ratings display
