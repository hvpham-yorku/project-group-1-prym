import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAllFarms } from '../api/farm';


function FarmListingsPage() {
	const { user } = useAuth();
	const navigate = useNavigate();

	const initials =
		(user?.firstName?.charAt(0) || '') + (user?.lastName?.charAt(0) || '');

	const profilePath = user?.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard';
	
	const [farms, setFarms] = useState([]);
	
	useEffect(() => {
		getAllFarms().then(setFarms).catch(console.error);
	}, []);
	
	const listItems = farms.map(farm =>
		<li key={farm.id}>
			<Link to={`/buyer/farmlistings/${farm.id}`}>
				<button style={{...styles.button, borderLeft: '10px solid #2e7d32'}}>
					<div style={styles.colContainer}>
						{/* IMAGE GOES HERE */}
						<p>{farm.shopName}</p>
						{/* RATING GOES HERE */}
					</div>
					<div style={styles.colContainer}>
						<p>{farm.description}</p>
						<p>{farm.shopAddress}</p>
						{/* CERTIFICATIONS GO HERE */}
					</div>
				</button>
			</Link>
		</li>
	);

	return (
		<div style={styles.page}>

			{/* ── Top navbar ── */}
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

			<h1 style={styles.header}>Farm Listings</h1>

			<div style={styles.containerMain}>
				{/* where all the farm listings are shown */}
				<div style={{...styles.containerSide, width: '60%'}}>
					<ul>{listItems}</ul>
				</div>
				<div style={{...styles.containerSide, width: '40%'}}>
					{/* recently viewed farms and a button to navigate to saved farms */}
					<Link to={`/buyer/saved_farms`}><button style={styles.savedButton}>Go To My Saved Farms</button></Link>
					{/*<p style={styles.recentlyViewedContainer}>Coming Soon...</p>*/}
				</div>
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
		width: 600,
		height: 100,
		border: 'none',
		borderRadius: 10,
		margin: 30,
		backgroundColor: '#4a7c59',
		color: 'white',
		fontSize: 50,
		fontFamily: 'Roboto',
	},
	button: {
		display: 'flex',
		flexDirection: 'row',
		backgroundColor: 'white',
		fontSize: 50,
		fontFamily: 'Roboto',
		width: 800,
		height: 300,
		border: '1px solid',
		borderRadius: 10,
		margin: 5,
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
};

export default FarmListingsPage;
