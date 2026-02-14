import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createSellerProfile } from "../api/seller"; 

function SellerProfileSetup() {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        shopName: "",
        shopAddress: ""
    });

    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            await createSellerProfile(formData);
            navigate("/seller/dashboard");
        } catch (err) {
            setError("Failed to create seller profile");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: "40px", textAlign: "center" }}>
            <h2>Complete Your Seller Profile</h2>

            {error && <p style={{ color: "red" }}>{error}</p>}

            <form onSubmit={handleSubmit} style={{ maxWidth: "400px", margin: "0 auto" }}>
                <div style={{ marginBottom: "15px" }}>
                    <input
                        type="text"
                        name="shopName"
                        placeholder="Shop Name"
                        value={formData.shopName}
                        onChange={handleChange}
                        required
                        style={{ width: "100%", padding: "10px" }}
                    />
                </div>

                <div style={{ marginBottom: "15px" }}>
                    <input
                        type="text"
                        name="shopAddress"
                        placeholder="Shop Address"
                        value={formData.shopAddress}
                        onChange={handleChange}
                        required
                        style={{ width: "100%", padding: "10px" }}
                    />
                </div>

                <button type="submit" disabled={loading}>
                    {loading ? "Saving..." : "Finish Setup"}
                </button>
            </form>
        </div>
    );
}

export default SellerProfileSetup;
