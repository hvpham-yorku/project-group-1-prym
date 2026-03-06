import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getBuyerProfile, updateBuyerProfile } from "../api/buyer";
import CowDiagram from "../components/CowDiagram";
import EditAccountModal from "../components/EditAccountModal";

// "Chuck, Rib x2, Sirloin"  →  { Chuck: 1, Rib: 2, Sirloin: 1 }
function parseCuts(str) {
  if (!str) return {};
  const result = {};
  str.split(", ").forEach((item) => {
    const match = item.match(/^(.+?) x(\d+)$/);
    if (match) result[match[1]] = parseInt(match[2], 10);
    else if (item.trim()) result[item.trim()] = 1;
  });
  return result;
}

// { Chuck: 1, Rib: 2 }  →  "Chuck, Rib x2"
function serializeCuts(cutsObj) {
  return Object.entries(cutsObj)
    .map(([cut, qty]) => (qty > 1 ? `${cut} x${qty}` : cut))
    .join(", ");
}

function BuyerDashboard() {
  const { user, clearUser, saveUser } = useAuth();
  const navigate = useNavigate();

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    phoneNumber: "",
    selectedCuts: {}, // { [cutId]: quantity }
    quantity: "",
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
        const data = await getBuyerProfile(user.id);
        setProfile(data);
        setFormData({
          phoneNumber: user?.phoneNumber || "",
          selectedCuts: parseCuts(data.preferredCuts),
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
        const phoneRegex =
          /^(\+?1[\s.\-]?)?(\(?\d{3}\)?[\s.\-]?)\d{3}[\s.\-]?\d{4}$/;
        if (!phoneRegex.test(formData.phoneNumber)) {
          setError("Please enter a valid phone number.");
          return;
        }
      }

      const preferredCuts = serializeCuts(formData.selectedCuts);

      await updateBuyerProfile(user.id, {
        preferredCuts,
        quantity: formData.quantity,
        phoneNumber: formData.phoneNumber,
      });

      const savedPhone = formData.phoneNumber || user?.phoneNumber;
      setProfile((prev) => ({
        ...prev,
        preferredCuts,
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

  // Toggle a cut on/off in edit mode
  const handleCutToggle = (id) => {
    setFormData((prev) => {
      const cuts = { ...prev.selectedCuts };
      if (id in cuts) delete cuts[id];
      else cuts[id] = 1;
      return { ...prev, selectedCuts: cuts };
    });
  };

  // +1 / -1 on a cut's quantity; deselects if qty would fall below 1
  const handleCutQty = (id, delta) => {
    setFormData((prev) => {
      const qty = (prev.selectedCuts[id] ?? 1) + delta;
      const cuts = { ...prev.selectedCuts };
      if (qty < 1) delete cuts[id];
      else if (qty <= 2) cuts[id] = qty;
      return { ...prev, selectedCuts: cuts };
    });
  };

  const handleDiscard = () => {
    setFormData({
      phoneNumber: user?.phoneNumber || "",
      selectedCuts: parseCuts(profile?.preferredCuts),
      quantity: profile?.quantity || "",
    });
    setError("");
    setIsEditing(false);
  };

  const initials =
    (user?.firstName?.charAt(0) || "") + (user?.lastName?.charAt(0) || "");

  // The cuts to show in the diagram — profile's saved cuts when viewing, form state when editing
  const displayCuts = isEditing
    ? formData.selectedCuts
    : parseCuts(profile?.preferredCuts);

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
          accentColor={BUYER_COLOR}
          onClose={() => setShowAccountModal(false)}
        />
      )}

      {/* Header Banner */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <button
            style={styles.backBtn}
            onClick={() => navigate("/farmlistings")}
          >
            ← Back
          </button>
          <div style={styles.avatar}>{initials}</div>
          <div>
            <h1 style={styles.bannerName}>
              {user?.firstName} {user?.lastName}
            </h1>
            <p style={styles.bannerEmail}>{user?.email}</p>
            <span style={styles.roleBadge}>BUYER</span>
            <button
              style={styles.editAccountBtn}
              onClick={() => setShowAccountModal(true)}
            >
              Edit Account
            </button>
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
                  user?.phoneNumber ? styles.fieldValue : styles.fieldValueEmpty
                }
              >
                {user?.phoneNumber || "Not set"}
              </p>
            )}
          </div>

          {/* Quantity */}
          <div style={styles.fieldCard}>
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
              <p
                style={
                  profile?.quantity ? styles.fieldValue : styles.fieldValueEmpty
                }
              >
                {profile?.quantity || "Not set"}
              </p>
            )}
          </div>

          {/* Preferred Cuts — cow diagram, full width */}
          <div
            style={{
              ...styles.fieldCard,
              gridColumn: "1 / -1",
              padding: "20px 16px 12px",
            }}
          >
            <p style={styles.fieldLabel}>
              Preferred Cuts
              {isEditing && (
                <span style={styles.editHint}>
                  — click a section to select, use − / + to set quantity
                </span>
              )}
            </p>

            {Object.keys(displayCuts).length === 0 && !isEditing ? (
              <p style={styles.fieldValueEmpty}>Not set</p>
            ) : (
              <>
                <CowDiagram
                  selectedCuts={displayCuts}
                  onToggle={handleCutToggle}
                  onQuantityChange={handleCutQty}
                  readOnly={!isEditing}
                />

                {/* Cut text summary */}
                {Object.keys(displayCuts).length > 0 && (
                  <div style={styles.cutsTextRow}>
                    <span style={styles.cutsLabel}>Selected cuts: </span>
                    {Object.entries(displayCuts).map(([cut, qty], i, arr) => (
                      <span key={cut}>
                        <span style={styles.cutName}>
                          {cut}
                          {qty > 1 ? ` ×${qty}` : ""}
                        </span>
                        {i < arr.length - 1 && (
                          <span style={styles.cutSep}>, </span>
                        )}
                      </span>
                    ))}
                  </div>
                )}
              </>
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
  backBtn: {
    background: "none",
    border: "2px solid rgba(255,255,255,0.6)",
    borderRadius: "6px",
    color: "white",
    fontSize: "14px",
    fontWeight: "600",
    padding: "6px 14px",
    cursor: "pointer",
    marginRight: "8px",
    flexShrink: 0,
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
    borderBottom: `2px solid ${BUYER_COLOR}`,
    backgroundColor: "transparent",
    fontSize: "16px",
    color: "#222",
    outline: "none",
    boxSizing: "border-box",
  },
  cutsTextRow: {
    margin: "14px 8px 4px",
    fontSize: "14px",
    lineHeight: 1.6,
  },
  cutsLabel: {
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.07em",
    textTransform: "uppercase",
    color: BUYER_COLOR,
    marginRight: "6px",
  },
  cutName: {
    fontWeight: "600",
    color: "#222",
    fontSize: "14px",
  },
  cutSep: {
    color: "#aaa",
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
  editAccountBtn: {
  background: "rgba(255,255,255,0.15)",
  border: "2px solid rgba(255,255,255,0.6)",
  borderRadius: "6px",
  color: "white",
  fontSize: "13px",
  fontWeight: "600",
  padding: "6px 14px",
  cursor: "pointer",
  marginTop: "10px",
  display: "inline-block",
},
};

export default BuyerDashboard;
