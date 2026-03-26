import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { updateSellerProfile, getSellerProfile, getCertifications, setCertifications } from "../api/seller";
import { ALL_CERTS } from "../constants/certifications";

function SellerProfileSetup() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [description, setDescription] = useState("");
  const [selectedCerts, setSelectedCerts] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const [profile, certs] = await Promise.all([
          getSellerProfile(user.id),
          getCertifications(user.id),
        ]);
        setDescription(profile.description || "");
        setSelectedCerts(certs.map((c) => c.name));
      } catch (err) {
        setError("Failed to load profile.");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [user?.id]);

  const toggleCert = (value) => {
    setSelectedCerts((prev) =>
      prev.includes(value) ? prev.filter((c) => c !== value) : [...prev, value]
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await updateSellerProfile(user.id, { description });
      await setCertifications(user.id, selectedCerts);
      navigate("/seller/dashboard");
    } catch (err) {
      console.error("Setup Error:", err);
      setError("Failed to save profile. Check if the server is running.");
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
            <label style={styles.label}>Certifications</label>
            <p style={styles.hint}>Select all that apply to your farm</p>
            <div style={styles.certGrid}>
              {ALL_CERTS.map((cert) => (
                <label key={cert.value} style={styles.certOption}>
                  <input
                    type="checkbox"
                    checked={selectedCerts.includes(cert.value)}
                    onChange={() => toggleCert(cert.value)}
                    disabled={loading}
                    style={styles.checkbox}
                  />
                  {cert.label}
                </label>
              ))}
            </div>
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>Farm Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Tell buyers about your farm, cattle, and practices..."
              style={styles.textarea}
              rows={4}
              disabled={loading}
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
    maxWidth: "480px",
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
    marginBottom: "28px",
    fontWeight: "500",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "24px",
  },
  inputGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  label: {
    color: "#333",
    fontWeight: "600",
    fontSize: "15px",
  },
  hint: {
    color: "#888",
    fontSize: "13px",
    margin: 0,
  },
  certGrid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "10px",
    marginTop: "4px",
  },
  certOption: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    fontSize: "14px",
    color: "#333",
    cursor: "pointer",
  },
  checkbox: {
    accentColor: "#4a7c59",
    width: "16px",
    height: "16px",
    cursor: "pointer",
  },
  textarea: {
    padding: "12px",
    borderRadius: "4px",
    border: "1px solid #ccc",
    fontSize: "15px",
    resize: "vertical",
    fontFamily: "inherit",
  },
  button: {
    padding: "12px",
    backgroundColor: "#4a7c59",
    color: "white",
    border: "none",
    borderRadius: "4px",
    fontSize: "16px",
    cursor: "pointer",
    marginTop: "4px",
  },
  error: {
    backgroundColor: "#fee",
    color: "#c00",
    padding: "12px",
    borderRadius: "4px",
    marginBottom: "8px",
    textAlign: "center",
  },
};

export default SellerProfileSetup;
