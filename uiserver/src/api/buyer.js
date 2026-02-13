// API helper for buyer profile endpoints
// This file gives the frontend a way to communicate with the buyer controller on the backend
// Each function maps to one endpoint in BuyerController.java

const API_URL = 'http://localhost:8080/api/buyer';

// Creates a new buyer profile after signup
// Calls POST /api/buyer/profile
// profileData contains: userId, firstName, lastName, phoneNumber, preferredCuts, quantity
export async function createBuyerProfile(profileData) {
    const response = await fetch(`${API_URL}/profile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }, // tells the backend we're sending JSON
        credentials: 'include', // sends the session cookie so Spring Security lets us through
        body: JSON.stringify(profileData) // converts the JavaScript object to a JSON string
    });

    const data = await response.json(); // read the response from the backend

    if (!response.ok) {
        throw new Error(data.error || 'Failed to create profile'); // if something went wrong, throw an error
    }

    return data; // return the created buyer profile back to the React page
}

// Fetches an existing buyer profile by user ID
// Calls GET /api/buyer/profile/{userId}
// No body needed for GET requests, we just pass the userId in the URL
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
// profileData contains the updated fields
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
