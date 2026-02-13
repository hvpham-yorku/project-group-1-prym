import axios from 'axios';

export const getSellerProfile = async (sellerId) => {
    const response = await axios.get(`/api/sellers/${sellerId}`, {
        withCredentials: true
    });
    return response.data;
};

export const updateSellerProfile = async (sellerId, data) => {
    const response = await axios.patch(`/api/sellers/${sellerId}`, data, {
        withCredentials: true
    });
    return response.data;
};