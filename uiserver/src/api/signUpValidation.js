export function validateSignUp(formData, setError) {
	// Validation
	        if (formData.password !== formData.confirmPassword) {
	            setError('Passwords do not match');
	            return false;
	        }

	        if (formData.password.length < 8) {
	            setError('Password must be at least 8 characters');
	            return false;
	        }

	        if (formData.username.length < 3) {
	            setError('Username must be at least 3 characters');
	            return false;
	        }

	        // Basic phone validation (adjust regex as needed)
	      const phoneRegex = /^(\+?1[\s.\-]?)?(\(?\d{3}\)?[\s.\-]?)\d{3}[\s.\-]?\d{4}$/;

	        if (!phoneRegex.test(formData.phoneNumber)) {
	            setError('Please enter a valid phone number');
	            return false;
	        }

	        // Basic postal code format check (3-10 alphanumeric chars; backend validates via Nominatim)
	        if (!formData.zipCode || formData.zipCode.trim().length < 3) {
	            setError('Please enter a valid postal/ZIP code');
	            return false;
	        }
			
			return true;
}

export default validateSignUp;