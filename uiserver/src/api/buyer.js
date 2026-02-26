// API helper for buyer profile endpoints
// This file gives the frontend a way to communicate with the buyer controller on the backend
// Each function maps to one endpoint in BuyerController.java

const API_URL = '/api/buyer';

// Creates a new buyer profile after signup
// Calls POST /api/buyer/profile
// profileData contains: userId, preferredCuts, quantity
export async function createBuyerProfile(profileData) {
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

// Fetches an existing buyer profile by user ID
// Calls GET /api/buyer/profile/{userId}
export async function getBuyerProfile(userId) {
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

// Updates an existing buyer profile
// Calls PUT /api/buyer/profile/{userId}
// profileData contains: preferredCuts, quantity
export async function updateBuyerProfile(userId, profileData) {
    const response = await fetch(`${API_URL}/profile/${userId}`, {
        method: 'PUT',
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
