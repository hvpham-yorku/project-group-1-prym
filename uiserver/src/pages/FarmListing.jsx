import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getFarm, saveFarm, getSavedFarms, removeSavedFarm, getCowTypes } from '../api/farm';

function FarmListing(){
	
	const { user } = useAuth();
	const navigate = useNavigate();

	const initials =
		(user?.firstName?.charAt(0) || '') + (user?.lastName?.charAt(0) || '');

	const profilePath = user?.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard';
	
	let {farmname} = useParams();
	
	const [farm, setFarm] = useState(null);
	const [savedIds, setSavedIds] = useState(new Set());
	const [cowTypes, setCowTypes] = useState([]);

	useEffect(() => {
		getFarm(farmname).then((f) => {
			setFarm(f);
			if (f) getCowTypes(f.id).then(setCowTypes).catch(console.error);
		}).catch(console.error);
		getSavedFarms().then((saved) => setSavedIds(new Set(saved.map((f) => f.id)))).catch(console.error);
	}, [farmname]);
	
	async function handleSave(e, farm){
		e.preventDefault();
		await saveFarm(farm);
		setSavedIds((prev) => new Set([...prev, farm.id]));
	}

	async function handleUnsave(e, farm){
		e.preventDefault();
		await removeSavedFarm(farm);
		setSavedIds((prev) => { const next = new Set(prev); next.delete(farm.id); return next; });
	}
	
	if (!farm) return <div>Loading...</div>;
	
	const certs = (farm.certifications || []).map(c => <li key={c.id} style={styles.cert}>{c.name}</li>);
	
	return(
		<div style={styles.page}>
			{/* top navbar */}
			<nav style={styles.navbar}>
					<button
						style={styles.backBtn}
						onClick={() => navigate(-1)}
					>
						← Back
					</button>
					<span style={styles.brand}>PRYM</span>
					{/* spacer so brand stays centred */}
					<div style={{ width: 90 }} />
			</nav>
			
			<div style={{width: '100%', height: '300'}} />

			<h1 style={styles.header}>{farm.shopName}</h1>
			<div style={styles.ratingRow}>
				<span style={styles.stars}>{'★'.repeat(Math.round(farm.averageRating))}{'☆'.repeat(5 - Math.round(farm.averageRating))}</span>
				<span style={styles.ratingText}>{farm.averageRating.toFixed(1)} ({farm.totalRatings} ratings)</span>
			</div>

			<div style={styles.container}>
				<p style={styles.descBox}>{farm.description}</p>
				<ul style = {styles.certBox}><p>Our Farm Is:</p>{certs}</ul>
			</div>

			<h2 style={styles.sectionTitle}>Available Cattle</h2>
			<div style={styles.container}>
				{cowTypes.length === 0 ? (
					<p style={{ margin: 20, color: '#888' }}>No cattle listed yet.</p>
				) : (
					cowTypes.map((ct) => (
						<div key={ct.id} style={styles.cowCard}>
							<div style={styles.cowBreed}>{ct.breed.replace('_', ' ')}</div>
							<p style={styles.cowDesc}>{ct.description}</p>
							<div style={styles.cowMeta}>
								<span style={styles.cowPrice}>${ct.pricePerPound.toFixed(2)}/lb</span>
								<span style={styles.cowAvail}>{ct.availableCount} available</span>
							</div>
						</div>
					))
				)}
			</div>
			
			<div style={styles.bottomButtonContainer}>
				{savedIds.has(farm.id) ? (
					<button style={{...styles.button, backgroundColor: '#c0392b', width: 160}} onClick={(e) => handleUnsave(e, farm)}>
						Remove from Saved
					</button>
				) : (
					<button style={styles.button} onClick={(e) => handleSave(e, farm)}>
						Save Farm
					</button>
				)}
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
		backBtn: {
			background: 'none',
			border: '2px solid rgba(255,255,255,0.6)',
			borderRadius: '6px',
			color: 'white',
			fontSize: '14px',
			fontWeight: '600',
			padding: '6px 14px',
			cursor: 'pointer',
			flexShrink: 0,
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
	ratingRow: {
		display: 'flex',
		alignItems: 'center',
		gap: 10,
		padding: '4px 20px 12px 20px',
	},
	stars: {
		color: '#f5a623',
		fontSize: 28,
	},
	ratingText: {
		color: '#555',
		fontSize: 18,
		fontFamily: 'Roboto',
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
	sectionTitle: {
		fontFamily: 'Roboto',
		fontSize: 28,
		color: '#4a7c59',
		margin: '20px 20px 0 20px',
	},
	cowCard: {
		margin: 20,
		padding: 20,
		backgroundColor: '#f5f5f0',
		border: '1px solid #ccc',
		borderRadius: 10,
		width: 260,
		display: 'flex',
		flexDirection: 'column',
		gap: 10,
	},
	cowBreed: {
		fontFamily: 'Roboto',
		fontSize: 22,
		fontWeight: '700',
		color: '#4a7c59',
		textTransform: 'capitalize',
	},
	cowDesc: {
		fontFamily: 'Roboto',
		fontSize: 14,
		color: '#555',
		margin: 0,
		flexGrow: 1,
	},
	cowMeta: {
		display: 'flex',
		justifyContent: 'space-between',
		alignItems: 'center',
	},
	cowPrice: {
		fontFamily: 'Roboto',
		fontSize: 16,
		fontWeight: '700',
		color: '#2e7d32',
	},
	cowAvail: {
		fontFamily: 'Roboto',
		fontSize: 13,
		color: '#888',
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
