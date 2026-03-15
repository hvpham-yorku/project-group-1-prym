// API helpers for the group buying feature (Model 2).
// Groups are standalone entities — not tied to any specific seller cow.

const API_URL = '/api/buyer';

// POST /api/buyer/groups/create
// name: string, certifications: comma-separated string e.g. "KOSHER,ORGANIC"
export async function createGroup(userId, name, certifications) {
    const response = await fetch(`${API_URL}/groups/create`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId, name, certifications }),
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to create group');
    return data;
}

// GET /api/buyer/groups?userId={userId}
// Returns all groups the buyer has NOT joined yet.
export async function getAvailableGroups(userId) {
    const response = await fetch(`${API_URL}/groups?userId=${userId}`, {
        method: 'GET',
        credentials: 'include',
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to load available groups');
    return data;
}

// GET /api/buyer/groups/mine?userId={userId}
export async function getMyGroups(userId) {
    const response = await fetch(`${API_URL}/groups/mine?userId=${userId}`, {
        method: 'GET',
        credentials: 'include',
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to load your groups');
    return data;
}

// GET /api/buyer/groups/{groupId}?userId={userId}
export async function getGroup(userId, groupId) {
    const response = await fetch(`${API_URL}/groups/${groupId}?userId=${userId}`, {
        method: 'GET',
        credentials: 'include',
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to load group');
    return data;
}

// POST /api/buyer/groups/join/{groupId}
// Joins the group with no cuts — cuts are set later on the group page.
export async function joinGroup(userId, groupId) {
    const response = await fetch(`${API_URL}/groups/join/${groupId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId }),
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to join group');
    return data;
}

// POST /api/buyer/groups/cuts/{groupId}
// Saves the buyer's cut selections. cuts is a serialized string e.g. "Chuck, Rib x2".
export async function saveCuts(userId, groupId, cuts) {
    const response = await fetch(`${API_URL}/groups/cuts/${groupId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId, cuts }),
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to save cuts');
    return data;
}

// GET /api/buyer/groups/by-code/{code}?userId={userId}
// Looks up a group by invite code and returns its detail DTO for preview.
export async function getGroupByCode(userId, code) {
    const response = await fetch(`${API_URL}/groups/by-code/${encodeURIComponent(code)}?userId=${userId}`, {
        method: 'GET',
        credentials: 'include',
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Invalid invite code');
    return data;
}

// POST /api/buyer/groups/{groupId}/regenerate-code
export async function regenerateInviteCode(userId, groupId) {
    const response = await fetch(`${API_URL}/groups/${groupId}/regenerate-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId }),
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to regenerate code');
    return data;
}

// GET /api/buyer/groups/{groupId}/matching-farms?userId={userId}
// Returns { perfectMatches, partialMatches } — farms whose certs match the group's requirements.
export async function getMatchingFarms(userId, groupId) {
    const response = await fetch(`${API_URL}/groups/${groupId}/matching-farms?userId=${userId}`, {
        method: 'GET',
        credentials: 'include',
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to load matching farms');
    return data;
}

// POST /api/buyer/groups/leave/{groupId}
export async function leaveGroup(userId, groupId) {
    const response = await fetch(`${API_URL}/groups/leave/${groupId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId }),
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to leave group');
    return data;
}
