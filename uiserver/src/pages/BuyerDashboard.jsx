import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getBuyerProfile, updateBuyerProfile } from "../api/buyer";

function BuyerDashboard() {
  const { user, clearUser, saveUser } = useAuth();
  const navigate = useNavigate();

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    phoneNumber: "",
    preferredCuts: "",
    quantity: "",
  });
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        const data = await getBuyerProfile(user.id);
        setProfile(data);
        setFormData({
          phoneNumber: user?.phoneNumber || "",
          preferredCuts: data.preferredCuts || "",
          quantity: data.quantity || "",
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
    setError("");
    try {
      if (formData.phoneNumber) {
        const phoneRegex = /^\+?[\d]{1,3}?[\s\-.]?\(?\d{1,4}\)?[\s\-.]?\d{1,4}[\s\-.]?\d{1,9}$/;
        if (!phoneRegex.test(formData.phoneNumber)) {
          setError("Please enter a valid phone number.");
          return;
        }
      }
      await updateBuyerProfile(user.id, {
        preferredCuts: formData.preferredCuts,
        quantity: formData.quantity,
        phoneNumber: formData.phoneNumber,
      });
      const savedPhone = formData.phoneNumber || user?.phoneNumber;
      setProfile((prev) => ({
        ...prev,
        preferredCuts: formData.preferredCuts,
        quantity: formData.quantity,
      }));
      setFormData((prev) => ({ ...prev, phoneNumber: savedPhone }));
      saveUser({ ...user, phoneNumber: savedPhone });
      setIsEditing(false);
    } catch (err) {
      console.error("Save error:", err);
      setError("Failed to save profile.");
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleDiscard = () => {
    setFormData({
      phoneNumber: user?.phoneNumber || "",
      preferredCuts: profile?.preferredCuts || "",
      quantity: profile?.quantity || "",
    });
    setError("");
    setIsEditing(false);
  };

  const initials =
    (user?.firstName?.charAt(0) || "") + (user?.lastName?.charAt(0) || "");

  if (loading) {
    return (
      <div style={styles.page}>
        <p style={{ padding: "48px", color: "#666" }}>Loading...</p>
      </div>
    );
  }

  return (
    <div style={styles.page}>

      {/* Header Banner */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <div style={styles.avatar}>{initials}</div>
          <div>
            <h1 style={styles.bannerName}>{user?.firstName} {user?.lastName}</h1>
            <p style={styles.bannerEmail}>{user?.email}</p>
            <span style={styles.roleBadge}>BUYER</span>
          </div>
        </div>
      </div>

      {/* Content */}
      <div style={styles.content}>

        {error && <div style={styles.error}>{error}</div>}

        {/* Fields Grid */}
        <div style={styles.grid}>

          {/* Phone */}
          <div style={styles.fieldCard}>
            <p style={styles.fieldLabel}>Phone Number</p>
            {isEditing ? (
              <input
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                placeholder="e.g. +1 416 555 0000"
                style={styles.fieldInput}
              />
            ) : (
              <p style={user?.phoneNumber ? styles.fieldValue : styles.fieldValueEmpty}>
                {user?.phoneNumber || "Not set"}
              </p>
            )}
          </div>

          {/* Preferred Cuts */}
          <div style={styles.fieldCard}>
            <p style={styles.fieldLabel}>Preferred Cuts</p>
            {isEditing ? (
              <input
                name="preferredCuts"
                value={formData.preferredCuts}
                onChange={handleChange}
                placeholder="e.g. ribeye, brisket"
                style={styles.fieldInput}
              />
            ) : (
              <p style={profile?.preferredCuts ? styles.fieldValue : styles.fieldValueEmpty}>
                {profile?.preferredCuts || "Not set"}
              </p>
            )}
          </div>

          {/* Quantity — full width */}
          <div style={{ ...styles.fieldCard, gridColumn: "1 / -1" }}>
            <p style={styles.fieldLabel}>Quantity</p>
            {isEditing ? (
              <select
                name="quantity"
                value={formData.quantity}
                onChange={handleChange}
                style={styles.fieldInput}
              >
                <option value="">Select quantity</option>
                <option value="Quarter cow">Quarter cow</option>
                <option value="Half cow">Half cow</option>
                <option value="Whole cow">Whole cow</option>
              </select>
            ) : (
              <p style={profile?.quantity ? styles.fieldValue : styles.fieldValueEmpty}>
                {profile?.quantity || "Not set"}
              </p>
            )}
          </div>

        </div>

        {/* Buttons */}
        <div style={styles.buttonRow}>
          {isEditing ? (
            <>
              <button style={styles.secondaryButton} onClick={handleDiscard}>Discard</button>
              <button style={styles.primaryButton} onClick={handleSave}>Save Changes</button>
            </>
          ) : (
            <>
              <button style={styles.secondaryButton} onClick={handleLogout}>Logout</button>
              <button style={styles.primaryButton} onClick={() => setIsEditing(true)}>Edit Profile</button>
            </>
          )}
        </div>

      </div>
    </div>
  );
}

const BUYER_COLOR = "#4a7c59";
const BROWN = "#5c4033";

const styles = {
  page: {
    minHeight: "100vh",
    backgroundColor: "#f5f5f0",
  },
  banner: {
    backgroundColor: BUYER_COLOR,
    padding: "40px 0",
  },
  bannerInner: {
    maxWidth: "900px",
    margin: "0 auto",
    padding: "0 48px",
    display: "flex",
    alignItems: "center",
    gap: "24px",
  },
  avatar: {
    width: "80px",
    height: "80px",
    borderRadius: "50%",
    backgroundColor: "rgba(255,255,255,0.2)",
    border: "3px solid rgba(255,255,255,0.5)",
    color: "white",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "28px",
    fontWeight: "bold",
    flexShrink: 0,
  },
  bannerName: {
    fontSize: "26px",
    fontWeight: "700",
    color: "white",
    margin: "0 0 4px 0",
  },
  bannerEmail: {
    fontSize: "14px",
    color: "rgba(255,255,255,0.75)",
    margin: "0 0 10px 0",
  },
  roleBadge: {
    display: "inline-block",
    backgroundColor: "rgba(255,255,255,0.2)",
    color: "white",
    padding: "3px 12px",
    borderRadius: "99px",
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.08em",
  },
  content: {
    maxWidth: "900px",
    margin: "0 auto",
    padding: "40px 48px",
  },
  error: {
    backgroundColor: "#fee",
    color: "#c00",
    padding: "12px 16px",
    borderRadius: "6px",
    marginBottom: "24px",
    fontSize: "14px",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "16px",
    marginBottom: "32px",
  },
  fieldCard: {
    backgroundColor: "white",
    borderRadius: "8px",
    padding: "20px 24px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
  },
  fieldLabel: {
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.07em",
    textTransform: "uppercase",
    color: BUYER_COLOR,
    margin: "0 0 8px 0",
  },
  fieldValue: {
    fontSize: "16px",
    color: "#222",
    margin: 0,
  },
  fieldValueEmpty: {
    fontSize: "15px",
    color: "#bbb",
    fontStyle: "italic",
    margin: 0,
  },
  fieldInput: {
    width: "100%",
    padding: "6px 0",
    border: "none",
    borderBottom: `2px solid ${BUYER_COLOR}`,
    backgroundColor: "transparent",
    fontSize: "16px",
    color: "#222",
    outline: "none",
    boxSizing: "border-box",
  },
  buttonRow: {
    display: "flex",
    justifyContent: "flex-end",
    gap: "12px",
  },
  primaryButton: {
    padding: "12px 28px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "15px",
    fontWeight: "600",
    cursor: "pointer",
  },
  secondaryButton: {
    padding: "12px 28px",
    backgroundColor: "white",
    color: BROWN,
    border: `2px solid ${BROWN}`,
    borderRadius: "6px",
    fontSize: "15px",
    fontWeight: "600",
    cursor: "pointer",
  },
};

export default BuyerDashboard;
