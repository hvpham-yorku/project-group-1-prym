import {useParams} from 'react-router-dom';
import farmImage from '../assets/rural-farm-landscape-stockcake.webp';
import {getFarm} from '../api/farm.js';

function FarmListing(){
	
	let { farmid } = useParams();
	console.log(farmid);
	const farm = getFarm(farmid);
	console.log(farm);
	
	return(
		<div>
			<img src={farmImage} width='100%' height='300' alt="farm image"/>
			
			<h1 style={styles.header}>{farm.name}</h1>
			
			<div style={styles.container}>
				<p style={styles.descBox}>{farm.description}</p>
				<p style = {styles.certBox}> These are the certifications! </p>
			</div>
			
			<div style={styles.container}>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
			</div>
		</div>
	);
}

const styles = {
	header: {
		backgroundColor: 'lightGreen',
		fontFamily: 'Roboto',
		display: 'flex',
		alignItems: 'center',
		//color: '#4a7c59',
		color: 'black',
		fontSize: 50,
	},
	container: {
		display: 'flex',
		flexDirection: 'row',
	},
	descBox: {
		margin: 20,
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: '#f5f5f0',
		border: 'solid',
		borderColor: '#333',
		borderRadius: 5,
		width: '60%',
		height: 100,
	},
	certBox:{
		margin: 20,
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: '#f5f5f0',
		border: 'solid',
		borderColor: '#333',
		borderRadius: 5,
		width: '40%',
		height: 100,
	},
	cowBox:	{
		margin: 20,
		display: 'flex',
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: '#f5f5f0',
		border: 'solid',
		borderColor: '#333',
		borderRadius: 5,
		width: 400,
		height: 600,
	},
};

export default FarmListing;