// API helper for seller profile endpoints
// Each function maps to one endpoint in SellerController.java

const API_URL = '/api/seller';

// Creates a new seller profile after signup
// Calls POST /api/seller/profile
export async function createSellerProfile(profileData) {
    const response = await fetch(`${API_URL}/profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(profileData)
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Failed to create profile');
    }

    return data;
}

// Fetches an existing seller profile by user ID
// Calls GET /api/seller/profile/{userId}
export async function getSellerProfile(userId) {
    const response = await fetch(`${API_URL}/profile/${userId}`, {
        method: 'GET',
        credentials: 'include'
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Failed to get profile');
    }

    return data;
}

// Fetches certifications for a seller
// Calls GET /api/seller/certifications/{userId}
export async function getCertifications(userId) {
    const response = await fetch(`${API_URL}/certifications/${userId}`, {
        method: 'GET',
        credentials: 'include'
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to get certifications');
    return data;
}

// Replaces all certifications for a seller with the given list
// Calls PUT /api/seller/certifications/{userId}
export async function setCertifications(userId, certNames) {
    const response = await fetch(`${API_URL}/certifications/${userId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(certNames)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to set certifications');
    return data;
}

// Updates an existing seller profile
// Calls PATCH /api/seller/profile/{userId}
export async function updateSellerProfile(userId, profileData) {
    const response = await fetch(`${API_URL}/profile/${userId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(profileData)
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Failed to update profile');
    }

    return data;
}


