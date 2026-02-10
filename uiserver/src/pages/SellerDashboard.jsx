import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/auth';
function SellerDashboard() {
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
                <h1 style={styles.title}>Seller Dashboard</h1>
                <p style={styles.welcome}>Welcome, {user?.email}!</p>
                <p style={styles.info}>You are logged in as a <strong>SELLER</strong></p>
                
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
        border: '2px solid #5c4033'
    },
    title: {
        color: '#5c4033',
        marginBottom: '16px'
    },
    welcome: {
        fontSize: '18px',
        color: '#333'
    },
    info: {
        color: '#666',
        marginBottom: '24px'
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