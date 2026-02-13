import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerBuyer } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';

function BuyerSignup() {
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        confirmPassword: '',
        username: '',
        firstName: '',
        lastName: '',
        phoneNumber: '',
        profilePicture: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [imagePreview, setImagePreview] = useState(null);

    const navigate = useNavigate();
    const { saveUser } = useAuth();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            // Check file size (limit to 2MB)
            if (file.size > 2 * 1024 * 1024) {
                setError('Image must be less than 2MB');
                return;
            }

            const reader = new FileReader();
            reader.onloadend = () => {
                const base64String = reader.result;
                setFormData(prev => ({ ...prev, profilePicture: base64String }));
                setImagePreview(base64String);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        // Validation
        if (formData.password !== formData.confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        if (formData.password.length < 8) {
            setError('Password must be at least 8 characters');
            return;
        }

        if (formData.username.length < 3) {
            setError('Username must be at least 3 characters');
            return;
        }

        // Basic phone validation (adjust regex as needed)
        const phoneRegex = /^[\d\s\-\+\(\)]{10,}$/;
        if (!phoneRegex.test(formData.phoneNumber)) {
            setError('Please enter a valid phone number');
            return;
        }

        setLoading(true);

        try {
            const user = await registerBuyer({
                email: formData.email,
                password: formData.password,
                username: formData.username,
                firstName: formData.firstName,
                lastName: formData.lastName,
                phoneNumber: formData.phoneNumber,
                profilePicture: formData.profilePicture || null
            });
            saveUser(user);
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
                <h1 style={styles.title}>Join PRYM</h1>
                <p style={styles.subtitle}>Create a Buyer Account</p>

                {error && <div style={styles.error}>{error}</div>}

                <form onSubmit={handleSubmit} style={styles.form}>
                    {/* Profile Picture Upload */}
                    <div style={styles.imageUploadContainer}>
                        <div style={styles.imagePreview}>
                            {imagePreview ? (
                                <img src={imagePreview} alt="Profile preview" style={styles.previewImg} />
                            ) : (
                                <span style={styles.placeholderText}>No image</span>
                            )}
                        </div>
                        <label style={styles.uploadLabel}>
                            Choose Profile Picture
                            <input
                                type="file"
                                accept="image/*"
                                onChange={handleImageChange}
                                style={styles.fileInput}
                            />
                        </label>
                    </div>

                    {/* Name Fields - Side by Side */}
                    <div style={styles.row}>
                        <div style={styles.halfWidth}>
                            <label style={styles.label}>First Name</label>
                            <input
                                type="text"
                                name="firstName"
                                value={formData.firstName}
                                onChange={handleChange}
                                style={styles.input}
                                required
                            />
                        </div>
                        <div style={styles.halfWidth}>
                            <label style={styles.label}>Last Name</label>
                            <input
                                type="text"
                                name="lastName"
                                value={formData.lastName}
                                onChange={handleChange}
                                style={styles.input}
                                required
                            />
                        </div>
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Username</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            style={styles.input}
                            required
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Email</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            style={styles.input}
                            required
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Phone Number</label>
                        <input
                            type="tel"
                            name="phoneNumber"
                            value={formData.phoneNumber}
                            onChange={handleChange}
                            placeholder="e.g., 416-555-1234"
                            style={styles.input}
                            required
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Password</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            style={styles.input}
                            required
                        />
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Confirm Password</label>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            style={styles.input}
                            required
                        />
                    </div>

                    <button type="submit" style={styles.button} disabled={loading}>
                        {loading ? 'Creating Account...' : 'Sign Up as Buyer'}
                    </button>
                </form>

                <div style={styles.links}>
                    <p>Already have an account? <Link to="/login" style={styles.link}>Sign in</Link></p>
                    <p>Want to sell? <Link to="/register/seller" style={styles.link}>Sign up as Seller</Link></p>
                </div>
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
        maxWidth: '500px',
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
    row: {
        display: 'flex',
        gap: '16px'
    },
    halfWidth: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        gap: '4px'
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
    },
    links: {
        marginTop: '24px',
        textAlign: 'center',
        color: '#666'
    },
    link: {
        color: '#4a7c59',
        textDecoration: 'none',
        fontWeight: '500'
    },
    imageUploadContainer: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '12px'
    },
    imagePreview: {
        width: '100px',
        height: '100px',
        borderRadius: '50%',
        backgroundColor: '#e0e0e0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        border: '2px solid #ccc'
    },
    previewImg: {
        width: '100%',
        height: '100%',
        objectFit: 'cover'
    },
    placeholderText: {
        color: '#999',
        fontSize: '12px'
    },
    uploadLabel: {
        padding: '8px 16px',
        backgroundColor: '#5c4033',
        color: 'white',
        borderRadius: '4px',
        cursor: 'pointer',
        fontSize: '14px'
    },
    fileInput: {
        display: 'none'
    }
};

export default BuyerSignup;