import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
function savedFarms(){
	
	const { user } = useAuth();
	const navigate = useNavigate();

	const initials =
		(user?.firstName?.charAt(0) || '') + (user?.lastName?.charAt(0) || '');
	const profilePath = user?.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard';
		
	return(
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
		
		<h1> Saved Farms Page Coming Soon! </h1>
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
	}
};

export default savedFarms;