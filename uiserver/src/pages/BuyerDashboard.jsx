import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/auth';

function BuyerDashboard() {
    const { user, clearUser } = useAuth();
    const navigate = useNavigate();

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

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                {/* Profile Picture */}
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

                <h1 style={styles.title}>Buyer Dashboard</h1>
                <p style={styles.welcome}>Welcome, {user?.username}!</p>
                <p style={styles.name}>{user?.firstName} {user?.lastName}</p>
                <p style={styles.info}>You are logged in as a <strong>BUYER</strong></p>
                
               
                {/* User Info Card */}
                <div style={styles.infoCard}>
                    <div style={styles.infoRow}>
                        <span style={styles.infoLabel}>Email:</span>
                        <span style={styles.infoValue}>{user?.email}</span>
                    </div>
                    <div style={styles.infoRow}>
                        <span style={styles.infoLabel}>Phone:</span>
                        <span style={styles.infoValue}>{user?.phoneNumber}</span>
                    </div>
                </div>
                <button onClick={() => navigate('/buyer/profile')} style={styles.profileButton}>
                    My Profile
                </button>

                <button onClick={handleLogout} style={styles.button}>
                    Logout
                </button>
            </div>
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
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        textAlign: 'center',
        border: '2px solid #4a7c59',
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
        border: '3px solid #4a7c59'
    },
    profilePicturePlaceholder: {
        width: '120px',
        height: '120px',
        borderRadius: '50%',
        backgroundColor: '#4a7c59',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '36px',
        fontWeight: 'bold'
    },
    title: {
        color: '#4a7c59',
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
    profileButton: {
        padding: '12px 24px',
        backgroundColor: '#4a7c59',
        color: 'white',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer',
        marginBottom: '12px'
    },
    button: {
        padding: '12px 24px',
        backgroundColor: '#5c4033',
        color: 'white',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer'
    }
};

export default BuyerDashboard;