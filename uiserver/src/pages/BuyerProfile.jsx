import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getBuyerProfile, updateBuyerProfile } from '../api/buyer';
import { useAuth } from '../context/AuthContext';

// This page lets buyers view and edit their meat preferences from the dashboard
function BuyerProfile() {
    // Profile data from the backend
    const [profile, setProfile] = useState(null);

    // Form fields for edit mode
    const [preferredCuts, setPreferredCuts] = useState('');
    const [quantity, setQuantity] = useState('');

    // UI state
    const [isEditing, setIsEditing] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const { user } = useAuth();
    const navigate = useNavigate();

    // Runs once when the page loads to fetch the buyer's profile from the backend
    useEffect(() => {
        async function fetchProfile() {
            try {
                const data = await getBuyerProfile(user.id);
                setProfile(data);

                // Pre-fill the form fields with the current values
                setPreferredCuts(data.preferredCuts || '');
                setQuantity(data.quantity || '');
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }

        fetchProfile();
    }, []);

    // Called when the buyer clicks save after editing their profile
    const handleSave = async (e) => {
        e.preventDefault();
        setError('');

        try {
            const updated = await updateBuyerProfile(user.id, {
                preferredCuts,
                quantity
            });

            setProfile(updated);
            setIsEditing(false);
        } catch (err) {
            setError(err.message);
        }
    };

    // Cancel editing and reset the form fields back to the current profile values
    const handleCancel = () => {
        setPreferredCuts(profile.preferredCuts || '');
        setQuantity(profile.quantity || '');
        setIsEditing(false);
    };

    if (loading) return <div style={styles.container}><p>Loading...</p></div>;
    if (!profile && error) return <div style={styles.container}><p style={{ color: '#c00' }}>{error}</p></div>;

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <button onClick={() => navigate('/buyer/dashboard')} style={styles.backButton}>
                    Back to Dashboard
                </button>
                <h1 style={styles.title}>Meat Preferences</h1>

                {error && <div style={styles.error}>{error}</div>}

                {/* View mode */}
                {!isEditing ? (
                    <div>
                        <div style={styles.field}>
                            <span style={styles.fieldLabel}>Preferred Cuts:</span>
                            <span>{profile.preferredCuts || 'Not set'}</span>
                        </div>
                        <div style={styles.field}>
                            <span style={styles.fieldLabel}>Quantity:</span>
                            <span>{profile.quantity || 'Not set'}</span>
                        </div>

                        <button onClick={() => setIsEditing(true)} style={styles.button}>
                            Edit Preferences
                        </button>
                    </div>
                ) : (
                    /* Edit mode */
                    <form onSubmit={handleSave} style={styles.form}>
                        <div style={styles.inputGroup}>
                            <label style={styles.label}>Preferred Cuts</label>
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

                        <div style={styles.buttonRow}>
                            <button type="submit" style={styles.button}>Save</button>
                            <button type="button" onClick={handleCancel} style={styles.cancelButton}>Cancel</button>
                        </div>
                    </form>
                )}
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
        marginBottom: '24px',
        fontSize: '28px'
    },
    field: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '12px 0',
        borderBottom: '1px solid #eee'
    },
    fieldLabel: {
        fontWeight: '500',
        color: '#333'
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
        marginTop: '16px'
    },
    cancelButton: {
        padding: '12px',
        backgroundColor: '#ccc',
        color: '#333',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer',
        marginTop: '8px'
    },
    buttonRow: {
        display: 'flex',
        flexDirection: 'column',
        gap: '0px'
    },
    backButton: {
        padding: '8px 16px',
        backgroundColor: 'transparent',
        color: '#4a7c59',
        border: '1px solid #4a7c59',
        borderRadius: '4px',
        fontSize: '14px',
        cursor: 'pointer',
        marginBottom: '16px'
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

export default BuyerProfile;
