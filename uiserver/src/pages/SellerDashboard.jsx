import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getSellerProfile, updateSellerProfile } from "../api/seller";

function SellerDashboard() {
  const { user, clearUser, saveUser } = useAuth();
  const navigate = useNavigate();

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    shopName: "",
    phoneNumber: "",
    shopAddress: "",
    category: "",
    description: "",
  });
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        const data = await getSellerProfile(user.id);
        setProfile(data);
        setFormData({
          shopName: data.shopName || "",
          phoneNumber: data.phoneNumber || "",
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

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error("Logout failed:", error);
    } finally {
      clearUser();
      navigate("/login");
    }
  };

  const handleSave = async () => {
    try {
      await updateSellerProfile(user.id, { ...formData });

      setProfile((prev) => ({
        ...prev,
        shopName: formData.shopName,
        phoneNumber: formData.phoneNumber,
        shopAddress: formData.shopAddress,
        category: formData.category,
        description: formData.description,
      }));
      setIsEditing(false);
      saveUser({ ...user, phoneNumber: formData.phoneNumber });
    } catch (err) {
      console.error("Save error:", err);
      setError("Failed to save profile.");
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "50px" }}>Loading...</div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {/* Profile Picture */}
        <div style={styles.profilePictureContainer}>
          {user?.profilePicture ? (
            <img
              src={user.profilePicture}
              alt="Profile"
              style={styles.profilePicture}
            />
          ) : (
            <div style={styles.profilePicturePlaceholder}>
              {user?.firstName?.charAt(0)}
              {user?.lastName?.charAt(0)}
            </div>
          )}
        </div>

        <h1 style={styles.title}>Seller Dashboard</h1>
        <p style={styles.welcome}>Welcome, {user?.username}!</p>
        <p style={styles.name}>
          {user?.firstName} {user?.lastName}
        </p>
        <p style={styles.info}>
          You are logged in as a <strong>SELLER</strong>
        </p>

        {error && <div style={styles.error}>{error}</div>}

        {/* Info Card */}
        <div style={styles.infoCard}>
          <div style={styles.infoRow}>
            <span style={styles.infoLabel}>Email:</span>
            <span style={styles.infoValue}>{user?.email}</span>
          </div>
          <div style={styles.infoRow}>
            <span style={styles.infoLabel}>Phone:</span>
            {isEditing ? (
              <input
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                style={styles.editInput}
              />
            ) : (
              <span style={styles.infoValue}>{profile?.phoneNumber}</span>
            )}
          </div>
          <div style={styles.infoRow}>
            <span style={styles.infoLabel}>Shop Name:</span>
            {isEditing ? (
              <input
                name="shopName"
                value={formData.shopName}
                onChange={handleChange}
                style={styles.editInput}
              />
            ) : (
              <span style={styles.infoValue}>{profile?.shopName}</span>
            )}
          </div>
          <div style={styles.infoRow}>
            <span style={styles.infoLabel}>Address:</span>
            {isEditing ? (
              <input
                name="shopAddress"
                value={formData.shopAddress}
                onChange={handleChange}
                style={styles.editInput}
              />
            ) : (
              <span style={styles.infoValue}>{profile?.shopAddress}</span>
            )}
          </div>
          <div style={styles.infoRow}>
            <span style={styles.infoLabel}>Category:</span>
            {isEditing ? (
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                style={styles.editInput}
              >
                <option value="">Select</option>
                <option value="HALAL">Halal</option>
                <option value="KOSHER">Kosher</option>
                <option value="ORGANIC">Organic</option>
                <option value="CONVENTIONAL">Conventional</option>
              </select>
            ) : (
              <span style={styles.infoValue}>{profile?.category}</span>
            )}
          </div>
          <div style={styles.infoRow}>
            <span style={styles.infoLabel}>Description:</span>
            {isEditing ? (
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                style={styles.editInput}
                rows={3}
              />
            ) : (
              <span style={styles.infoValue}>{profile?.description}</span>
            )}
          </div>
        </div>

        {/* Buttons */}
        <div style={{ display: "flex", justifyContent: "center", gap: "12px" }}>
          {isEditing ? (
            <>
              <button style={styles.button} onClick={handleSave}>
                Save
              </button>
              <button
                style={styles.secondaryButton}
                onClick={() => {
                  setFormData({
                    shopName: profile?.shopName || "",
                    phoneNumber: profile?.phoneNumber || "",
                    shopAddress: profile?.shopAddress || "",
                    category: profile?.category || "",
                    description: profile?.description || "",
                  });
                  setIsEditing(false);
                }}
              >
                Discard
              </button>
            </>
          ) : (
            <>
              <button style={styles.button} onClick={() => setIsEditing(true)}>
                Edit Profile
              </button>
              <button style={styles.secondaryButton} onClick={handleLogout}>
                Logout
              </button>
            </>
          )}
        </div>
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
    boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
    textAlign: "center",
    border: "2px solid #5c4033",
    minWidth: "350px",
  },
  profilePictureContainer: {
    display: "flex",
    justifyContent: "center",
    marginBottom: "20px",
  },
  profilePicture: {
    width: "120px",
    height: "120px",
    borderRadius: "50%",
    objectFit: "cover",
    border: "3px solid #5c4033",
  },
  profilePicturePlaceholder: {
    width: "120px",
    height: "120px",
    borderRadius: "50%",
    backgroundColor: "#5c4033",
    color: "white",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "36px",
    fontWeight: "bold",
  },
  title: {
    color: "#5c4033",
    marginBottom: "8px",
  },
  welcome: {
    fontSize: "22px",
    color: "#333",
    fontWeight: "600",
    marginBottom: "4px",
  },
  name: {
    fontSize: "16px",
    color: "#666",
    marginBottom: "8px",
  },
  info: {
    color: "#666",
    marginBottom: "20px",
  },
  infoCard: {
    backgroundColor: "#f9f9f9",
    borderRadius: "8px",
    padding: "16px",
    marginBottom: "24px",
    textAlign: "left",
  },
  infoRow: {
    display: "flex",
    justifyContent: "space-between",
    padding: "8px 0",
    borderBottom: "1px solid #eee",
  },
  infoLabel: {
    color: "#666",
    fontWeight: "500",
  },
  infoValue: {
    color: "#333",
  },
  button: {
    padding: "12px 24px",
    backgroundColor: "#4a7c59",
    color: "white",
    border: "none",
    borderRadius: "4px",
    fontSize: "16px",
    cursor: "pointer",
  },
  error: {
    backgroundColor: "#fee",
    color: "#c00",
    padding: "12px",
    borderRadius: "4px",
    marginBottom: "16px",
    textAlign: "center",
  },
  editInput: {
    padding: "6px",
    borderRadius: "4px",
    border: "1px solid #ccc",
    fontSize: "14px",
  },
  secondaryButton: {
    padding: "12px 24px",
    backgroundColor: "#5c4033",
    color: "white",
    border: "none",
    borderRadius: "4px",
    fontSize: "16px",
    cursor: "pointer",
  },
};

export default SellerDashboard;
