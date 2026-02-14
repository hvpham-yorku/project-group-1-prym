import axios from 'axios';

export const getSellerProfile = async (userId) => { // Add userId here
    try {
        const response = await axios.get(`/api/seller/profile/${userId}`, { // Use backticks and add ID
            withCredentials: true 
        });
        return response.data;
    } catch (err) {
        throw err;
    }
};

// Create seller profile (for first-time sellers)
export const createSellerProfile = async (data) => {
    const response = await axios.post('/api/seller/profile', data, {
        withCredentials: true
    });
    return response.data;
};

// Update existing seller profile
export const updateSellerProfile = async (userId, data) => {
    const response = await axios.patch(`/api/seller/${userId}`, data, { // Should be /api/seller/ID, not /profile
        withCredentials: true
    });
    return response.data;
};
