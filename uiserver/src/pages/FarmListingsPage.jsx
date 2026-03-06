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
			<Link to={`/farmlistings/${farm.id}`}>
				<button style={styles.button}>{farm.shopName}</button>
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
					<div style={styles.avatar}>{initials}</div>
					<span style={styles.profileLabel}>Profile</span>
				</button>

				<span style={styles.brand}>PRYM</span>

				{/* spacer so brand stays centred */}
				<div style={{ width: 90 }} />
			</nav>

			<h1 style={styles.header}>Farm Listings</h1>

			<div style={styles.containerMain}>
				{/* where all the farm listings are shown */}
				<div style={styles.listingContainer}>
					<ul>{listItems}</ul>
				</div>
				<div style={styles.containerSide}>
					{/* recently viewed farms and a button to navigate to saved farms */}
					<p style={styles.recentlyViewedContainer}>Coming Soon...</p>
					<button style={styles.savedButton}>Coming Soon...</button>
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
	button: {
		backgroundColor: 'white',
		color: '#4a7c59',
		fontSize: 50,
		fontFamily: 'Roboto',
		width: 800,
		height: 200,
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
