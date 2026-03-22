import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Link } from 'react-router-dom';
import farmImage from '../assets/rural-farm-landscape-stockcake.webp';
import { getFarm, saveFarm } from '../api/farm';

function FarmListing(){
	
	let {farmname} = useParams();
	console.log(farmname);
	
	const [farm, setFarm] = useState(null);
	
	useEffect(() => {
		getFarm(farmname).then(setFarm).catch(console.error);
	}, [farmname]);
	
	if (!farm) return <div>Loading...</div>;
	
	const certs = (farm.certifications || []).map(c => <li key={c.id}>{c.name}</li>);
	
	return(
		<div>
			<img src={farmImage} width='100%' height='300' alt="farm image"/>

			<h1 style={styles.header}>{farm.shopName}</h1>

			<div style={styles.container}>
				<p style={styles.descBox}>{farm.description}</p>
				<ul style = {styles.certBox}>{certs}</ul>
			</div>

			<div style={styles.container}>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
			</div>
			
			<div style={styles.bottomButtonContainer}>
				<button style={styles.button} OnClick={saveFarm(farm)}>Save Farm</button>
				<button style={styles.button}>Rate Farm</button>
				<Link to={`/buyer/farmlistings`}><button style={{...styles.button, width: 200}}>Return To Farm Listings</button></Link>
			</div>
		</div>
	);
}

const styles = {
	header: {
		backgroundColor: '#4a7c59',
		fontFamily: 'Roboto',
		display: 'flex',
		alignItems: 'center',
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
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: '#f5f5f0',
		border: 'solid',
		borderColor: '#333',
		borderRadius: 5,
		width: '40%',
		height: 100,
	},
	certification: {
		margin: 5,
		display: 'flex',
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'center',
		backgroundColor: '#f5f5f0',
		border: 'solid',
		borderColor: '#333',
		borderRadius: '50%',
		width: '20%',
		height: '80%',
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
	bottomButtonContainer: {
		display: 'flex',
		flexDirection:'row',
		justifyContent: 'right',
		border: 'none',
		margin: 5,
	},
	button: {
		display: 'flex',
		margin: 10,
		padding: "12px 10px",
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: "#4a7c59",
		color: "white",
		border: "none",
		borderRadius: "6px",
		fontSize: "15px",
		fontWeight: "600",
		width: 100,
	},
	farmImage: {
			height: '60%',
			border: 'solid',
			margin: 5,
			color: 'grey',
		},
		farmName: {
			backgroundColor: '#4a7c59',
			borderRadius: 10,
			color: 'white',
			padding: "5px 5px",
			margin: 5,
			fontSize: 30,
		},
		rating: {
			color: 'yellow',
			margin: 5,
			fontSize: 20,
		},
		description: {
			height: '50%',
			color: 'grey',
			border: 'solid',
			margin: 5,
			alignItens: 'right',
			justifyContent: 'right',
			fontSize: 20,
		},
		location: {
			width: '100%',
			color: '#4a7c59',
			fontSize: 30,
		},
		certifications: {
			margin: 5,
			fontSize: 50,
		},
	
};

export default FarmListing;
