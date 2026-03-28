// API helpers for group-seller association requests.

// ── Buyer-side ────────────────────────────────────────────────────────────────

// POST /api/buyer/groups/{groupId}/associate/{sellerId}
export async function requestAssociation(userId, groupId, sellerId) {
    const res = await fetch(`/api/buyer/groups/${groupId}/associate/${sellerId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to send association request');
    return data;
}

// POST /api/buyer/groups/{groupId}/associate/cancel
export async function cancelAssociation(userId, groupId) {
    const res = await fetch(`/api/buyer/groups/${groupId}/associate/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to cancel association request');
    return data;
}

// POST /api/buyer/groups/{groupId}/disassociate
export async function requestDisassociation(userId, groupId) {
    const res = await fetch(`/api/buyer/groups/${groupId}/disassociate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to request disassociation');
    return data;
}

// GET /api/buyer/groups/{groupId}/association?userId={userId}
// Returns the current association object, or {} if none.
export async function getGroupAssociation(userId, groupId) {
    const res = await fetch(`/api/buyer/groups/${groupId}/association?userId=${userId}`, {
        credentials: 'include',
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to fetch association');
    return data;
}

// ── Seller-side ───────────────────────────────────────────────────────────────

// GET /api/seller/associations/pending?userId={userId}
export async function getSellerPendingRequests(userId) {
    const res = await fetch(`/api/seller/associations/pending?userId=${userId}`, {
        credentials: 'include',
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to fetch pending requests');
    return data;
}

// GET /api/seller/associations?userId={userId}
export async function getSellerAssociations(userId) {
    const res = await fetch(`/api/seller/associations?userId=${userId}`, {
        credentials: 'include',
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to fetch associations');
    return data;
}

// POST /api/seller/associations/{associationId}/respond
// action: "APPROVE" | "DENY", note: optional string
export async function respondToAssociation(userId, associationId, action, note = '') {
    const res = await fetch(`/api/seller/associations/${associationId}/respond`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId, action, note }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to respond to association');
    return data;
}

// GET /api/seller/associations/{associationId}/messages?userId={userId}
// Returns { groupId, messages: [...] } for an active association.
export async function getAssociationMessages(userId, associationId) {
    const res = await fetch(`/api/seller/associations/${associationId}/messages?userId=${userId}`, {
        credentials: 'include',
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to fetch messages');
    return data;
}

// POST /api/seller/associations/{associationId}/respond-disassociation
// action: "CONFIRM" | "DENY"
export async function respondToDisassociation(userId, associationId, action) {
    const res = await fetch(`/api/seller/associations/${associationId}/respond-disassociation`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ userId, action }),
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to respond to disassociation');
    return data;
}
