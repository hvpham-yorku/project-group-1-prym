import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/auth';
import { useState, useEffect } from 'react';
import { getSellerProfile, updateSellerProfile } from '../api/seller';

function SellerDashboard() {
    const { user, clearUser, saveUser } = useAuth();
    const navigate = useNavigate();

    const [isEditing, setIsEditing] = useState(false);
    const [formData, setFormData] = useState({
        firstName: user?.firstName || '',
        lastName: user?.lastName || '',
        username: user?.username || '',
        email: user?.email || '',
        phoneNumber: user?.phoneNumber || '',
        shopName: '',
        shopAddress: ''
    });
    const [activeSection, setActiveSection] = useState("Dashboard");
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                setLoading(true);
                const data = await getSellerProfile(user.id);
                setProfile(data);
                setFormData({
                    firstName: data.firstName || '',
                    lastName: data.lastName || '',
                    username: data.username || '',
                    email: data.email || '',
                    phoneNumber: data.phoneNumber || '',
                    shopName: data.shopName || '',
                    shopAddress: data.shopAddress || ''
                });
            } catch (err) {
                setError("Failed to load profile.");
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
    }, [user.id]);

    const handleLogout = async () => {
        try {
            await logout();
        } catch (error) {
            console.error('Logout failed:', error);
        } finally {
            clearUser();
            navigate('/login');
        }
    };
	
	const handleSave = async () => {
	    try {
	        // Only send fields that backend allows updating
	        const payload = {
    shopName: formData.shopName,
    shopAddress: formData.shopAddress
};

	        const updated = await updateSellerProfile(user.id, payload);
	        setProfile(updated);
	        
	        setIsEditing(false);
	    } catch (err) {
	        console.error("Save error:", err);
	        alert("Failed to save profile. Check the console for errors.");
	    }
	};

    if (loading) {
        return <div style={{ textAlign: 'center', padding: '50px' }}>Loading...</div>;
    }

    return (
        <div>
            <Nav setActiveSection={setActiveSection} handleLogout={handleLogout} />

            <div style={styles.container}>
                <div style={styles.card}>
                    {activeSection === "Dashboard" && (
                        <>
                            <h1 style={styles.title}>Seller Dashboard</h1>
                            <p style={styles.welcome}>Welcome, {user?.username}!</p>
                            <p style={styles.info}>You are logged in as a <strong>SELLER</strong></p>
                        </>
                    )}

                    {activeSection === "Profile" && (
                        <>
                            <div style={styles.profilePictureContainer}>
                                {user?.profilePicture ? (
                                    <img
                                        src={user.profilePicture}
                                        alt="Profile"
                                        style={styles.profilePicture}
                                    />
                                ) : (
                                    <div style={styles.profilePicturePlaceholder}>
                                        {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                                    </div>
                                )}
                            </div>

                            <h1 style={styles.title}>Your Profile</h1>
                            <p style={styles.welcome}>Welcome, {isEditing ? formData.firstName : user?.username}!</p>
                            <p style={styles.name}>
                                {isEditing ? `${formData.firstName} ${formData.lastName}` : `${user?.firstName} ${user?.lastName}`}
                            </p>
                            <p style={styles.info}>You are logged in as a <strong>SELLER</strong></p>

                            <div style={styles.infoCard}>
    {Object.entries({
        "Shop Name": formData.shopName,
        "Shop Address": formData.shopAddress
    }).map(([label, value]) => (
        <div key={label} style={styles.infoRow}>
            <span style={styles.infoLabel}>{label}:</span>
            {isEditing ? (
                <input
                    type="text"
                    value={value}
                    onChange={(e) => setFormData({ ...formData, [camelCase(label)]: e.target.value })}
                />
            ) : (
                <span style={styles.infoValue}>{value}</span>
            )}
        </div>
    ))}
</div>

                            <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '20px' }}>
                                {isEditing ? (
                                    <>
                                        <button style={styles.button} onClick={handleSave}>Save</button>
                                        <button
                                            style={styles.button}
                                            onClick={() => {
                                                setFormData({
                                                    firstName: profile?.firstName || '',
                                                    lastName: profile?.lastName || '',
                                                    username: profile?.username || '',
                                                    email: profile?.email || '',
                                                    phoneNumber: profile?.phoneNumber || '',
                                                    shopName: profile?.shopName || '',
                                                    shopAddress: profile?.shopAddress || ''
                                                });
                                                setIsEditing(false);
                                            }}
                                        >
                                            Discard
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        <button style={styles.button} onClick={() => setIsEditing(true)}>Edit Profile</button>
                                        <button style={styles.button} onClick={handleLogout}>Logout</button>
                                    </>
                                )}
                            </div>
                        </>
                    )}

                    {activeSection === "Notifications" && (
                        <div>
                            <h1>Notifications</h1>
                            <p>No notifications yet.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

// Utility to convert labels to camelCase for formData keys
function camelCase(label) {
    return label
        .replace(/\s(.)/g, function(match, group1) { return group1.toUpperCase(); })
        .replace(/\s/g, '')
        .replace(/^(.)/, function(match, group1) { return group1.toLowerCase(); });
}

function Nav({ setActiveSection, handleLogout }) {
    const [hamburgerOpen, setHamburgerOpen] = useState(false);
    const toggleHamburger = () => setHamburgerOpen(!hamburgerOpen);

    return (
        <div>
            <div style={{
                position: 'absolute',
                top: 20,
                left: 20,
                fontSize: '24px',
                cursor: 'pointer',
                zIndex: 1000
            }}
                onClick={toggleHamburger}
            >
                ☰
            </div>

            {hamburgerOpen && (
                <div style={{
                    width: '200px',
                    height: '100vh',
                    backgroundColor: '#ddd',
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    padding: '20px',
                    paddingTop: '70px',
                    zIndex: 999
                }}>
                    <ul style={{ listStyle: 'none', padding: 0 }}>
                        {["Dashboard", "Profile", "Notifications"].map(section => (
                            <li key={section}>
                                <button
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                                    onClick={() => { setActiveSection(section); setHamburgerOpen(false); }}
                                >
                                    {section}
                                </button>
                            </li>
                        ))}
                        <li>
                            <button
                                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                                onClick={() => { handleLogout(); setHamburgerOpen(false); }}
                            >
                                Logout
                            </button>
                        </li>
                    </ul>
                </div>
            )}
        </div>
    );
}

const styles = {
    container: { 
		minHeight: '100vh', 
		display: 'flex', 
		alignItems: 'center', 
		justifyContent: 'center', 
		backgroundColor: '#f5f5f0', 
		padding: '20px' 
	},
    card: { 
		backgroundColor: 'white', 
		padding: '40px', 
		borderRadius: '8px', 
		boxShadow: '0 4px 6px rgba(0,0,0,0.1)', 
		textAlign: 'center', 
		border: '2px solid #5c4033', 
		minWidth: '350px' 
	},
    profilePictureContainer: { 
		display: 'flex', 
		justifyContent: 'center', 
		marginBottom: '20px' 
	},
    profilePicture: { 
		width: '120px', 
		height: '120px', 
		borderRadius: '50%', 
		objectFit: 'cover', 
		border: '3px solid #5c4033' 
	},
    profilePicturePlaceholder: { 
		width: '120px', 
		height: '120px', 
		borderRadius: '50%', 
		backgroundColor: '#5c4033', 
		color: 'white', 
		display: 'flex', 
		alignItems: 'center', 
		justifyContent: 'center', 
		fontSize: '36px', 
		fontWeight: 'bold' },
    title: { 
		color: '#5c4033', 
		marginBottom: '8px' 
	},
    welcome: { 
		fontSize: '22px', 
		color: '#333', 
		fontWeight: '600', 
		marginBottom: '4px' 
	},
    name: { 
		fontSize: '16px', 
		color: '#666', 
		marginBottom: '8px' 
	},
    info: { 
		color: '#666', 
		marginBottom: '20px'
	},
    infoCard: { 
		backgroundColor: '#f9f9f9', 
		borderRadius: '8px', 
		padding: '16px', 
		marginBottom: '24px', 
		textAlign: 'left' 
	},
    infoRow: { 
		display: 'flex', 
		justifyContent: 'space-between', 
		
		padding: '8px 0', 
		borderBottom: '1px solid #eee' 
	},
    infoLabel: { 
		color: '#666', 
		fontWeight: '500' 
	},
    infoValue: { 
		color: '#333' 
	},
    button: { 
		padding: '12px 24px', 
		backgroundColor: '#4a7c59', 
		color: 'white', 
		border: 'none', 
		borderRadius: '4px', 
		
		fontSize: '16px', 
		cursor: 'pointer' 
	}
};

export default SellerDashboard;
