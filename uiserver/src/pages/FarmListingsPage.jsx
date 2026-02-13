import {farms} from '../assets/data.js';

function FarmListingsPage(){
	const listItems = farms.map(farm => 
		<li key={farm.id}>
			<div style={styles.listing}>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>{farm.name}</button></a>
			</div>
		</li>
	);
	
	return (
		<div>
			<h1 style={styles.header}>Farm Listings</h1>
			<div style={styles.container}>
				<ul>{listItems}</ul>
			</div>
		</div>
	);
}

const styles = {
	container: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
		backgroundColor: '#f5f5f0',
	},
	listing: {
		display: 'flex',
		flexDirection: 'row',
		backgroundColor: 'green',
		margin: 10,
		borderRadius: 8,
	},
	button: {
		backgroundColor: 'white',
		color: '#4a7c59',
		fontSize: 50,
		fontFamily: 'Roboto',
		border: 'solid',
		borderColor: '#333',
		borderRadius: 8,
		width: 1500,
		height: 300,
		cursor: 'pointer',	
	},
	header: {
		fontFamily: 'Roboto',
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		color: '#4a7c59',
		fontSize: 100,
	},
};

export default FarmListingsPage;