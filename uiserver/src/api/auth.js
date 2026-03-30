//API helpers for authentication — register, login, logout, session check, and account updates.
//All requests include credentials so the session cookie gets sent along.
const API_URL = '/api/auth';

//safely parses the response body as JSON, handles empty responses
//and non-JSON errors without crashing everything
async function parseResponse(response) {
    const text = await response.text();
    try {
        return text ? JSON.parse(text) : {};
    } catch {
        throw new Error(`Server error: ${response.status}`);
    }
}

//registers a new buyer account and auto-logs them in via session cookie
export async function registerBuyer(userData) { // userData: { email, password, username, firstName, lastName, phoneNumber, profilePicture (optional) }

    const response = await fetch(`${API_URL}/register/buyer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(userData)
    });

    const data = await parseResponse(response);

    if (!response.ok) {
        throw new Error(data.error || 'Registration failed');
    }

    return data;
}

//registers a new seller account, also creates an empty seller profile on the backend
export async function registerSeller(userData) {
    const response = await fetch(`${API_URL}/register/seller`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(userData)
    });

    const data = await parseResponse(response);

    if (!response.ok) {
        throw new Error(data.error || 'Registration failed');
    }

    return data;
}

//logs in with email and password, backend sets the session cookie
export async function login(email, password) {
    const response = await fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password })
    });

    const data = await parseResponse(response);

    if (!response.ok) {
        throw new Error(data.error || 'Login failed');
    }

    return data;
}

//tells the backend to kill the session, cookie gets cleared
export async function logout() {
    const response = await fetch(`${API_URL}/logout`, {
        method: 'POST',
        credentials: 'include'
    });

    const data = await parseResponse(response);

    if (!response.ok) {
        throw new Error(data.error || 'Logout failed');
    }

    return data;
}

//checks if the user is still logged in by validating their session cookie
//called on page load by AuthContext to restore the user state
export async function getCurrentUser() {
    const response = await fetch(`${API_URL}/me`, {
        method: 'GET',
        credentials: 'include'
    });

    const data = await parseResponse(response);

    if (!response.ok) {
        throw new Error(data.error || 'Not logged in');
    }

    return data;
}

//updates basic account info like name, email, pic, zip code etc
//used by the edit account modal on both dashboards
export async function updateUserInfo({firstName, lastName, email, username, phoneNumber, profilePicture, zipCode}){
    const response = await fetch(`${API_URL}/user`,{
        method: 'PATCH',
        headers: {'Content-Type': 'application/json'},
        credentials: 'include',
        body: JSON.stringify({firstName, lastName, email, username, phoneNumber, profilePicture, zipCode})
    });
    
    const data = await response.json();
    if(!response.ok){
        throw new Error(data.error || 'Failed to update account info');
    }
    return data;
}
