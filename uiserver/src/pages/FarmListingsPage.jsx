import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAllFarms } from '../api/farm';


//Browse all available farms page. Shows every farm in the system as a list
//of clickable cards. Buyers use this to find farms they want to buy from.
function FarmListingsPage() {
	const { user } = useAuth();
	const navigate = useNavigate();

	//build initials for the avatar circle in the navbar
	const initials =
		(user?.firstName?.charAt(0) || '') + (user?.lastName?.charAt(0) || '');

	const profilePath = user?.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard';

	const [farms, setFarms] = useState([]);
	const [searchQuery, setSearchQuery] = useState('');
	const [showFilters, setShowFilters] = useState(false);
	const [selectedCategory, setSelectedCategory] = useState('');
	const [minRating, setMinRating] = useState(0);
	const [sortBy, setSortBy] = useState('');

	useEffect(() => {
		getAllFarms().then(setFarms).catch(console.error);
	}, []);

	let filteredFarms = farms.filter(farm =>
		(farm.shopName || '').toLowerCase().includes(searchQuery.toLowerCase())
	)

	//category filter
	if(selectedCategory){
		filteredFarms = filteredFarms.filter(farm=>
			(farm.certifications || []).some(c => c.name === selectedCategory)
		);
	}

	//minimum rating filter
	if(minRating > 0){
		filteredFarms = filteredFarms.filter(farm =>
			farm.averageRating >= minRating
		);
	}

	//sorting filter
	if(sortBy === 'name-asc'){
		filteredFarms = [...filteredFarms].sort((a,b) => (a.shopName || '').localeCompare(b.shopName || ''));
	} else if (sortBy === 'name-desc'){
		filteredFarms = [...filteredFarms].sort((a,b) => (b.shopName || '').localeCompare(a.shopName || ''));
	} else if (sortBy === 'rating-high'){
		filteredFarms = [...filteredFarms].sort((a, b) => b.averageRating - a.averageRating);
	} else if (sortBy === 'rating-low'){
		filteredFarms = [...filteredFarms].sort((a, b) => a.averageRating - b.averageRating);
	}

	const listItems = filteredFarms.map(farm => {
		let certs = (farm.certifications || []).map(c =>
			 <li key={c.id}>
			 {c.name === "KOSHER" && (
			 	<span style={{ ...styles.badge, ...styles.badgeKosher }}>Kosher</span>
			 )}
			 {c.name === "HALAL" && (
			 	<span style={{ ...styles.badge, ...styles.badgeHalal }}>Halal</span>
			 )}
			 {c.name === "ORGANIC" && (
			 	<span style={{ ...styles.badge, ...styles.badgeOrganic }}>Organic</span>
			 )}
			 {c.name === "GRASS_FED" && (
			 	<span style={{ ...styles.badge, ...styles.badgeGrassFed }}>Grass-Fed</span>
			 )}
			 {c.name === "NON_GMO" && (
			 	<span style={{ ...styles.badge, ...styles.badgeNonGmo }}>Non-GMO</span>
			 )}
			 </li>
		 );
		return (<li key={farm.id}>
			<Link to={`/buyer/farmlistings/${farm.id}`}>
				<button style={{...styles.button, borderLeft: '10px solid #2e7d32'}}>
					<div style={{...styles.colContainer, width: "40%"}}>
						<div style={styles.farmImage}>{user?.profilePicture ? (
							<img src={farm.getUser().getProfilePicture()} alt="farm_photo" style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />) : ( 'no image found' )}
						</div>
						<div style={styles.farmName}>{farm.shopName}</div>
						<div style ={styles.certBadges}>{certs}</div>
					</div>
					<div style={{...styles.colContainer, width: "60%"}}>
						<div style={styles.description}>{farm.description}</div>
						<div style={styles.location}>{farm.shopAddress}</div>
					</div>
				</button>
			</Link>
		</li> );
	});

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

			{/* Search bar */}
			<div style={styles.searchContainer}>
    			<input
        			style={styles.searchInput}
        			type="text"
        			placeholder="Search farms by name..."
        			value={searchQuery}
        			onChange={e=> setSearchQuery(e.target.value)}
    			/>
				<button
					style={styles.filterBtn}
					onClick={()=>setShowFilters(!showFilters)}
				>Filters
				</button>
			</div>

			{/*filter dropdown*/}
			{showFilters &&(
				<div style ={styles.filterPanel}>
					{/*category filter*/}
					<label style = {styles.filterLabel}>Category</label>
					<select style={styles.filterSelect} value = {selectedCategory} onChange = {e => setSelectedCategory(e.target.value)}>
						<option value="">ALL</option>
						<option value="HALAL">Halal</option>
						<option value="KOSHER">Kosher</option>
						<option value="ORGANIC">Organic</option>
						<option value="CONVENTIONAL">Conventional</option>
					</select>

					{/* Rating filter */}
        			<label style={styles.filterLabel}>Min Rating</label>
        			<select style={styles.filterSelect} value={minRating} onChange={e => setMinRating(Number(e.target.value))}>
            			<option value={0}>Any</option>
            			<option value={1}>1+</option>
            			<option value={2}>2+</option>
            			<option value={3}>3+</option>
            			<option value={4}>4+</option>
            			<option value={5}>5</option>
        			</select>

					{/*Sort filter*/}
					<label style={styles.filterLabel}>Sort By</label>
        			<select style={styles.filterSelect} value={sortBy} onChange={e => setSortBy(e.target.value)}>
            			<option value="">Default</option>
            			<option value="name-asc">Name A-Z</option>
            			<option value="name-desc">Name Z-A</option>
            			<option value="rating-high">Rating High-Low</option>
            			<option value="rating-low">Rating Low-High</option>
        			</select>

					{/*Clear button*/}
					<button style={styles.clearBtn} onClick={() => { setSelectedCategory(''); setMinRating(0); setSortBy(''); }}>
            			Clear Filters
        			</button>
				</div>
			)}

			<div style={styles.containerMain}>
				{/* where all the farm listings are shown */}
				<div style={{...styles.containerSide, width: '80%'}}>
					{filteredFarms.length === 0
						? <p style={styles.emptyState}>No farms match your current filters.</p>
						: <ul>{listItems}</ul>
					}
				</div>
				<div style={{...styles.containerSide, width: '20%'}}>
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
		width: 300,
		height: 100,
		border: 'none',
		borderRadius: 10,
		margin: 30,
		backgroundColor: '#4a7c59',
		color: 'white',
		fontSize: 40,
		fontFamily: 'Roboto, sans-serif',
	},
	button: {
		display: 'flex',
		flexDirection: 'row',
		backgroundColor: 'white',
		fontSize: 50,
		fontFamily: 'Roboto, sans-serif',
		width: 1100,
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
		fontFamily: 'Roboto, sans-serif',
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
		borderRadius: 8,
	},
	farmName: {
		backgroundColor: '#4a7c59',
		borderRadius: 10,
		color: 'white',
		padding: "5px 5px",
		margin: 5,
		fontSize: 30,
		fontFamily: 'Roboto, sans-serif',
	},
	rating: {
		color: '#f5a623',
		margin: 5,
		fontSize: 20,
		fontFamily: 'Roboto, sans-serif',
	},
	description: {
		height: '40%',
		fontSize: 20,
		marginTop: 40,
		fontFamily: 'Roboto, sans-serif',
	},
	location: {
		color: '#4a7c59',
		fontSize: 30,
		fontFamily: 'Roboto, sans-serif',
	},
	certifications: {
		display: 'flex',
		flexDirection: 'column',
		flexWrap: "wrap",
		margin: 5,
		fontSize: 50,
		fontFamily: 'Roboto, sans-serif',
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
		fontFamily: 'Roboto, sans-serif',
	},
	badge: {
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		height: 25,
		backgroundColor: 'green',
		fontSize: 20,
		margin: 5,
		padding: '4px 8px',
		borderRadius: 99,
		textTransform: "uppercase",
		fontFamily: 'Roboto, sans-serif',
	},
	certBadges: {
	    display: "flex",
	    gap: "6px",
	    flexWrap: "wrap",
	 },
	 badgeKosher:   { backgroundColor: "#e3f2fd", color: "#1565c0" },
	 badgeHalal:    { backgroundColor: "#fff3e0", color: "#e65100" },
	 badgeOrganic:  { backgroundColor: "#e8f5e9", color: "#2e7d32" },
	 badgeGrassFed: { backgroundColor: "#f1f8e9", color: "#558b2f" },
	 badgeNonGmo:   { backgroundColor: "#fce4ec", color: "#880e4f" },
	searchContainer: {
    	display: 'flex',
    	justifyContent: 'center',
    	margin: '0 0 20px 0',
	},
	searchInput: {
    	width: '60%',
    	padding: '12px 20px',
    	fontSize: '18px',
    	border: '2px solid #4a7c59',
    	borderRadius: '30px',
    	outline: 'none',
    	fontFamily: 'Roboto',
	},
	filterBtn: {
    padding: '12px 20px',
    marginLeft: '10px',
    backgroundColor: '#4a7c59',
    color: 'white',
    border: 'none',
    borderRadius: '30px',
    fontSize: '16px',
    fontWeight: '600',
    cursor: 'pointer',
    fontFamily: 'Roboto',
},
	filterPanel: {
    	display: 'flex',
    	flexDirection: 'row',
   		alignItems: 'center',
    	justifyContent: 'center',
    	gap: '16px',
    	backgroundColor: 'white',
    	padding: '16px 24px',
    	margin: '0 auto 20px auto',
    	width: '70%',
    	borderRadius: '12px',
    	border: '1px solid #ddd',
    	flexWrap: 'wrap',
	},
	filterLabel: {
    	fontSize: '14px',
    	fontWeight: '600',
    	color: '#4a7c59',
    	fontFamily: 'Roboto',
	},
	filterSelect: {
    	padding: '8px 12px',
    	border: '1px solid #4a7c59',
    	borderRadius: '8px',
    	fontSize: '14px',
    	fontFamily: 'Roboto',
    	outline: 'none',
    	cursor: 'pointer',
	},
	emptyState: {
    	color: '#666',
    	fontSize: '20px',
    	fontFamily: 'Roboto',
    	textAlign: 'center',
    	marginTop: 60,
	},
	clearBtn: {
    	padding: '8px 16px',
    	backgroundColor: 'white',
    	color: '#c0392b',
    	border: '2px solid #c0392b',
    	borderRadius: '8px',
    	fontSize: '14px',
    	fontWeight: '600',
    	cursor: 'pointer',
    	fontFamily: 'Roboto',
	},
};

export default FarmListingsPage;
