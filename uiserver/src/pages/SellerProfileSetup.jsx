import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { updateSellerProfile, getSellerProfile } from "../api/seller";

function SellerProfileSetup() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [formData, setFormData] = useState({
    shopName: "",
    shopAddress: "",
    category: "",
    description: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const data = await getSellerProfile(user.id);
        setFormData({
          shopName: data.shopName || "",
          shopAddress: data.shopAddress || "",
          category: data.category || "",
          description: data.description || "",
        });
      } catch (err) {
        setError("Failed to load profile.");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [user?.id]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await updateSellerProfile(user?.id, formData);
      navigate("/seller/dashboard");
    } catch (err) {
      console.error("Setup Error:", err);
      setError("Failed to create profile. Check if the server is running.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
        <div style={styles.card}>
            <h1 style={styles.title}>Complete Your Profile</h1>
            <p style={styles.subtitle}>Tell buyers about your farm</p>

            {error && <div style={styles.error}>{error}</div>}

            <form onSubmit={handleSubmit} style={styles.form}>

                <div style={styles.inputGroup}>
                    <label style={styles.label}>Shop Name</label>
                    <input
                        type="text"
                        name="shopName"
                        value={formData.shopName}
                        style={styles.readOnly}
                        readOnly
                    />
                </div>

                <div style={styles.inputGroup}>
                    <label style={styles.label}>Shop Address</label>
                    <input
                        type="text"
                        name="shopAddress"
                        value={formData.shopAddress}
                        onChange={handleChange}
                        placeholder="e.g., 123 Farm Road, Ontario"
                        style={styles.input}
                        required
                    />
                </div>

                <div style={styles.inputGroup}>
                    <label style={styles.label}>Category</label>
                    <select
                        name="category"
                        value={formData.category}
                        onChange={handleChange}
                        style={styles.input}
                        required
                    >
                        <option value="">Select a category</option>
                        <option value="HALAL">Halal</option>
                        <option value="KOSHER">Kosher</option>
                        <option value="ORGANIC">Organic</option>
                        <option value="CONVENTIONAL">Conventional</option>
                    </select>
                </div>

                <div style={styles.inputGroup}>
                    <label style={styles.label}>Farm Description</label>
                    <textarea
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        placeholder="Tell buyers about your farm, cattle, and practices..."
                        style={styles.textarea}
                        rows={4}
                    />
                </div>

                <button type="submit" style={styles.button} disabled={loading}>
                    {loading ? "Saving..." : "Complete Profile"}
                </button>

            </form>
        </div>
    </div>
);
}

const styles = {
  container: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f5f5f0",
    padding: "20px",
  },
  card: {
    backgroundColor: "white",
    padding: "40px",
    borderRadius: "8px",
    boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)",
    width: "100%",
    maxWidth: "400px",
    border: "2px solid #5c4033",
  },
  title: {
    color: "#5c4033",
    textAlign: "center",
    marginBottom: "8px",
    fontSize: "28px",
  },
  subtitle: {
    color: "#4a7c59",
    textAlign: "center",
    marginBottom: "24px",
    fontWeight: "500",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  inputGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  label: {
    color: "#333",
    fontWeight: "500",
  },
  input: {
    padding: "12px",
    borderRadius: "4px",
    border: "1px solid #ccc",
    fontSize: "16px",
  },
  button: {
    padding: "12px",
    backgroundColor: "#4a7c59",
    color: "white",
    border: "none",
    borderRadius: "4px",
    fontSize: "16px",
    cursor: "pointer",
    marginTop: "8px",
  },
  error: {
    backgroundColor: "#fee",
    color: "#c00",
    padding: "12px",
    borderRadius: "4px",
    marginBottom: "16px",
    textAlign: "center",
  },
  textarea: {
    padding: "12px",
    borderRadius: "4px",
    border: "1px solid #ccc",
    fontSize: "16px",
    resize: "vertical",
    fontFamily: "inherit",
  },
  readOnly: {
    padding: "12px",
    borderRadius: "4px",
    border: "1px solid #ccc",
    fontSize: "16px",
    backgroundColor: "#f0f0f0",
    color: "#888",
  },
};

export default SellerProfileSetup;
