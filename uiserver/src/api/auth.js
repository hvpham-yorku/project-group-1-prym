const API_URL = 'http://localhost:8080/api/auth';

export async function registerBuyer(userData) { // userData: { email, password, username, firstName, lastName, phoneNumber, profilePicture (optional) }

    const response = await fetch(`${API_URL}/register/buyer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(userData)
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Registration failed');
    }

    return data;
}

export async function registerSeller(userData) {
    const response = await fetch(`${API_URL}/register/seller`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(userData)
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Registration failed');
    }

    return data;
}

export async function login(email, password) {
    const response = await fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password })
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Login failed');
    }

    return data;
}

export async function logout() {
    const response = await fetch(`${API_URL}/logout`, {
        method: 'POST',
        credentials: 'include'
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Logout failed');
    }

    return data;
}

export async function getCurrentUser() {
    const response = await fetch(`${API_URL}/me`, {
        method: 'GET',
        credentials: 'include'
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Not logged in');
    }

    return data;
}