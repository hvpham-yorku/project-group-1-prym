import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createBuyerProfile } from '../api/buyer';
import { useAuth } from '../context/AuthContext';
import CowDiagram from '../components/CowDiagram';

function BuyerProfileSetup() {
    // { cutId: quantity }  e.g. { 'Chuck': 1, 'Rib': 2 }
    const [selectedCuts, setSelectedCuts] = useState({});
    const [quantity, setQuantity] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();
    const { user } = useAuth();

    // Add with qty=1, or remove if already selected
    const handleToggle = (cut) => {
        setSelectedCuts(prev => {
            if (cut in prev) {
                const next = { ...prev };
                delete next[cut];
                return next;
            }
            return { ...prev, [cut]: 1 };
        });
    };

    // delta = +1 or -1; deselects if qty would drop below 1
    const handleQuantityChange = (id, delta) => {
        setSelectedCuts(prev => {
            const qty = (prev[id] ?? 1) + delta;
            if (qty < 1) {
                const next = { ...prev };
                delete next[id];
                return next;
            }
            if (qty > 2) return prev;
            return { ...prev, [id]: qty };
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (Object.keys(selectedCuts).length === 0) {
            setError('Please select at least one preferred cut from the diagram.');
            return;
        }

        setLoading(true);
        try {
            const preferredCuts = Object.entries(selectedCuts)
                .map(([cut, qty]) => qty > 1 ? `${cut} x${qty}` : cut)
                .join(', ');

            await createBuyerProfile({ userId: user.id, preferredCuts, quantity });
            navigate('/buyer/dashboard');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const cutCount = Object.keys(selectedCuts).length;

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Meat Preferences</h1>
                <p style={styles.subtitle}>
                    Click cuts to select — then use − / + to set your desired quantity
                </p>

                {error && <div style={styles.error}>{error}</div>}

                <form onSubmit={handleSubmit} style={styles.form}>

                    <div style={styles.diagramWrapper}>
                        <CowDiagram
                            selectedCuts={selectedCuts}
                            onToggle={handleToggle}
                            onQuantityChange={handleQuantityChange}
                        />
                    </div>

                    {/* Selected cut tags */}
                    <div style={styles.tagsArea}>
                        {cutCount === 0 ? (
                            <p style={styles.noSelection}>
                                No cuts selected — click the diagram above
                            </p>
                        ) : (
                            <div style={styles.tagsRow}>
                                {Object.entries(selectedCuts).map(([cut, qty]) => (
                                    <span
                                        key={cut}
                                        style={styles.tag}
                                        onClick={() => handleToggle(cut)}
                                        title="Click to remove"
                                    >
                                        {cut}{qty > 1 ? ` ×${qty}` : ''} ✕
                                    </span>
                                ))}
                            </div>
                        )}
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
        maxWidth: '780px',
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
        fontWeight: '500',
        fontSize: '14px'
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
    },
    diagramWrapper: {
        display: 'flex',
        justifyContent: 'center'
    },
    tagsArea: {
        minHeight: '36px',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center'
    },
    tagsRow: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: '8px',
        justifyContent: 'center'
    },
    tag: {
        padding: '5px 14px',
        backgroundColor: '#4a7c59',
        color: 'white',
        borderRadius: '999px',
        fontSize: '13px',
        fontWeight: '600',
        cursor: 'pointer',
        userSelect: 'none'
    },
    noSelection: {
        textAlign: 'center',
        color: '#aaa',
        fontStyle: 'italic',
        fontSize: '14px',
        margin: 0
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
        marginTop: '4px'
    },
    error: {
        backgroundColor: '#fee',
        color: '#c00',
        padding: '12px',
        borderRadius: '4px',
        textAlign: 'center'
    }
};

export default BuyerProfileSetup;
