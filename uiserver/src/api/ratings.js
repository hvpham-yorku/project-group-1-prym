//API helpers for the rating system.
//Ratings are a two step thing: seller generates a code, buyer redeems it.

import { getAllFarms } from './farm';

//gets ratings for a farm by seller id. Has to look up the username first
//because the backend ratings endpoint is keyed by username for some reason
export async function getFarmRatings(sellerId){
    const farms = await getAllFarms();
    const farm = farms.find((f) => f.id === Number(sellerId));
    if(!farm) throw new Error('Farm not found');
    const username = farm.user.username;
    const response = await fetch(`/api/ratings/${username}`);
    return response.json();
}

//seller generates a one time code
export async function generateRatingCode(userId){
    const response = await fetch('/api/ratings/generate-code',{
        method:'POST',
        headers: {'Content-Type':'application/json'},
        credentials: 'include',
        body: JSON.stringify({userId}),
    });
    return response.json();
}

//buyer submits a rating using the code
export async function submitRating(userId, code, score){
    const response = await fetch('/api/ratings/submit',{
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        credentials: 'include',
        body: JSON.stringify({userId, code, score}),
    });
    return response.json();
}
