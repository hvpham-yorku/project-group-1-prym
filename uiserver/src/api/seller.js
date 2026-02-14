import axios from 'axios';

export const getSellerProfile = async () => {
    try {
        const response = await axios.get('/api/sellers/profile', {
            withCredentials: true // sends cookies for auth
        });
        return response.data; // only return the profile data
    } catch (err) {
        // If profile doesn't exist, backend might return 404
        throw err;
    }
};

// Create seller profile (for first-time sellers)
export const createSellerProfile = async (data) => {
    const response = await axios.post('/api/sellers/profile', data, {
        withCredentials: true
    });
    return response.data;
};

// Update existing seller profile
export const updateSellerProfile = async (data) => {
    const response = await axios.patch('/api/sellers/profile', data, {
        withCredentials: true
    });
    return response.data;
};
