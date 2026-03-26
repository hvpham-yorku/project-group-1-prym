import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import farmImage from '../assets/rural-farm-landscape-stockcake.webp';
import { getFarm, saveFarm, getSavedFarms } from '../api/farm';

function FarmListing(){
	
	const { user } = useAuth();
	const navigate = useNavigate();

	const initials =
		(user?.firstName?.charAt(0) || '') + (user?.lastName?.charAt(0) || '');

	const profilePath = user?.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard';
	
	let {farmname} = useParams();
	console.log(farmname);
	
	const [farm, setFarm] = useState(null);
	const [savedIds, setSavedIds] = useState(new Set());
	
	useEffect(() => {
		getFarm(farmname).then(setFarm).catch(console.error);
		getSavedFarms().then((saved) => setSavedIds(new Set(saved.map((f) => f)))).catch(console.error);
	}, [farmname]);
	
	async function handleSave(e, farm){
		e.preventDefault(); // stop parent link from navigating
		await saveFarm(farm);
		setSavedIds((prev) => new Set([...prev, farm]));
	}
	
	if (!farm) return <div>Loading...</div>;
	
	const certs = (farm.certifications || []).map(c => <li key={c.id} style={styles.cert}>{c.name}</li>);
	
	return(
		<div style={styles.page}>
			{/* top navbar */}
			<nav style={styles.navbar}>
					<button
						style={styles.profileBtn}
						onClick={() => navigate(profilePath)}
						title="My Profile"
					>
					<div style={styles.avatar}>
						{user?.profilePicture ? (
							<img src={user.profilePicture} alt="Profile" style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
								) : (
								initials
								)}
							</div>
							<span style={styles.profileLabel}>Profile</span>
					</button>
					<span style={styles.brand}>PRYM</span>
					{/* spacer so brand stays centred */}
					<div style={{ width: 90 }} />
			</nav>
			
			<div style={{width: '100%', height: '300'}} alt="farm image"/>

			<h1 style={styles.header}>{farm.shopName}</h1>

			<div style={styles.container}>
				<p style={styles.descBox}>{farm.description}</p>
				<ul style = {styles.certBox}><p>Our Farm Is:</p>{certs}</ul>
			</div>

			<div style={styles.container}>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
				<div style={styles.cowBox}> COW </div>
			</div>
			
			<div style={styles.bottomButtonContainer}>
				<button style={{...styles.button, backgroundColor: savedIds.has(farm) ? '#a5c8a5' : '#4a7c59'}} onClick={(e) => handleSave(e, farm)} disabled={savedIds.has(farm)}>
					{savedIds.has(farm) ? 'Saved' : 'Save Farm'}
				</button>
				<button style={styles.button}>Rate Farm</button>
				<Link to={`/buyer/farmlistings`}><button style={{...styles.button, width: 200}}>Return To Farm Listings</button></Link>
			</div>
		</div>
	);
}

const styles = {
	page: {
			minHeight: '100vh',
			backgroundColor: '#faf8f4',
		},
		navbar: {
			display: 'flex',
			alignItems: 'center',
			justifyContent: 'space-between',
			backgroundColor: '#4a7c59',
			padding: '10px 20px',
		},
		profileBtn: {
			display: 'flex',
			alignItems: 'center',
			gap: '10px',
			background: 'none',
			border: 'none',
			cursor: 'pointer',
			padding: '4px 8px',
			borderRadius: '8px',
		},
		avatar: {
			width: '38px',
			height: '38px',
			borderRadius: '50%',
			backgroundColor: 'rgba(255,255,255,0.25)',
			border: '2px solid rgba(255,255,255,0.6)',
			color: 'white',
			display: 'flex',
			alignItems: 'center',
			justifyContent: 'center',
			fontSize: '15px',
			fontWeight: '700',
			flexShrink: 0,
		},
		profileLabel: {
			color: 'white',
			fontSize: '14px',
			fontWeight: '600',
			fontFamily: 'Roboto, sans-serif',
		},
		brand: {
			color: 'white',
			fontSize: '22px',
			fontWeight: '800',
			letterSpacing: '3px',
			fontFamily: 'Roboto, sans-serif',
			position: 'absolute',
			left: '50%',
			transform: 'translateX(-50%)',
		},
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
				display: 'flex',
				flex: 'column',
				margin: 5,
				fontSize: 50,
	},
	cert: {
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				height: 25,
				backgroundColor: 'green',
				borderRadius: 5,
				fontSize: 20,
				margin: 5,
				padding: '4px 8px',
	},
	
};

export default FarmListing;
