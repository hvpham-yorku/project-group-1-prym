import {useState, useEffect} from 'react';

export function getFarm(name){
	const farms = getAllFarms();
	return farms.find((f) => f.shopName === name);
}

export function getAllFarms(){
	const [farms, setFarms] = useState([]);
		
		useEffect(() => {
			fetch('/api/seller/all')
			.then(res => res.json())
			.then(data => setFarms(data))
			.catch(err => console.error("Failed to load farms", err))
		}, []);
	return farms;
}
