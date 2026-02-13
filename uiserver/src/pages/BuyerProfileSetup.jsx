import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createBuyerProfile } from '../api/buyer';
import { useAuth } from '../context/AuthContext';

// This page appears right after signup so the buyer can set their meat preferences
function BuyerProfileSetup() {
    const [preferredCuts, setPreferredCuts] = useState('');
    const [quantity, setQuantity] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();
    const { user } = useAuth(); // get the logged-in user so we can send their userId

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            // send meat preferences to the backend
            await createBuyerProfile({
                userId: user.id,
                preferredCuts,
                quantity
            });

            // profile created successfully, go to dashboard
            navigate('/buyer/dashboard');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Meat Preferences</h1>
                <p style={styles.subtitle}>Tell us what you're looking for</p>

                {error && <div style={styles.error}>{error}</div>}

                <form onSubmit={handleSubmit} style={styles.form}>
                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Preferred Cuts (e.g., Ribeye, Sirloin)</label>
                        <input
                            type="text"
                            value={preferredCuts}
                            onChange={(e) => setPreferredCuts(e.target.value)}
                            style={styles.input}
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Quantity</label>
                        <select
                            value={quantity}
                            onChange={(e) => setQuantity(e.target.value)}
                            style={styles.input}
                        >
                            <option value="">Select quantity</option>
                            <option value="Quarter cow">Quarter cow</option>
                            <option value="Half cow">Half cow</option>
                            <option value="Whole cow">Whole cow</option>
                        </select>
                    </div>

                    <button type="submit" style={styles.button} disabled={loading}>
                        {loading ? 'Saving...' : 'Complete Profile'}
                    </button>
                </form>
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
        width: '100%',
        maxWidth: '400px',
        border: '2px solid #5c4033'
    },
    title: {
        color: '#5c4033',
        textAlign: 'center',
        marginBottom: '8px',
        fontSize: '28px'
    },
    subtitle: {
        color: '#4a7c59',
        textAlign: 'center',
        marginBottom: '24px',
        fontWeight: '500'
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '16px'
    },
    inputGroup: {
        display: 'flex',
        flexDirection: 'column',
        gap: '4px'
    },
    label: {
        color: '#333',
        fontWeight: '500'
    },
    input: {
        padding: '12px',
        borderRadius: '4px',
        border: '1px solid #ccc',
        fontSize: '16px'
    },
    button: {
        padding: '12px',
        backgroundColor: '#4a7c59',
        color: 'white',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer',
        marginTop: '8px'
    },
    error: {
        backgroundColor: '#fee',
        color: '#c00',
        padding: '12px',
        borderRadius: '4px',
        marginBottom: '16px',
        textAlign: 'center'
    }
};

export default BuyerProfileSetup;
