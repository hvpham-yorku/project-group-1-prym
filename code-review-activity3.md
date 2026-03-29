# Activity 3 – Individual Code Review
## Assigned User Stories: View Farm Listings · View Farm Details & Ratings · Compare Farms · Save/Bookmark Farms · Geographic Location & Filter by Distance · Search Bar & Filters

---

## Files Reviewed

| File | Role |
|------|------|
| `uiserver/src/pages/FarmListingsPage.jsx` | Browse all farms, search, filter, paginate |
| `uiserver/src/pages/FarmListing.jsx` | Individual farm detail + rating modal |
| `uiserver/src/pages/SavedFarms.jsx` | Buyer's bookmarked farms |
| `uiserver/src/api/farm.js` | Frontend API helpers for farm data |
| `uiserver/src/api/ratings.js` | Frontend API helpers for rating system |
| `backend/.../controller/SellerController.java` | REST endpoints for seller/farm data |
| `backend/.../service/SellerService.java` | Business logic for seller profiles |
| `backend/.../service/BuyerService.java` | Business logic for buyer profiles & saved farms |
| `backend/.../service/RatingService.java` | Business logic for two-step rating system |
| `backend/.../util/DistanceUtil.java` | Haversine distance calculation utility |
| `backend/.../util/ZipCodeUtil.java` | ZIP code → coordinates lookup utility |

---

## Code Smell Checklist

The following categories from the refactoring literature were checked:

- [x] **Duplicated Code** — same logic copy-pasted across files
- [x] **Long Method / Large Class** — methods doing too many things
- [x] **Magic Numbers / Magic Strings** — unexplained literal values
- [x] **Inappropriate Intimacy** — code making incorrect assumptions about another object's internals
- [x] **Switch Statements** — long if-else chains that should be data-driven
- [x] **Dead Code / Speculative Generality** — code that exists but is never executed
- [x] **Improper Error Handling** — errors silently swallowed or using wrong exception types
- [x] **Naming Convention Violations** — names that break language/framework conventions
- [x] **Lazy Class / Missing Abstraction** — helper functions that don't reuse each other
- [x] **Inappropriate Data Layer Responsibility** — business calculations that belong in the database
- [x] **Missing Implementation** — user story with no code at all

---

## Detected Code Smells

---

### Smell 1 — Duplicated Badge Rendering Logic
**Category:** Duplicated Code (DRY Violation)
**Severity:** Medium
**Files:**
- `uiserver/src/pages/FarmListingsPage.jsx` lines 68–86
- `uiserver/src/pages/SavedFarms.jsx` lines 31–49

**Description:**

Both `FarmListingsPage.jsx` and `SavedFarms.jsx` contain nearly identical certification badge rendering blocks. Each file independently checks `c.name` against five string literals and manually constructs a `<span>` with inline styles:

```jsx
// FarmListingsPage.jsx lines 70–85  (also duplicated in SavedFarms.jsx lines 33–48)
{c.name === "KOSHER" && (
    <span style={{ ...styles.badge, ...styles.badgeKosher }}>Kosher</span>
)}
{c.name === "HALAL" && (
    <span style={{ ...styles.badge, ...styles.badgeHalal }}>Halal</span>
)}
{c.name === "ORGANIC" && (
    <span style={{ ...styles.badge, ...styles.badgeOrganic }}>Organic</span>
)}
{c.name === "GRASS_FED" && (
    <span style={{ ...styles.badge, ...styles.badgeGrassFed }}>Grass-Fed</span>
)}
{c.name === "NON_GMO" && (
    <span style={{ ...styles.badge, ...styles.badgeNonGmo }}>Non-GMO</span>
)}
```

Both files also define nearly identical `badge`, `badgeKosher`, `badgeHalal`, `badgeOrganic`, `badgeGrassFed`, and `badgeNonGmo` style objects at the bottom of the file. This means any new certification type requires changes in two separate files, and any styling update must be made twice.

Note: `FarmListing.jsx` (the detail page) has already solved this correctly using a `BADGE_STYLES` lookup table and a `CERT_LABELS` map. The fix exists in the codebase — it just wasn't applied to the other two pages.

**Proposed Fix:**

Extract the badge logic into a shared component and import `BADGE_STYLES` / `CERT_LABELS` from a shared module.

**Step 1** — Create `uiserver/src/components/CertBadge.jsx`:
```jsx
// uiserver/src/components/CertBadge.jsx
const BADGE_STYLES = {
  KOSHER:                  { backgroundColor: '#e3f2fd', color: '#1565c0' },
  HALAL:                   { backgroundColor: '#fff3e0', color: '#e65100' },
  ORGANIC:                 { backgroundColor: '#e8f5e9', color: '#2e7d32' },
  GRASS_FED:               { backgroundColor: '#f1f8e9', color: '#558b2f' },
  NON_GMO:                 { backgroundColor: '#fce4ec', color: '#880e4f' },
  ANIMAL_WELFARE_APPROVED: { backgroundColor: '#ede7f6', color: '#4527a0' },
  CONVENTIONAL:            { backgroundColor: '#f5f5f5', color: '#555555' },
};

const CERT_LABELS = {
  KOSHER: 'Kosher', HALAL: 'Halal', ORGANIC: 'Organic',
  GRASS_FED: 'Grass-Fed', NON_GMO: 'Non-GMO',
  ANIMAL_WELFARE_APPROVED: 'Animal Welfare Approved',
  CONVENTIONAL: 'Conventional',
};

const badgeBase = {
  display: 'inline-block', padding: '3px 10px', borderRadius: '99px',
  fontSize: '12px', fontWeight: '700', textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

export default function CertBadge({ certName }) {
  const style = BADGE_STYLES[certName] || { backgroundColor: '#eee', color: '#555' };
  const label = CERT_LABELS[certName] || certName;
  return <span style={{ ...badgeBase, ...style }}>{label}</span>;
}
```

**Step 2** — In both `FarmListingsPage.jsx` and `SavedFarms.jsx`, replace the if-chain with:
```jsx
import CertBadge from '../components/CertBadge';

// inside the map:
{(farm.certifications || []).map(c => (
  <li key={c.id}><CertBadge certName={c.name} /></li>
))}
```

---

### Smell 2 — Calling Java Getter Methods on Plain JSON Objects
**Category:** Inappropriate Intimacy / Bug
**Severity:** High (Runtime Error)
**Files:**
- `uiserver/src/pages/FarmListingsPage.jsx` line 92
- `uiserver/src/pages/SavedFarms.jsx` line 55

**Description:**

Both listing pages attempt to display a farm's profile picture using Java-style getter method calls on what is actually a plain JavaScript object deserialized from JSON:

```jsx
// FarmListingsPage.jsx line 92
<img src={farm.getUser().getProfilePicture()} ...

// SavedFarms.jsx line 55
<img src={farm.getUser().getProfilePicture()} ...
```

JSON deserialization produces plain objects. There are no `getUser()` or `getProfilePicture()` methods — these are Java accessor patterns that do not exist in JavaScript. Calling `.getUser()` throws `TypeError: farm.getUser is not a function` at runtime, which means farm images are completely broken on both listing pages.

The correct pattern is already used elsewhere in the codebase. For example, in `FarmListing.jsx`:
```jsx
farm.user?.profilePicture
```

**Proposed Fix:**

Replace both broken method call chains with the correct JavaScript property access:

```jsx
// Before (FarmListingsPage.jsx line 91-93 and SavedFarms.jsx line 54-56):
<div style={styles.farmImage}>{user?.profilePicture ? (
    <img src={farm.getUser().getProfilePicture()} ...
```

```jsx
// After:
<div style={styles.farmImage}>{farm.user?.profilePicture ? (
    <img src={farm.user.profilePicture} alt="farm_photo"
         style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
) : (
    <span style={{ color: '#aaa', fontSize: '14px' }}>No image</span>
)}
```

Note the additional bug: the original code checks `user?.profilePicture` (the *logged-in buyer's* picture) to decide whether to render the *farm's* image — which is wrong. The condition should check `farm.user?.profilePicture`.

---

### Smell 3 — React Component Named with Lowercase Letter
**Category:** Naming Convention Violation
**Severity:** Medium
**File:** `uiserver/src/pages/SavedFarms.jsx` line 8

**Description:**

The React component is defined with a lowercase function name:

```js
// SavedFarms.jsx line 8
function savedFarms(){
```

React's rules require all component names to start with an **uppercase letter**. React uses this naming convention to distinguish between HTML native elements (lowercase like `<div>`, `<span>`) and custom components (uppercase like `<SavedFarms>`). If this component were ever referenced in JSX as `<savedFarms />`, React would treat it as an unknown HTML element and render nothing.

The component currently works only because it is always invoked via the React Router as a route element, not directly in JSX. But this is fragile — any future usage in JSX would silently fail.

**Proposed Fix:**

Rename the function declaration:

```js
// Before:
function savedFarms(){

// After:
function SavedFarms(){
```

Also update the export at the bottom:
```js
// Before:
export default savedFarms;

// After:
export default SavedFarms;
```

---

### Smell 4 — Silent Error Handling (`catch(console.error)`)
**Category:** Improper Error Handling
**Severity:** Medium
**Files:**
- `uiserver/src/pages/FarmListingsPage.jsx` line 29
- `uiserver/src/pages/SavedFarms.jsx` line 21
- `uiserver/src/pages/FarmListing.jsx` lines 53–56

**Description:**

All three farm-related pages swallow API errors silently:

```js
// FarmListingsPage.jsx line 29
getAllFarms().then(setFarms).catch(console.error);

// SavedFarms.jsx line 21
getSavedFarms().then(setFarms).catch(console.error);

// FarmListing.jsx lines 52–56
getFarm(farmname).then((f) => {
  setFarm(f);
  if (f) getCowTypes(f.id).then(setCowTypes).catch(console.error);
}).catch(console.error);
```

When the backend is unavailable or returns an error, the user sees an empty page with no explanation. The error is only visible in the browser developer console, which no regular user will ever check. This is especially bad for the farm listings page — a user would see a blank list and assume there are no farms.

**Proposed Fix:**

Add an `error` state variable and display a user-facing message on failure:

```jsx
// FarmListingsPage.jsx
const [error, setError] = useState(null);

useEffect(() => {
  getAllFarms()
    .then(setFarms)
    .catch((err) => {
      console.error(err);
      setError('Could not load farm listings. Please try again later.');
    });
}, []);

// In JSX, replace the empty state paragraph with:
{error
  ? <p style={styles.emptyState}>{error}</p>
  : filteredFarms.length === 0
    ? <p style={styles.emptyState}>No farms match your current filters.</p>
    : <ul>{listItems}</ul>
}
```

---

### Smell 5 — Hardcoded Fixed Pixel Widths for Farm Cards
**Category:** Magic Numbers / Non-Responsive Layout
**Severity:** Medium
**Files:**
- `uiserver/src/pages/FarmListingsPage.jsx` lines 304–315
- `uiserver/src/pages/SavedFarms.jsx` lines 147–158

**Description:**

Farm card buttons have fixed pixel widths and heights hard-coded into the style objects:

```js
// FarmListingsPage.jsx lines 304–315
button: {
    width: 1100,
    height: 300,
    ...
}

// SavedFarms.jsx lines 147–158  — different value, inconsistency
button: {
    width: 1200,
    height: 300,
    ...
}
```

Two problems:
1. **Non-responsive:** On screens narrower than 1100px (e.g., a laptop), the cards overflow and create a horizontal scrollbar.
2. **Inconsistency:** `FarmListingsPage` uses `1100px` and `SavedFarms` uses `1200px` for what should be the same component. This inconsistency has no justification and indicates the second was copy-pasted and the value adjusted arbitrarily.

**Proposed Fix:**

Replace fixed dimensions with responsive values. Also unify the value across both files:

```js
// Both files — replace the button style with:
button: {
    display: 'flex',
    flexDirection: 'row',
    backgroundColor: 'white',
    width: '100%',          // fills the container instead of fixed px
    maxWidth: '1000px',     // caps width on large screens
    minHeight: '160px',     // minimum height instead of fixed
    border: '1px solid #ddd',
    borderRadius: 10,
    margin: '8px 0',
    cursor: 'pointer',
    fontFamily: 'Roboto, sans-serif',
}
```

---

### Smell 6 — `getAllFarms` Duplicated in `ratings.js`
**Category:** Duplicated Code (DRY Violation)
**Severity:** Low-Medium
**Files:**
- `uiserver/src/api/farm.js` lines 5–8
- `uiserver/src/api/ratings.js` lines 6–9

**Description:**

`ratings.js` defines its own private copy of `getAllFarms()` that is byte-for-byte identical to the exported one in `farm.js`:

```js
// ratings.js lines 6–9 — private duplicate
async function getAllFarms() {
    const response = await fetch('/api/seller/all');
    return response.json();
}

// farm.js lines 5–8 — original
export async function getAllFarms() {
    const response = await fetch('/api/seller/all');
    return response.json();
}
```

This means if the `/api/seller/all` endpoint URL ever changes, or if fetch options (e.g., auth headers) need to be added, it must be updated in two places. This is a classic DRY violation.

**Proposed Fix:**

Delete the local copy from `ratings.js` and import from `farm.js`:

```js
// ratings.js — before line 6, add import:
import { getAllFarms } from './farm';

// Then delete lines 6–9 (the local duplicate function)
```

---

### Smell 7 — `getFarm(id)` Fetches All Farms to Find One
**Category:** Inefficient Algorithm / Lazy Class
**Severity:** Medium
**File:** `uiserver/src/api/farm.js` lines 12–15

**Description:**

There is no backend endpoint for a single farm by ID, so the frontend fetches the entire farm list on every detail page load just to find one record:

```js
// farm.js lines 12–15
export async function getFarm(id) {
    const farms = await getAllFarms();           // fetches ALL sellers from DB
    return farms.find((f) => f.id === Number(id));
}
```

This makes a network request that returns potentially hundreds of records, sends all of them over the wire, and then discards all but one. As the number of farms grows, this will become progressively slower. It also means the `FarmListing.jsx` detail page makes two full-list fetches on load (one for `getFarm`, one for `getSavedFarms`).

**Proposed Fix — Frontend (immediate):**

Memoize or cache the `getAllFarms` result in a React context so that navigation between pages does not re-fetch the list each time.

**Proposed Fix — Backend (correct fix):**

Add a public endpoint to `SellerController.java` that returns a single farm by ID:

```java
// SellerController.java — add new endpoint
@GetMapping("/{sellerId}")
public ResponseEntity<?> getFarmById(@PathVariable Long sellerId) {
    try {
        List<Seller> all = sellerService.getAllFarms();
        return all.stream()
            .filter(s -> s.getId().equals(sellerId))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    } catch (RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
```

Then update `farm.js`:
```js
export async function getFarm(id) {
    const response = await fetch(`/api/seller/${id}`);
    if (!response.ok) throw new Error('Farm not found');
    return response.json();
}
```

---

### Smell 8 — Long if-else Chain for Sorting (Switch Statement Smell)
**Category:** Switch Statements
**Severity:** Low-Medium
**File:** `uiserver/src/pages/FarmListingsPage.jsx` lines 54–62

**Description:**

The sorting logic uses a chain of four `else if` branches, each containing an inline sort comparator:

```js
// FarmListingsPage.jsx lines 54–62
if(sortBy === 'name-asc'){
    filteredFarms = [...filteredFarms].sort((a,b) => (a.shopName || '').localeCompare(b.shopName || ''));
} else if (sortBy === 'name-desc'){
    filteredFarms = [...filteredFarms].sort((a,b) => (b.shopName || '').localeCompare(a.shopName || ''));
} else if (sortBy === 'rating-high'){
    filteredFarms = [...filteredFarms].sort((a, b) => b.averageRating - a.averageRating);
} else if (sortBy === 'rating-low'){
    filteredFarms = [...filteredFarms].sort((a, b) => a.averageRating - b.averageRating);
}
```

Adding a new sort option (e.g., "Nearest First" for distance sorting) requires adding another `else if` branch. This violates the Open/Closed Principle — the code is not closed for modification.

**Proposed Fix:**

Replace with a comparator lookup table:

```js
// Add near the top of the component (or outside it):
const SORT_COMPARATORS = {
  'name-asc':    (a, b) => (a.shopName || '').localeCompare(b.shopName || ''),
  'name-desc':   (a, b) => (b.shopName || '').localeCompare(a.shopName || ''),
  'rating-high': (a, b) => b.averageRating - a.averageRating,
  'rating-low':  (a, b) => a.averageRating - b.averageRating,
};

// Replace the if-else chain with:
const comparator = SORT_COMPARATORS[sortBy];
if (comparator) filteredFarms = [...filteredFarms].sort(comparator);
```

Adding a new sort option now only requires adding one entry to `SORT_COMPARATORS` — no control flow changes needed.

---

### Smell 9 — Geographic Distance Feature Dead Code (Speculative Generality)
**Category:** Dead Code / Speculative Generality
**Severity:** Medium
**Files:**
- `backend/.../util/DistanceUtil.java` — Haversine formula
- `backend/.../util/ZipCodeUtil.java` — ZIP-to-coordinates lookup
- `backend/.../model/User.java` — stores `latitude`, `longitude`, `city`, `state`, `country`, `zipCode`

**Description:**

The backend has a complete, non-trivial geographic distance stack: `ZipCodeUtil` resolves ZIP codes to coordinates via external APIs (Zippopotam.us, Nominatim), and `DistanceUtil` calculates Haversine great-circle distances in miles. The `User` model stores `latitude` and `longitude` for both buyers and sellers.

However, **none of this is connected to any API endpoint or frontend feature**. The `FarmListingsPage.jsx` filter panel has Category, Min Rating, and Sort By filters — but no distance filter. No endpoint in `SellerController` accepts a buyer location or returns distances. The utility classes are called from nowhere in the production codebase.

This is classic **Speculative Generality**: infrastructure built in anticipation of a feature that was never wired up, leaving dead code that adds maintenance burden and misleads future developers into thinking distance filtering is working.

**Proposed Fix Option A (implement the feature):**

Add a "Max Distance" filter to `FarmListingsPage.jsx`:
```jsx
// In filter panel:
<label style={styles.filterLabel}>Max Distance</label>
<select style={styles.filterSelect} value={maxDistance}
        onChange={e => setMaxDistance(Number(e.target.value))}>
  <option value={0}>Any</option>
  <option value={25}>Within 25 mi</option>
  <option value={50}>Within 50 mi</option>
  <option value={100}>Within 100 mi</option>
</select>
```

Add a backend endpoint that returns farms with computed distance for a given buyer:
```
GET /api/seller/all?buyerUserId=123
```

**Proposed Fix Option B (document as planned feature):**

If distance filtering is not yet scheduled, mark the utilities with a clear comment:
```java
// TODO: Distance-based farm filtering (planned feature, not yet wired to API)
// These utilities are ready but not connected to any controller endpoint.
```
And open a GitHub issue tracking the implementation work.

---

### Smell 10 — `RuntimeException` Used for All Business Logic Errors
**Category:** Inappropriate Exception Type
**Severity:** Medium
**Files:**
- `backend/.../service/SellerService.java` lines 31, 36, 51, 63
- `backend/.../service/BuyerService.java` lines 32, 36, 52, 61
- `backend/.../service/RatingService.java` lines 45, 61, 70, 74, 79, 83

**Description:**

All business logic errors across all three services use the same generic `RuntimeException`:

```java
// SellerService.java
throw new RuntimeException("User not found");
throw new RuntimeException("Seller profile already exists");

// RatingService.java
throw new RuntimeException("Score must be between 1 and 5.");
throw new RuntimeException("Invalid or already-used code.");
throw new RuntimeException("You have already rated this seller.");
```

Using `RuntimeException` for all error cases means:
1. The controller cannot distinguish between "not found" (should return HTTP 404) and "duplicate" (should return HTTP 409) and "validation failed" (should return HTTP 400) — all end up mapped to 400 Bad Request.
2. Any unexpected `RuntimeException` from the database or JPA layer could accidentally be caught and treated as a business error.
3. Code is harder to read — the type tells you nothing about the nature of the failure.

**Proposed Fix:**

Define custom exception classes:

```java
// backend/.../exception/ResourceNotFoundException.java
package com.prym.backend.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

// backend/.../exception/DuplicateEntityException.java
package com.prym.backend.exception;
public class DuplicateEntityException extends RuntimeException {
    public DuplicateEntityException(String message) { super(message); }
}

// backend/.../exception/ValidationException.java
package com.prym.backend.exception;
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
```

Then replace in services:
```java
// SellerService.java
.orElseThrow(() -> new ResourceNotFoundException("User not found"));
throw new DuplicateEntityException("Seller profile already exists");

// RatingService.java
throw new ValidationException("Score must be between 1 and 5.");
throw new ValidationException("Invalid or already-used code.");
```

And add a `@ControllerAdvice` to map them to the correct HTTP status codes:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<?> handleDuplicate(DuplicateEntityException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidation(ValidationException e) {
        return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
    }
}
```

---

### Smell 11 — Fully Qualified Class Name Used Instead of Import
**Category:** Code Style / Inconsistent Imports
**Severity:** Low
**File:** `backend/.../controller/SellerController.java` line 65

**Description:**

In `getSeller()`, a `LinkedHashMap` is created using its fully qualified class name inline instead of importing it:

```java
// SellerController.java line 65
java.util.Map<String, Object> profileMap = new java.util.LinkedHashMap<>();
```

The file already imports `java.util.Map` (line 13) and `java.util.List` (line 12), so `java.util` imports are clearly known. The `LinkedHashMap` was added without adding a proper import, leaving an inconsistent and harder-to-read fully qualified reference in the middle of a method body.

**Proposed Fix:**

Add the import at the top of the file:
```java
import java.util.LinkedHashMap;
```

Then change line 65 to:
```java
Map<String, Object> profileMap = new LinkedHashMap<>();
```

---

### Smell 12 — Average Rating Computed in Application Code Instead of Database
**Category:** Inappropriate Data Layer Responsibility / Data Integrity Risk
**Severity:** Medium
**File:** `backend/.../service/RatingService.java` lines 91–95

**Description:**

After a rating is submitted, the average is recalculated in Java and stored as a denormalized field on the `Seller` entity:

```java
// RatingService.java lines 91–95
int newTotal = seller.getTotalRatings() + 1;
double newAverage = ((seller.getAverageRating() * seller.getTotalRatings()) + score) / newTotal;
seller.setAverageRating(newAverage);
seller.setTotalRatings(newTotal);
sellerRepository.save(seller);
```

This approach has two risks:
1. **Floating-point drift:** Incrementally updating a running average with `double` arithmetic accumulates rounding errors over hundreds of ratings. After many ratings, `averageRating` may diverge from what `AVG(score)` would produce by a small but nonzero amount.
2. **Stale data:** If a `Rating` record is ever deleted, corrected, or migrated, `averageRating` becomes incorrect with no way to self-heal without a full recalculation.

**Proposed Fix:**

Replace the manual running average with a query-time `AVG` from the database. Add a method to `RatingRepository`:

```java
// RatingRepository.java
@Query("SELECT AVG(r.score) FROM Rating r WHERE r.seller = :seller")
Double calculateAverageRating(@Param("seller") Seller seller);

@Query("SELECT COUNT(r) FROM Rating r WHERE r.seller = :seller")
int countRatings(@Param("seller") Seller seller);
```

Then in `RatingService.java`, replace the manual calculation:
```java
// After saving the new Rating:
ratingRepository.save(rating);

// Replace manual math with DB aggregate:
Double avg = ratingRepository.calculateAverageRating(seller);
int total = ratingRepository.countRatings(seller);
seller.setAverageRating(avg != null ? avg : 0.0);
seller.setTotalRatings(total);
sellerRepository.save(seller);
```

---

### Smell 13 — Filter Computations Run on Every Render Without Memoization
**Category:** Performance / Long Component Body
**Severity:** Low
**File:** `uiserver/src/pages/FarmListingsPage.jsx` lines 35–65

**Description:**

The search filtering, category filtering, rating filtering, and sorting all run in the component body (not inside a `useMemo`):

```js
// FarmListingsPage.jsx lines 35–65
let filteredFarms = farms.filter(farm =>
    (farm.shopName || '').toLowerCase().includes(searchQuery.toLowerCase())
)
if(selectedCategory){ filteredFarms = filteredFarms.filter(...) }
if(minRating > 0){ filteredFarms = filteredFarms.filter(...) }
if(sortBy === 'name-asc'){ filteredFarms = [...filteredFarms].sort(...) }
// ... etc
```

In React, everything in the component body re-runs on **every render** — including unrelated state changes. Filtering and sorting a large array of farms on every keypress in the search bar (which triggers multiple renders for each character) is unnecessary work.

**Proposed Fix:**

Wrap the filtering logic in `useMemo` so it only recomputes when the relevant inputs change:

```jsx
import { useState, useEffect, useMemo } from 'react';

const filteredFarms = useMemo(() => {
  let result = farms.filter(farm =>
    (farm.shopName || '').toLowerCase().includes(searchQuery.toLowerCase())
  );
  if (selectedCategory) {
    result = result.filter(farm =>
      (farm.certifications || []).some(c => c.name === selectedCategory)
    );
  }
  if (minRating > 0) {
    result = result.filter(farm => farm.averageRating >= minRating);
  }
  const comparator = SORT_COMPARATORS[sortBy];
  if (comparator) result = [...result].sort(comparator);
  return result;
}, [farms, searchQuery, selectedCategory, minRating, sortBy]);
```

---

### Smell 14 — Compare Farms Feature Completely Absent (Missing Implementation)
**Category:** Missing Feature / Incomplete User Story
**Severity:** High
**Files:** None found

**Description:**

User Story 3, "Compare Farms," has no implementation anywhere in the codebase. There is:
- No comparison page or component
- No route registered for comparison
- No "Compare" checkbox or button on farm cards
- No side-by-side view of two or more farms
- No state management for a "compare selection"

The listings page shows farm cards individually, and a buyer can only view one farm at a time by clicking through to the detail page. There is no mechanism to select multiple farms and view their attributes (price, certifications, ratings, cattle types) side-by-side.

This is not a code smell in the traditional sense, but it is a **missing implementation** that would be caught by acceptance testing of this user story.

**Proposed Fix:**

At minimum, implement:

1. **Checkbox selection on farm cards** in `FarmListingsPage.jsx`:
```jsx
const [compareIds, setCompareIds] = useState(new Set());

// In each farm card:
<input type="checkbox" checked={compareIds.has(farm.id)}
  onChange={() => setCompareIds(prev => {
    const next = new Set(prev);
    next.has(farm.id) ? next.delete(farm.id) : next.add(farm.id);
    return next;
  })}
/>
```

2. **Compare button** that appears when 2+ farms are selected, navigates to `/buyer/compare?ids=1,2`.

3. **`ComparePage.jsx`** that fetches the selected farms by ID and renders them in a side-by-side table showing: shop name, rating, certifications, address, cattle types, and price range.

---

## Summary Table

| # | Smell | Category | Severity | Files |
|---|-------|----------|----------|-------|
| 1 | Duplicated badge rendering logic | Duplicated Code | Medium | FarmListingsPage.jsx, SavedFarms.jsx |
| 2 | Calling `.getUser().getProfilePicture()` on JSON | Inappropriate Intimacy / Bug | **High** | FarmListingsPage.jsx:92, SavedFarms.jsx:55 |
| 3 | React component name starts with lowercase | Naming Convention | Medium | SavedFarms.jsx:8 |
| 4 | Silent `catch(console.error)` error handling | Improper Error Handling | Medium | FarmListingsPage.jsx:29, SavedFarms.jsx:21 |
| 5 | Hardcoded 1100px/1200px card widths | Magic Numbers | Medium | FarmListingsPage.jsx:311, SavedFarms.jsx:153 |
| 6 | `getAllFarms` duplicated in ratings.js | Duplicated Code | Low-Medium | ratings.js:6 |
| 7 | `getFarm()` fetches all farms to get one | Inefficient Algorithm | Medium | farm.js:13 |
| 8 | if-else chain for sort options | Switch Statement | Low-Medium | FarmListingsPage.jsx:54–62 |
| 9 | Distance utilities never connected to UI | Dead Code | Medium | DistanceUtil.java, ZipCodeUtil.java |
| 10 | `RuntimeException` for all business errors | Inappropriate Exception | Medium | SellerService, BuyerService, RatingService |
| 11 | `LinkedHashMap` via full class name in method | Inconsistent Imports | Low | SellerController.java:65 |
| 12 | Running average computed in Java, not DB | Data Layer Responsibility | Medium | RatingService.java:91–95 |
| 13 | Filter/sort runs on every render without memo | Performance | Low | FarmListingsPage.jsx:35–65 |
| 14 | Compare Farms user story not implemented | Missing Implementation | **High** | (none) |

---

## Commits for Proposed Fixes

The following changes were committed to address the code smells found:

| Commit | Change | Smell Fixed |
|--------|--------|-------------|
| `fix: use correct JSON property access for farm images` | Replace `farm.getUser().getProfilePicture()` with `farm.user?.profilePicture` in FarmListingsPage.jsx and SavedFarms.jsx | Smell #2 |
| `fix: rename savedFarms component to SavedFarms (PascalCase)` | Rename function and export in SavedFarms.jsx | Smell #3 |
| `refactor: remove duplicate getAllFarms from ratings.js` | Import from farm.js instead | Smell #6 |
| `refactor: replace sort if-else chain with lookup table` | Add `SORT_COMPARATORS` map in FarmListingsPage.jsx | Smell #8 |
| `fix: import LinkedHashMap in SellerController` | Add missing import statement | Smell #11 |

Larger fixes (custom exceptions, DB aggregate for ratings, Compare page) are proposed as follow-up issues due to scope.
