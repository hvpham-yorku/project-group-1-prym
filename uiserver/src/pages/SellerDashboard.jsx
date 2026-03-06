import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getSellerProfile, updateSellerProfile } from "../api/seller";
import EditAccountModal from "../components/EditAccountModal";

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
  const [showAccountModal, setShowAccountModal] = useState(false);

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
    setError("");
    try {
      if (formData.phoneNumber) {
        const phoneRegex =
          /^(\+?1[\s.\-]?)?(\(?\d{3}\)?[\s.\-]?)\d{3}[\s.\-]?\d{4}$/;
        if (!phoneRegex.test(formData.phoneNumber)) {
          setError("Please enter a valid phone number.");
          return;
        }
      }
      await updateSellerProfile(user.id, { ...formData });
      const savedPhone = formData.phoneNumber || profile?.phoneNumber;
      setProfile((prev) => ({
        ...prev,
        shopName: formData.shopName,
        phoneNumber: savedPhone,
        shopAddress: formData.shopAddress,
        category: formData.category,
        description: formData.description,
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
      shopName: profile?.shopName || "",
      phoneNumber: profile?.phoneNumber || "",
      shopAddress: profile?.shopAddress || "",
      category: profile?.category || "",
      description: profile?.description || "",
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
      {showAccountModal && (
        <EditAccountModal
          accentColor={SELLER_COLOR}
          onClose={() => setShowAccountModal(false)}
        />
      )}

      {/* Header Banner */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <div style={styles.avatarWrapper}>
            <div style={styles.avatar}>
              {user?.profilePicture ? (
                <img src={user.profilePicture} alt="Profile" style={{ width: "100%", height: "100%", borderRadius: "50%", objectFit: "cover" }} />
              ) : (
                initials
              )}
            </div>
            <button
              style={styles.editAccountBtn}
              onClick={() => setShowAccountModal(true)}
              title="Edit account"
            >
              ✎
            </button>
          </div>
          <div>
            <h1 style={styles.bannerName}>
              {user?.firstName} {user?.lastName}
            </h1>
            <p style={styles.bannerEmail}>{user?.email}</p>
            <span style={styles.roleBadge}>SELLER</span>
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
              <p
                style={
                  profile?.phoneNumber
                    ? styles.fieldValue
                    : styles.fieldValueEmpty
                }
              >
                {profile?.phoneNumber || "Not set"}
              </p>
            )}
          </div>

          {/* Shop Name */}
          <div style={styles.fieldCard}>
            <p style={styles.fieldLabel}>Shop Name</p>
            {isEditing ? (
              <input
                name="shopName"
                value={formData.shopName}
                onChange={handleChange}
                placeholder="Your shop name"
                style={styles.fieldInput}
              />
            ) : (
              <p
                style={
                  profile?.shopName ? styles.fieldValue : styles.fieldValueEmpty
                }
              >
                {profile?.shopName || "Not set"}
              </p>
            )}
          </div>

          {/* Address */}
          <div style={styles.fieldCard}>
            <p style={styles.fieldLabel}>Address</p>
            {isEditing ? (
              <input
                name="shopAddress"
                value={formData.shopAddress}
                onChange={handleChange}
                placeholder="Your shop address"
                style={styles.fieldInput}
              />
            ) : (
              <p
                style={
                  profile?.shopAddress
                    ? styles.fieldValue
                    : styles.fieldValueEmpty
                }
              >
                {profile?.shopAddress || "Not set"}
              </p>
            )}
          </div>

          {/* Category */}
          <div style={styles.fieldCard}>
            <p style={styles.fieldLabel}>Category</p>
            {isEditing ? (
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                style={styles.fieldInput}
              >
                <option value="">Select category</option>
                <option value="HALAL">Halal</option>
                <option value="KOSHER">Kosher</option>
                <option value="ORGANIC">Organic</option>
                <option value="CONVENTIONAL">Conventional</option>
              </select>
            ) : (
              <p
                style={
                  profile?.category ? styles.fieldValue : styles.fieldValueEmpty
                }
              >
                {profile?.category || "Not set"}
              </p>
            )}
          </div>

          {/* Description — full width */}
          <div style={{ ...styles.fieldCard, gridColumn: "1 / -1" }}>
            <p style={styles.fieldLabel}>Description</p>
            {isEditing ? (
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Tell buyers about your shop"
                rows={4}
                style={{ ...styles.fieldInput, resize: "vertical" }}
              />
            ) : (
              <p
                style={
                  profile?.description
                    ? styles.fieldValue
                    : styles.fieldValueEmpty
                }
              >
                {profile?.description || "Not set"}
              </p>
            )}
          </div>
        </div>

        {/* Buttons */}
        <div style={styles.buttonRow}>
          {isEditing ? (
            <>
              <button style={styles.secondaryButton} onClick={handleDiscard}>
                Discard
              </button>
              <button style={styles.primaryButton} onClick={handleSave}>
                Save Changes
              </button>
            </>
          ) : (
            <>
              <button style={styles.secondaryButton} onClick={handleLogout}>
                Logout
              </button>
              <button
                style={styles.primaryButton}
                onClick={() => setIsEditing(true)}
              >
                Edit Profile
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

const SELLER_COLOR = "#5c4033";
const GREEN = "#4a7c59";

const styles = {
  page: {
    minHeight: "100vh",
    backgroundColor: "#f5f5f0",
  },
  banner: {
    backgroundColor: SELLER_COLOR,
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
    color: SELLER_COLOR,
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
    borderBottom: `2px solid ${SELLER_COLOR}`,
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
    backgroundColor: GREEN,
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
    color: SELLER_COLOR,
    border: `2px solid ${SELLER_COLOR}`,
    borderRadius: "6px",
    fontSize: "15px",
    fontWeight: "600",
    cursor: "pointer",
  },
  editAccountBtn: {
    background: "rgba(255,255,255,0.15)",
    border: "2px solid rgba(255,255,255,0.5)",
    borderRadius: "50%",
    color: "white",
    fontSize: "14px",
    width: "28px",
    height: "28px",
    cursor: "pointer",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: 0,
  },
  avatarWrapper: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "8px",
    flexShrink: 0,
  },
};

export default SellerDashboard;
