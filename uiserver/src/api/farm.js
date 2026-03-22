import {useState, useEffect} from 'react';

export async function getAllFarms() {
	const response = await fetch('/api/seller/all');
	return response.json();
}

export async function getFarm(id) {
	const farms = await getAllFarms();
	return farms.find((f) => f.id === Number(id));
}

export async function getSavedFarms(){
	const response = await fetch('/api/buyer/');
	return response.json();
}