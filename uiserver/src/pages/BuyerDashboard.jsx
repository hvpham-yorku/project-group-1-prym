import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getBuyerProfile, updateBuyerProfile } from "../api/buyer";
import { getAvailableGroups, getMyGroups, joinGroup } from "../api/groups";
import CowDiagram from "../components/CowDiagram";

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
    selectedCuts: {},   // { [cutId]: quantity }
    quantity: "",
  });
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [availableGroups, setAvailableGroups] = useState([]);
  const [myGroups, setMyGroups] = useState([]);
  const [groupsLoading, setGroupsLoading] = useState(true);
  const [groupsError, setGroupsError] = useState("");
  const [joiningGroupId, setJoiningGroupId] = useState(null);

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

  useEffect(() => {
    const fetchGroups = async () => {
      if (!user?.id) return;
      try {
        setGroupsLoading(true);
        const [available, mine] = await Promise.all([
          getAvailableGroups(user.id),
          getMyGroups(user.id),
        ]);
        setAvailableGroups(available);
        setMyGroups(mine);
      } catch (err) {
        setGroupsError("Failed to load groups.");
        console.error(err);
      } finally {
        setGroupsLoading(false);
      }
    };
    fetchGroups();
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
        const phoneRegex = /^(\+?1[\s.\-]?)?(\(?\d{3}\)?[\s.\-]?)\d{3}[\s.\-]?\d{4}$/;
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

  const handleJoinGroup = async (groupId) => {
    setJoiningGroupId(groupId);
    setGroupsError("");
    try {
      await joinGroup(user.id, groupId);
      navigate(`/buyer/groups/${groupId}`);
    } catch (err) {
      setGroupsError(err.message || "Failed to join group.");
      setJoiningGroupId(null);
    }
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
  const displayCuts = isEditing ? formData.selectedCuts : parseCuts(profile?.preferredCuts);

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
          <button style={styles.backBtn} onClick={() => navigate('/farmlistings')}>
            ← Back
          </button>
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
              <p style={profile?.quantity ? styles.fieldValue : styles.fieldValueEmpty}>
                {profile?.quantity || "Not set"}
              </p>
            )}
          </div>

          {/* Preferred Cuts — cow diagram, full width */}
          <div style={{ ...styles.fieldCard, gridColumn: "1 / -1", padding: "20px 16px 12px" }}>
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
                          {cut}{qty > 1 ? ` ×${qty}` : ''}
                        </span>
                        {i < arr.length - 1 && <span style={styles.cutSep}>,  </span>}
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

        {/* ── Groups Section ── */}
        <div style={styles.groupsSection}>
          <div style={styles.groupsHeader}>
            <h2 style={styles.sectionTitle}>Groups</h2>
            {myGroups.length === 0 && !groupsLoading && (
              <div style={{ display: "flex", gap: "10px" }}>
                <button style={styles.browseGroupBtn} onClick={() => navigate('/buyer/browse-groups')}>
                  Find a Group
                </button>
                <button style={styles.createGroupBtn} onClick={() => navigate('/buyer/create-group')}>
                  Start a Group
                </button>
              </div>
            )}
          </div>

          {groupsError && <div style={styles.error}>{groupsError}</div>}

          {/* ── My Groups ── */}
          <h3 style={styles.subSectionTitle}>My Groups</h3>
          {groupsLoading ? (
            <p style={styles.emptyText}>Loading your groups...</p>
          ) : myGroups.length === 0 ? (
            <p style={styles.emptyText}>You have not joined any groups yet.</p>
          ) : (
            <div style={styles.groupCardGrid}>
              {myGroups.map((g) => (
                <div key={g.groupId} style={{ ...styles.groupCard, cursor: 'pointer' }}
                  onClick={() => navigate(`/buyer/groups/${g.groupId}`)}>

                  <div style={styles.groupCardHeader}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                      <span style={{ fontSize: '10px', color: '#999', fontWeight: '600' }}>
                        #{g.groupId}
                      </span>
                      <span style={styles.cowNameText}>{g.groupName}</span>
                    </div>
                    <div style={styles.certBadges}>
                      {g.certifications.includes("KOSHER") && (
                        <span style={{ ...styles.badge, ...styles.badgeKosher }}>Kosher</span>
                      )}
                      {g.certifications.includes("HALAL") && (
                        <span style={{ ...styles.badge, ...styles.badgeHalal }}>Halal</span>
                      )}
                      {g.certifications.includes("ORGANIC") && (
                        <span style={{ ...styles.badge, ...styles.badgeOrganic }}>Organic</span>
                      )}
                      {g.certifications.includes("GRASS_FED") && (
                        <span style={{ ...styles.badge, ...styles.badgeGrassFed }}>Grass-Fed</span>
                      )}
                      {g.certifications.includes("NON_GMO") && (
                        <span style={{ ...styles.badge, ...styles.badgeNonGmo }}>Non-GMO</span>
                      )}
                    </div>
                  </div>
                  <div style={styles.membersSection}>
                    <span style={styles.membersLabel}>Members ({g.memberCount})</span>
                    {g.members.map((m, i) => (
                      <div key={i} style={styles.memberRow}>
                        <span style={styles.memberName}>{m.firstName}</span>
                        <span style={styles.memberCuts}>{m.claimedCuts || 'No cuts yet'}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* ── Available Groups — hidden once user has joined a group ── */}
          {myGroups.length === 0 && (
            <>
              <h3 style={{ ...styles.subSectionTitle, marginTop: "32px" }}>Available Groups</h3>
              {groupsLoading ? (
                <p style={styles.emptyText}>Loading available groups...</p>
              ) : availableGroups.length === 0 ? (
                <p style={styles.emptyText}>No groups available yet. Start one!</p>
              ) : (
                <div style={styles.groupCardGrid}>
                  {availableGroups.map((g) => (
                    <div
                      key={g.groupId}
                      style={{ ...styles.groupCard, cursor: 'pointer' }}
                      onClick={() => navigate(`/buyer/groups/${g.groupId}`)}
                    >
                      <div style={styles.groupCardHeader}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                          <span style={{ fontSize: '10px', color: '#999', fontWeight: '600' }}>
                            #{g.groupId}
                          </span>
                          <span style={styles.cowNameText}>{g.groupName}</span>
                        </div>
                        <div style={styles.certBadges}>
                          {g.certifications.includes("KOSHER") && (
                            <span style={{ ...styles.badge, ...styles.badgeKosher }}>Kosher</span>
                          )}
                          {g.certifications.includes("HALAL") && (
                            <span style={{ ...styles.badge, ...styles.badgeHalal }}>Halal</span>
                          )}
                          {g.certifications.includes("ORGANIC") && (
                            <span style={{ ...styles.badge, ...styles.badgeOrganic }}>Organic</span>
                          )}
                          {g.certifications.includes("GRASS_FED") && (
                            <span style={{ ...styles.badge, ...styles.badgeGrassFed }}>Grass-Fed</span>
                          )}
                          {g.certifications.includes("NON_GMO") && (
                            <span style={{ ...styles.badge, ...styles.badgeNonGmo }}>Non-GMO</span>
                          )}
                        </div>
                      </div>
                      <p style={styles.cardDetail}>
                        {g.memberCount === 0
                          ? "No members yet — be the first!"
                          : `${g.memberCount} member(s) so far`}
                      </p>
                      <button
                        style={{
                          ...styles.joinButton,
                          ...(joiningGroupId === g.groupId ? styles.joinButtonDisabled : {}),
                        }}
                        onClick={(e) => { e.stopPropagation(); handleJoinGroup(g.groupId); }}
                        disabled={joiningGroupId === g.groupId}
                      >
                        {joiningGroupId === g.groupId ? "Joining..." : "Join Group"}
                      </button>
                    </div>
                  ))}
                </div>
              )}
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
    background: 'none',
    border: '2px solid rgba(255,255,255,0.6)',
    borderRadius: '6px',
    color: 'white',
    fontSize: '14px',
    fontWeight: '600',
    padding: '6px 14px',
    cursor: 'pointer',
    marginRight: '8px',
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
  groupsSection: {
    marginTop: "48px",
    paddingTop: "32px",
    borderTop: "2px solid #e8e4e0",
  },
  groupsHeader: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: "20px",
  },
  browseGroupBtn: {
    padding: "9px 20px",
    backgroundColor: "white",
    color: BUYER_COLOR,
    border: `2px solid ${BUYER_COLOR}`,
    borderRadius: "99px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  createGroupBtn: {
    padding: "9px 20px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "99px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  sectionTitle: {
    fontSize: "22px",
    fontWeight: "700",
    color: BROWN,
    margin: 0,
  },
  subSectionTitle: {
    fontSize: "12px",
    fontWeight: "700",
    color: BUYER_COLOR,
    margin: "0 0 12px 0",
    textTransform: "uppercase",
    letterSpacing: "0.07em",
  },
  noCutsPrompt: {
    backgroundColor: "#fff8e1",
    border: "1px solid #ffe082",
    borderRadius: "8px",
    padding: "16px 20px",
    fontSize: "14px",
    color: "#795548",
  },
  emptyText: {
    fontSize: "14px",
    color: "#bbb",
    fontStyle: "italic",
    margin: 0,
  },
  groupCardGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
    gap: "16px",
  },
  groupCard: {
    backgroundColor: "white",
    borderRadius: "8px",
    padding: "20px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  groupCardHeader: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: "8px",
    flexWrap: "wrap",
  },
  cowNameText: {
    fontSize: "16px",
    fontWeight: "700",
    color: "#222",
  },
  certBadges: {
    display: "flex",
    gap: "6px",
    flexWrap: "wrap",
  },
  badge: {
    display: "inline-block",
    padding: "2px 8px",
    borderRadius: "99px",
    fontSize: "10px",
    fontWeight: "700",
    letterSpacing: "0.05em",
    textTransform: "uppercase",
  },
  badgeKosher:   { backgroundColor: "#e3f2fd", color: "#1565c0" },
  badgeHalal:    { backgroundColor: "#fff3e0", color: "#e65100" },
  badgeOrganic:  { backgroundColor: "#e8f5e9", color: "#2e7d32" },
  badgeGrassFed: { backgroundColor: "#f1f8e9", color: "#558b2f" },
  badgeNonGmo:   { backgroundColor: "#fce4ec", color: "#880e4f" },
  cardDetail: {
    fontSize: "13px",
    color: "#666",
    margin: 0,
  },
  membersSection: {
    marginTop: "4px",
  },
  membersLabel: {
    fontSize: "11px",
    fontWeight: "700",
    textTransform: "uppercase",
    letterSpacing: "0.07em",
    color: BUYER_COLOR,
    display: "block",
    marginBottom: "6px",
  },
  memberRow: {
    display: "flex",
    flexDirection: "column",
    gap: "1px",
    backgroundColor: "#f9f9f7",
    borderRadius: "4px",
    padding: "6px 10px",
    marginBottom: "6px",
    fontSize: "13px",
  },
  memberName: {
    fontWeight: "600",
    color: "#333",
  },
  memberCuts: {
    color: "#555",
  },
  memberPhone: {
    color: "#888",
    fontSize: "12px",
  },
  incompatibleNote: {
    fontSize: "12px",
    color: "#e65100",
    fontStyle: "italic",
    margin: 0,
  },
  joinButton: {
    marginTop: "8px",
    padding: "10px 20px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    alignSelf: "flex-start",
  },
  joinButtonDisabled: {
    opacity: 0.6,
    cursor: "not-allowed",
  },
};

export default BuyerDashboard;
