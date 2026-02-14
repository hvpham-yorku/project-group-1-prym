import axios from 'axios';

export const getSellerProfile = async (userId) => {
const response = await axios.get(`/api/seller/profile/${userId}`, {        withCredentials: true // sends cookies for auth
    });
    return response.data; // only return the profile data
};

// Create seller profile (for first-time sellers)
export const createSellerProfile = async (data) => {
    const response = await axios.post('/api/seller/profile', data, {
        withCredentials: true
    });
    return response.data;
};

export const updateSellerProfile = async (userId, data) => {
    const response = await axios.patch(`/api/seller/${userId}`, data, {
        withCredentials: true
    });
    return response.data;
};