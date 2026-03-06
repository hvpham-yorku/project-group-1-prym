import {useState, useEffect} from 'react';

/*export function getAllFarms(){
	const [farms, setFarms] = useState([]);
		
	useEffect(() => {
		fetch('api/seller/all')
		.then(res => res.json())
		.then(data => setFarms(data))
		.catch(err => console.error("Failed to load farms", err))
	}, []);
		
	
	
	return farms;
}*/


export function getAllFarms(){
	const [farms, setFarms] = useState([]);
	
	useEffect(() => {
		const fetchData = async () => {
			const response = await fetch('/api/seller/all');
			const data = await response.json();
			setFarms(data);
		};
		fetchData().catch(console.error);
	}, [])
	
	console.log(Array.isArray(farms));
	
	return farms;
}

export function getFarm(name){
	const farms = getAllFarms();
	console.log(farms);
	return farms.find((f) => f.shopName === name);
}


