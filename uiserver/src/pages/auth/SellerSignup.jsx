import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerSeller } from "../../api/auth";
import { useAuth } from '../../context/AuthContext';
import { validateSignUp } from '../../api/signUpValidation';
import { getStyling } from '../../api/signUpStyles';

//Seller registration page. Pretty similar to buyer signup but also collects
//the shop name. After registering, redirects to the seller profile setup page.
function SellerSignup() {
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        confirmPassword: '',
        username: '',
        firstName: '',
        lastName: '',
        shopName:'',
        phoneNumber: '',
        profilePicture: '',
        zipCode: ''
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
            if (file.size > 2 * 1024 * 1024) { // 2MB limit
                setError('Image must be less than 2MB');
                return;
            }

            const reader = new FileReader();
            reader.onloadend = () => {
                setFormData(prev => ({ ...prev, profilePicture: reader.result }));
                setImagePreview(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        validateSignUp(formData, setError);

        setLoading(true);

        try {
            const user = await registerSeller({
                email: formData.email,
                password: formData.password,
                username: formData.username,
                firstName: formData.firstName,
                lastName: formData.lastName,
                shopName: formData.shopName,
                phoneNumber: formData.phoneNumber,
                profilePicture: formData.profilePicture || null,
                zipCode: formData.zipCode
            });

            saveUser(user);

            // Redirect to Seller Profile Setup after signup
            navigate('/seller/profile-setup');
        } catch (err) {
            setError(err.message || 'Failed to create seller account');
        } finally {
            setLoading(false);
        }
    };
	const styles = getStyling('#5c4033');
	
    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1 style={styles.title}>Join PRYM</h1>
                <p style={styles.subtitle}>Create a Seller Account</p>

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

                    {/* Name Fields */}
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
                        <label style={styles.label}>ZIP / Postal Code</label>
                        <input
                            type="text"
                            name="zipCode"
                            value={formData.zipCode}
                            onChange={handleChange}
                            placeholder="e.g., 10001 or M5H 2N2"
                            style={styles.input}
                            maxLength="7"
                            required
                        />
                        <span style={styles.hint}>
                            Used to show you nearby buyers (US or Canada)
                        </span>
                    </div>

                    <div style={styles.inputGroup}>
                        <label style={styles.label}>Shop Name</label>
                        <input
                            type="text"
                            name="shopName"
                            value={formData.shopName}
                            onChange={handleChange}
                            style={styles.input}
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
                        {loading ? 'Creating Account...' : 'Sign Up as Seller'}
                    </button>
                </form>

                <div style={styles.links}>
                    <p>Already have an account? <Link to="/login" style={styles.link}>Sign in</Link></p>
                    <p>Want to buy? <Link to="/register/buyer" style={styles.link}>Sign up as Buyer</Link></p>
                </div>
            </div>
        </div>
    );
}

export default SellerSignup;
