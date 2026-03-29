//API helpers for farm listings and saved farms.
//These talk to both /api/seller (for listings) and /api/buyer (for saving farms).

//grabs every single farm/seller from the backend, no auth needed
export async function getAllFarms() {
	const response = await fetch('/api/seller/all');
	return response.json();
}

//finds one farm by id using the dedicated single-farm endpoint
export async function getFarm(id) {
	const response = await fetch(`/api/seller/farm/${id}`);
	if (!response.ok) throw new Error('Farm not found');
	return response.json();
}

//gets the list of farms this buyer has bookmarked
export async function getSavedFarms(){
	const response = await fetch('/api/buyer/all', { credentials: 'include' });
	return response.json();
}

//bookmarks a farm for the logged in buyer
export async function saveFarm(farm){
	const response = await fetch("/api/buyer/all", {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(farm)
	});
	return response.json();
}

export async function getCowTypes(farmId) {
	const response = await fetch(`/api/seller/${farmId}/cow-types`, { credentials: 'include' });
	return response.json();
}

export async function removeSavedFarm(farm){
	const response = await fetch("/api/buyer/saved-farms", {
		method: 'DELETE',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ sellerId: farm.id })
	});
	if (!response.ok) throw new Error(`Failed to remove farm: ${response.status}`);
	return response.json();
}
