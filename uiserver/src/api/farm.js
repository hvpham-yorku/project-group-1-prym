import {farms} from '../assets/data.js';

export function getFarm(name){
	return farms.find((f) => f.name === name);
}