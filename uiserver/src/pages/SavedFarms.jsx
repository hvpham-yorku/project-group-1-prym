import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getSavedFarms, removeSavedFarm } from '../api/farm';
import { useState, useEffect } from 'react';
import CertBadge from '../components/CertBadge';

//Shows the list of farms a buyer has bookmarked/saved.
//Basically the same layout as FarmListingsPage but only shows the saved ones.
function savedFarms(){

	const { user } = useAuth();
	const navigate = useNavigate();

	const initials =
		(user?.firstName?.charAt(0) || '') + (user?.lastName?.charAt(0) || '');
	const profilePath = user?.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard';

	const [farms, setFarms] = useState([]);

	//fetch only this buyer's saved farms on mount
	useEffect(() => {
		getSavedFarms().then(setFarms).catch(console.error);
	}, []);
	
	async function handleUnsave(farm){
		await removeSavedFarm(farm);
		setFarms((prev) => prev.filter((f) => f.id !== farm.id));
	}
	
	{/*making the list of farms to display */}
	const listItems = farms.map(farm => {
			let certs = (farm.certifications || []).map(c =>
				<li key={c.id}><CertBadge certName={c.name} /></li>
			);
			return (<li key={farm.id}>
				<Link to={`/buyer/farmlistings/${farm.id}`}>
					<button style={{...styles.button, borderLeft: '10px solid #2e7d32'}}>
						<div style={styles.colContainer}>
							<div style={styles.farmImage}>{user?.profilePicture ? (
								<img src={farm.getUser().getProfilePicture()} alt="farm_photo" style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />) : ( 'no image found' )}
							</div>
							<div style={styles.farmName}>{farm.shopName}</div>
							<div style={styles.certBadges}>{certs}</div>
						</div>
						<div style={{...styles.colContainer, width: "60%"}}>
							<div style={styles.description}>{farm.description}</div>
							<div style={styles.location}>{farm.shopAddress}</div>
							<div style={styles.rating}>{'★'.repeat(Math.round(farm.averageRating))}{'☆'.repeat(5 - Math.round(farm.averageRating))} {farm.averageRating.toFixed(1)} ({farm.totalRatings} ratings)</div>
						</div>
					</button>
				</Link>
				<button style={styles.removeButton} onClick={() => handleUnsave(farm)}>Remove</button>
			</li> );
		});
		
	return(
		<div style={styles.page}>
				{/* ── Top navbar ── */}
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
			
			<p style={styles.header}>Saved Farms</p>
			<div style={{display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
				<ul>{listItems}</ul>
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
	button: {
		display: 'flex',
		flexDirection: 'row',
		backgroundColor: 'white',
		fontSize: 50,
		fontFamily: 'Roboto',
		width: 1200,
		height: 300,
		border: '1px solid',
		borderRadius: 10,
		margin: 10,
	},
	removeButton: {
		display: 'flex',
		margin: 10,
		padding: "12px 10px",
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: "red",
		color: "white",
		border: "none",
		borderRadius: "6px",
		fontSize: "15px",
		fontWeight: "600",
		width: 100,
	},
	colContainer: {
		display: 'flex',
		flexDirection:'column',
		border: 'none',
		margin: 5,
	},
	header: {
		fontFamily: 'Roboto',	
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		color: '#4a7c59',
		fontSize: 100,
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
			height: '40%',
			marginTop: 40,
			fontSize: 20,
		},
	location: {
			width: '100%',
			color: '#4a7c59',
			fontSize: 30,
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
	certBadges: {
		    display: "flex",
		    gap: "6px",
		    flexWrap: "wrap",
	},
};

export default savedFarms;