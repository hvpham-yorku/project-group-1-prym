export async function getAllFarms() {
	const response = await fetch('/api/seller/all');
	return response.json();
}

export async function getFarm(id) {
	const farms = await getAllFarms();
	return farms.find((f) => f.id === Number(id));
}

export async function getSavedFarms(){
	const response = await fetch('/api/buyer/all', { credentials: 'include' });
	return response.json();
}

export async function saveFarm(farm){
	const response = await fetch("/api/buyer/all", {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(farm)
	});
	return response.json();
}

export async function removeSavedFarm(farm){
	const response = await fetch("/api/buyer/all", {
		method: 'DELETE',
		credentials: 'include'
	});
	return response.json();
}
