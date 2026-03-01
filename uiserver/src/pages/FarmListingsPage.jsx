import {farms} from '../assets/data.js';
import {Link} from 'react-router-dom';

function FarmListingsPage(){
	const listItems = farms.map(farm => 
		<li key={farm.id}>
				<Link to={`/farmlistingspage/${farm.name}`}><button style={styles.button}>{farm.name}</button></Link>
		</li>
	);
	
	return (
		<div>
			<h1 style={styles.header}>Farm Listings</h1>
			<div style={styles.containerMain}>
				<div style={styles.listingContainer}>
					<ul>{listItems}</ul>
				</div>
				<div style={styles.containerSide}>
					<p style={styles.recentlyViewedContainer}>Coming Soon...</p>
					<button style={styles.savedButton}>Coming Soon...</button>
				</div>
			</div>
		</div>
	);
}

const styles = {
	containerMain: {
		display: 'flex',
		flexDirection: 'row',
		justifyContent: 'center',
		width: '100%',
		height: '100%',
	},
	containerSide: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
		width: '50%',
		margin: 30,
	},
	listingContainer: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
		backgroundColor: '#f5f5f0',
		width: '50%',
		border: 'solid',
		margin: 30,
	},
	recentlyViewedContainer: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
		backgroundColor: '#f5f5f0',
		width: '90%',
		height: 900,
		border: 'solid',
		borderColor: 'black',
	},
	savedButton: {
		width: '90%',
		height: 100,
		border: 'solid',
		borderColor: 'black',
		margin: 30,
	},
	listing: {
		display: 'flex',
		flexDirection: 'row',
		backgroundColor: 'green',
		flex: 1,
	},
	button: {
		backgroundColor: 'white',
		color: '#4a7c59',
		fontSize: 50,
		fontFamily: 'Roboto',
		width: 800,
		height: 200,
		//border: 'none',
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