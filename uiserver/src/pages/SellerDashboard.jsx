import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getSellerProfile, updateSellerProfile, getCertifications, setCertifications } from "../api/seller";
import EditAccountModal from "../components/EditAccountModal";
import { generateRatingCode } from "../api/ratings";
import { ALL_CERTS } from "../constants/certifications";

function SellerDashboard() {
  const { user, clearUser, saveUser } = useAuth();
  const navigate = useNavigate();

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    shopName: "",
    shopAddress: "",
    description: "",
  });
  const [profile, setProfile] = useState(null);
  const [certList, setCertList] = useState([]);
  const [selectedCerts, setSelectedCerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [generatedCode, setGeneratedCode] = useState("");
  const [showCodeModal, setShowCodeModal] = useState(false);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        const [data, certs] = await Promise.all([
          getSellerProfile(user.id),
          getCertifications(user.id),
        ]);
        setProfile(data);
        setCertList(certs);
        setSelectedCerts(certs.map((c) => c.name));
        setFormData({
          shopName: data.shopName || "",
          shopAddress: data.shopAddress || "",
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
      await Promise.all([
        updateSellerProfile(user.id, { ...formData }),
        setCertifications(user.id, selectedCerts),
      ]);
      setProfile((prev) => ({...prev, ...formData}));
      setCertList(selectedCerts.map((name)=> ({name})));
      setIsEditing(false);
    } catch (err) {
      console.error("Save error:", err);
      setError("Failed to save profile.");
      //re-fetch to restore UI to actual server state
      try{
        const[data, certs] = await Promise.all([
          getSellerProfile(user.id),
          getCertifications(user.id),
        ]);
        setProfile(data);
        setCertList(certs);
        setSelectedCerts(certs.map((c) => c.name));
        setFormData({
          shopName: data.shopName || "",
          shopAddress: data.shopAddress || "",
          description: data.description || "",
        });
      } catch {
        //ignore the re-fetch failure
      }
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleDiscard = () => {
    setFormData({
      shopName: profile?.shopName || "",
      shopAddress: profile?.shopAddress || "",
      description: profile?.description || "",
    });
    setSelectedCerts(certList.map((c) => c.name));
    setError("");
    setIsEditing(false);
  };

  const toggleCert = (value) => {
    setSelectedCerts((prev) =>
      prev.includes(value) ? prev.filter((c) => c !== value) : [...prev, value]
    );
  };

  const handleGenerateCode = async () => {
    setError("");
    try {
      const result = await generateRatingCode(user.id);
      if (result.error) {
        setError(result.error);
      } else {
        setGeneratedCode(result.code);
        setShowCodeModal(true);
      }
    } catch (err) {
      setError("Failed to generate rating code.");
    }
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
                <img
                  src={user.profilePicture}
                  alt="Profile"
                  style={{ width: "100%", height: "100%", borderRadius: "50%", objectFit: "cover" }}
                />
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
            <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
              <h1 style={styles.bannerName}>{user?.firstName} {user?.lastName}</h1>
              <span style={styles.roleBadge}>SELLER</span>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "4px", marginTop: "4px" }}>
              <p style={styles.bannerEmail}>{user?.email}</p>
              <p style={styles.bannerEmail}>{user?.phoneNumber || "—"}</p>
              {user?.zipCode && (
                <p style={styles.bannerEmail}>
                  📍 {[user.city, user.state, user.country].filter(Boolean).join(", ") || user.zipCode}
                </p>
              )}
            </div>
          </div>

          <button style={styles.logoutBtn} onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

      {/* Content */}
      <div style={styles.content}>
        {error && <div style={styles.error}>{error}</div>}

        <div style={styles.mainLayout}>
          {/* Left panel: farm fields + buttons */}
          <div style={styles.leftPanel}>
            <div style={styles.grid}>

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
                  <p style={profile?.shopName ? styles.fieldValue : styles.fieldValueEmpty}>
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
                    placeholder="e.g. 100 Farm Rd, Toronto"
                    style={styles.fieldInput}
                  />
                ) : (
                  <p style={profile?.shopAddress ? styles.fieldValue : styles.fieldValueEmpty}>
                    {profile?.shopAddress || "Not set"}
                  </p>
                )}
              </div>

              {/* Certifications — full width */}
              <div style={{ ...styles.fieldCard, gridColumn: "1 / -1" }}>
                <p style={styles.fieldLabel}>
                  Certifications
                  {isEditing && (
                    <span style={styles.editHint}>— select all that apply</span>
                  )}
                </p>
                {isEditing ? (
                  <div style={styles.certGrid}>
                    {ALL_CERTS.map((cert) => (
                      <label key={cert.value} style={styles.certOption}>
                        <input
                          type="checkbox"
                          checked={selectedCerts.includes(cert.value)}
                          onChange={() => toggleCert(cert.value)}
                          style={styles.checkbox}
                        />
                        {cert.label}
                      </label>
                    ))}
                  </div>
                ) : certList.length > 0 ? (
                  <div style={styles.certTagRow}>
                    {certList.map((c) => (
                      <span key={c.name} style={styles.certTag}>
                        {ALL_CERTS.find((x) => x.value === c.name)?.label || c.name}
                      </span>
                    ))}
                  </div>
                ) : (
                  <p style={styles.fieldValueEmpty}>Not set</p>
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
                    placeholder="Tell buyers about your farm, cattle, and practices"
                    rows={4}
                    style={{ ...styles.fieldInput, resize: "vertical" }}
                  />
                ) : (
                  <p style={profile?.description ? styles.fieldValue : styles.fieldValueEmpty}>
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
                  <button style={styles.primaryButton} onClick={() => setIsEditing(true)}>
                    Edit Profile
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Right panel: rating code */}
          <div style={styles.rightPanel}>
            <div style={styles.fieldCard}>
              <p style={styles.fieldLabel}>Buyer Ratings</p>
              <p style={styles.ratingHint}>
                Generate a one-time code to share with a buyer so they can rate your farm.
              </p>
              <button style={styles.primaryButton} onClick={handleGenerateCode}>
                Generate Rating Code
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Rating Code Modal */}
      {showCodeModal && (
        <div style={styles.modalOverlay}>
          <div style={styles.codeModal}>
            <h2 style={styles.modalTitle}>Rating Code</h2>
            <p style={styles.modalSubtitle}>
              Share this code with your buyer so they can rate your farm
            </p>
            <div style={styles.codeDisplay}>{generatedCode}</div>
            <button style={styles.primaryButton} onClick={() => setShowCodeModal(false)}>
              Done
            </button>
          </div>
        </div>
      )}
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
    maxWidth: "1200px",
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
  avatarWrapper: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "8px",
    flexShrink: 0,
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
  bannerName: {
    fontSize: "26px",
    fontWeight: "700",
    color: "white",
    margin: "0 0 4px 0",
  },
  bannerEmail: {
    fontSize: "14px",
    color: "rgba(255,255,255,0.75)",
    margin: 0,
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
    maxWidth: "1200px",
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
  mainLayout: {
    display: "flex",
    gap: "32px",
    alignItems: "flex-start",
  },
  leftPanel: {
    flex: "3 1 0",
    minWidth: 0,
  },
  rightPanel: {
    flex: "1 1 0",
    minWidth: "220px",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "16px",
    marginBottom: "0",
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
  editHint: {
    fontSize: "10px",
    fontWeight: "500",
    letterSpacing: "0.02em",
    textTransform: "none",
    color: "#999",
    marginLeft: "4px",
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
  certGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(4, 1fr)",
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
    accentColor: GREEN,
    width: "15px",
    height: "15px",
    cursor: "pointer",
  },
  certTagRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "8px",
    marginTop: "4px",
  },
  certTag: {
    backgroundColor: "#eaf3ee",
    color: GREEN,
    border: `1px solid ${GREEN}`,
    borderRadius: "99px",
    padding: "3px 12px",
    fontSize: "13px",
    fontWeight: "600",
  },
  ratingHint: {
    fontSize: "13px",
    color: "#666",
    margin: "0 0 16px 0",
    lineHeight: 1.5,
  },
  logoutBtn: {
    marginLeft: "auto",
    background: "none",
    border: "2px solid rgba(255,255,255,0.6)",
    borderRadius: "6px",
    color: "white",
    fontSize: "14px",
    fontWeight: "600",
    padding: "6px 16px",
    cursor: "pointer",
  },
  buttonRow: {
    display: "flex",
    justifyContent: "flex-end",
    gap: "12px",
    marginTop: "16px",
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
  modalOverlay: {
    position: "fixed",
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: "rgba(0,0,0,0.5)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
  },
  codeModal: {
    backgroundColor: "white",
    borderRadius: "12px",
    padding: "36px",
    width: "360px",
    boxShadow: "0 8px 32px rgba(0,0,0,0.2)",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "16px",
  },
  modalTitle: {
    fontSize: "22px",
    fontWeight: "700",
    color: SELLER_COLOR,
    margin: 0,
  },
  modalSubtitle: {
    fontSize: "14px",
    color: "#666",
    margin: 0,
    textAlign: "center",
  },
  codeDisplay: {
    fontSize: "28px",
    fontWeight: "700",
    letterSpacing: "0.15em",
    color: SELLER_COLOR,
    backgroundColor: "#f5f5f0",
    padding: "16px 32px",
    borderRadius: "8px",
    border: `2px dashed ${SELLER_COLOR}`,
  },
};

export default SellerDashboard;
