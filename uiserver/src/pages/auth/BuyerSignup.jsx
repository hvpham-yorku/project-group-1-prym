import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerBuyer } from '../../api/auth';
import { useAuth } from '../../context/AuthContext';
import { validateSignUp } from '../../api/signUpValidation';
import { getStyling } from '../../api/signUpStyles';

//Buyer registration page. Collects personal info, profile pic, zip code, etc.
//After registering, sends the user to the buyer profile setup page to pick preferred cuts.
function BuyerSignup() {
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        confirmPassword: '',
        username: '',
        firstName: '',
        lastName: '',
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

    //reads the selected image file and converts it to base64 for storage
    //also enforces a 2MB size limit so we dont blow up the database
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

        validateSetup(formData, setError);

        setLoading(true);

        try {
            const user = await registerBuyer({
                email: formData.email,
                password: formData.password,
                username: formData.username,
                firstName: formData.firstName,
                lastName: formData.lastName,
                phoneNumber: formData.phoneNumber,
                profilePicture: formData.profilePicture || null,
                zipCode: formData.zipCode
            });
            saveUser(user);
            navigate('/buyer/profile-setup'); // redirect to profile setup instead of dashboard
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };
	const styles = getStyling('#4a7c59');
	
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
                            Used to show you nearby farms (US or Canada)
                        </span>
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



export default BuyerSignup;
