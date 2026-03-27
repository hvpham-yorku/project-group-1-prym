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

// Fetches cow types for a seller (for the dashboard)
// Calls GET /api/seller/cow-types/{userId}
export async function getSellerCowTypes(userId) {
    const response = await fetch(`${API_URL}/cow-types/${userId}`, {
        credentials: 'include'
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to get cow types');
    return data;
}

// Adds a new cow type to the seller's farm
// Calls POST /api/seller/cow-types/{userId}
export async function addCowType(userId, cowTypeData) {
    const response = await fetch(`${API_URL}/cow-types/${userId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(cowTypeData)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to add cow type');
    return data;
}

// Removes a cow type from the seller's farm
// Calls DELETE /api/seller/cow-types/{userId}/{cowTypeId}
export async function deleteCowType(userId, cowTypeId) {
    const response = await fetch(`${API_URL}/cow-types/${userId}/${cowTypeId}`, {
        method: 'DELETE',
        credentials: 'include'
    });
    if (response.status === 204) return {};
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Failed to delete cow type');
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


