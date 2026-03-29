import { useAuth } from "../context/AuthContext";
import { Link, useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect } from "react";
import { getBuyerProfile, updateBuyerProfile } from "../api/buyer";
import { getAvailableGroups, getMyGroups, joinGroup, getGroupByCode } from "../api/groups";
import { submitRating } from "../api/ratings";
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

//Main buyer dashboard / profile page. Shows user info in a banner,
//the cow diagram for preferred cuts, and the groups panel on the right.
//Pretty much the hub for everything a buyer does after logging in.
function BuyerDashboard() {
  const { user, clearUser, saveUser } = useAuth();
  const navigate = useNavigate();

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    phoneNumber: "",
    selectedCuts: {}, // { [cutId]: quantity }
  });
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showAccountModal, setShowAccountModal] = useState(false);

  const [availableGroups, setAvailableGroups] = useState([]);
  const [myGroups, setMyGroups] = useState([]);
  const [groupsLoading, setGroupsLoading] = useState(true);
  const [groupsError, setGroupsError] = useState("");
  const [joiningGroupId, setJoiningGroupId] = useState(null);
  const [inviteCode, setInviteCode] = useState("");
  const [codeError, setCodeError] = useState("");
  const [availPage, setAvailPage] = useState(1);
  const GROUPS_PAGE_SIZE = 6;

  const [showRatingModal, setShowRatingModal] = useState(false);
  const [ratingCode, setRatingCode] = useState("");
  const [ratingScore, setRatingScore] = useState(0);
  const [hoveredStar, setHoveredStar] = useState(0);
  const [ratingError, setRatingError] = useState("");
  const [ratingSuccess, setRatingSuccess] = useState("");
  const [ratingSubmitting, setRatingSubmitting] = useState(false);

  //loads the buyer profile on mount so we can show their preferred cuts
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

  //fetches both available groups and the user's own groups in parallel
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

  //saves the edited profile — validates phone number then sends
  //the preferred cuts and phone to the backend
  const handleSave = async () => {
    setError("");
    try {
      if (formData.phoneNumber) {
        const phoneRegex =
          /^(\+?1[\s.-]?)?(\(?\d{3}\)?[\s.-]?)\d{3}[\s.-]?\d{4}$/;
        if (!phoneRegex.test(formData.phoneNumber)) {
          setError("Please enter a valid phone number.");
          return;
        }
      }

      const preferredCuts = serializeCuts(formData.selectedCuts);

      await updateBuyerProfile(user.id, {
        preferredCuts,
        phoneNumber: formData.phoneNumber,
      });

      const savedPhone = formData.phoneNumber || user?.phoneNumber;
      setProfile((prev) => ({
        ...prev,
        preferredCuts,
      }));
      setFormData((prev) => ({ ...prev, phoneNumber: savedPhone }));
      saveUser({ ...user, phoneNumber: savedPhone });
      setIsEditing(false);
    } catch (err) {
      console.error("Save error:", err);
      setError("Failed to save profile.");
    }
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

  //joins a group directly from the available groups list then navigates there
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

  //looks up a group by its invite code and navigates to it
  const handleJoinByCode = async () => {
    if (!inviteCode.trim()) return;
    setCodeError("");
    try {
      const group = await getGroupByCode(user.id, inviteCode.trim());
      navigate(`/buyer/groups/${group.groupId}`);
    } catch (err) {
      setCodeError(err.message || "Invalid invite code.");
    }
  };

  const handleSubmitRating = async () => {
    setRatingError("");
    setRatingSuccess("");
    if (!ratingCode.trim()) { setRatingError("Please enter the code from the seller."); return; }
    if (ratingScore === 0) { setRatingError("Please select a star rating."); return; }
    setRatingSubmitting(true);
    try {
      const result = await submitRating(user.id, ratingCode.trim(), ratingScore);
      if (result.error) {
        setRatingError(result.error);
      } else {
        setRatingSuccess("Rating submitted successfully.");
        setTimeout(() => {
          setShowRatingModal(false);
          setRatingCode("");
          setRatingScore(0);
          setRatingSuccess("");
        }, 1500);
      }
    } catch {
      setRatingError("Something went wrong. Please try again.");
    } finally {
      setRatingSubmitting(false);
    }
  };

  //throws away any unsaved edits and resets form back to what's in the profile
  const handleDiscard = () => {
    setFormData({
      phoneNumber: user?.phoneNumber || "",
      selectedCuts: parseCuts(profile?.preferredCuts),
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
            <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
              <h1 style={styles.bannerName}>
                {user?.firstName} {user?.lastName}
              </h1>
              <span style={styles.roleBadge}>BUYER</span>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "4px", marginTop: "4px" }}>
              <p style={styles.bannerEmail}>{user?.email}</p>
              <p style={styles.bannerEmail}>
                {user?.phoneNumber || "—"}
              </p>
              {(user?.zipCode) && (
                <p style={styles.bannerEmail}>
                  📍 {[user.city, user.state, user.country].filter(Boolean).join(", ") || user.zipCode}
                </p>
              )}
            </div>
          </div>

          <div style={{ marginLeft: "auto", display: "flex", gap: "10px" }}>
            <button
              style={{
                ...styles.bannerBtn,
                ...(isEditing ? styles.joinButtonDisabled : {}),
              }}
              onClick={() => setIsEditing(true)}
              disabled={isEditing}
            >
              Edit Profile
            </button>
            <button style={styles.bannerBtn} onClick={handleLogout}>
              Logout
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <div style={styles.content}>
        {error && <div style={styles.error}>{error}</div>}

        {/* Location Prompt Banner */}
        {user && !user.zipCode && (
          <div style={styles.locationPrompt}>
            <div style={styles.promptIcon}>📍</div>
            <div style={styles.promptText}>
              <strong>Add your location to see nearby farms</strong>
              <p style={{ margin: '4px 0 0 0', fontSize: '13px' }}>
                We've added location features! Enter your ZIP code to discover local farms.
              </p>
            </div>
            <button
              onClick={() => setShowAccountModal(true)}
              style={styles.promptButton}
            >
              Add ZIP Code
            </button>
          </div>
        )}

        {/* Two-column layout: profile on left, groups on right */}
        <div style={styles.mainLayout}>

          {/* Left panel: profile fields + buttons */}
          <div style={styles.leftPanel}>
            <div style={styles.grid}>
              {/* Preferred Cuts — cow diagram, full width */}
              <div
                style={{
                  ...styles.fieldCard,
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
                  <Link to={`/buyer/farmlistings`}><button style={styles.primaryButton}>View Farm Listings</button></Link>
                  <button style={styles.primaryButton} onClick={() => setShowRatingModal(true)}>⭐ Rate a Farm</button>
                </>
              )}
            </div>
          </div>

          {/* Right panel: groups */}
          <div style={styles.rightPanel}>
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

          {/* ── Join by invite code ── */}
          {myGroups.length === 0 && !groupsLoading && (
            <div style={styles.codeRow}>
              <input
                style={styles.codeInput}
                placeholder="Enter invite code (e.g. X7K2AB3F)"
                value={inviteCode}
                onChange={(e) => { setInviteCode(e.target.value.toUpperCase()); setCodeError(""); }}
                onKeyDown={(e) => e.key === "Enter" && handleJoinByCode()}
                maxLength={8}
              />
              <button style={styles.codeBtn} onClick={handleJoinByCode}>
                Go
              </button>
            </div>
          )}
          {codeError && <p style={styles.codeError}>{codeError}</p>}

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
              ) : (() => {
                const availTotalPages = Math.max(1, Math.ceil(availableGroups.length / GROUPS_PAGE_SIZE));
                const pagedGroups = availableGroups.slice((availPage - 1) * GROUPS_PAGE_SIZE, availPage * GROUPS_PAGE_SIZE);
                return (<>
                <div style={styles.groupCardGrid}>
                  {pagedGroups.map((g) => (
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
                {availTotalPages > 1 && (
                  <div style={styles.pagination}>
                    <button style={{...styles.pageBtn, opacity: availPage === 1 ? 0.4 : 1}} onClick={() => setAvailPage(p => p - 1)} disabled={availPage === 1}>← Prev</button>
                    <span style={styles.pageInfo}>Page {availPage} of {availTotalPages}</span>
                    <button style={{...styles.pageBtn, opacity: availPage === availTotalPages ? 0.4 : 1}} onClick={() => setAvailPage(p => p + 1)} disabled={availPage === availTotalPages}>Next →</button>
                  </div>
                )}
                </>);
              })()}
            </>
          )}
        </div>
          </div>
        </div>
      </div>

      {showRatingModal && (
        <div style={styles.modalOverlay}>
          <div style={styles.ratingModal}>
            <h2 style={styles.modalTitle}>Rate a Farm</h2>
            <p style={styles.modalSubtitle}>Enter the code you received from the seller</p>
            <input
              style={styles.ratingCodeInput}
              placeholder="Enter seller code (e.g. PRYM-ABC123)"
              value={ratingCode}
              onChange={(e) => setRatingCode(e.target.value.toUpperCase())}
            />
            <div style={styles.starRow}>
              {[1, 2, 3, 4, 5].map((star) => (
                <span
                  key={star}
                  style={{ fontSize: "40px", cursor: "pointer", color: star <= (hoveredStar || ratingScore) ? "#f5a623" : "#ccc", transition: "color 0.1s" }}
                  onMouseEnter={() => setHoveredStar(star)}
                  onMouseLeave={() => setHoveredStar(0)}
                  onClick={() => setRatingScore(star)}
                >★</span>
              ))}
            </div>
            <p style={styles.scoreLabel}>
              {ratingScore > 0 ? `You selected: ${ratingScore} star${ratingScore !== 1 ? "s" : ""}` : "Select a rating"}
            </p>
            {ratingError && <p style={styles.modalError}>{ratingError}</p>}
            {ratingSuccess && <p style={styles.modalSuccess}>{ratingSuccess}</p>}
            <div style={styles.modalButtons}>
              <button
                style={styles.ratingCancelBtn}
                onClick={() => { setShowRatingModal(false); setRatingCode(""); setRatingScore(0); setRatingError(""); }}
              >Cancel</button>
              <button
                style={{ ...styles.ratingSubmitBtn, ...(ratingSubmitting ? { opacity: 0.6, cursor: "not-allowed" } : {}) }}
                onClick={handleSubmitRating}
                disabled={ratingSubmitting}
              >{ratingSubmitting ? "Submitting..." : "Submit Rating"}</button>
            </div>
          </div>
        </div>
      )}
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
    maxWidth: "1600px",
    margin: "0 auto",
    padding: "0 48px",
    display: "flex",
    alignItems: "center",
    gap: "24px",
  },
  bannerBtn: {
    background: "none",
    border: "2px solid rgba(255,255,255,0.6)",
    borderRadius: "6px",
    color: "white",
    fontSize: "14px",
    fontWeight: "600",
    padding: "6px 16px",
    cursor: "pointer",
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
    margin: 0,
  },
  bannerPhoneInput: {
    background: "rgba(255,255,255,0.15)",
    border: "1px solid rgba(255,255,255,0.5)",
    borderRadius: "4px",
    color: "white",
    fontSize: "14px",
    padding: "2px 8px",
    outline: "none",
    width: "160px",
  },
  bannerPhoneSaveBtn: {
    background: "rgba(255,255,255,0.2)",
    border: "1px solid rgba(255,255,255,0.5)",
    borderRadius: "4px",
    color: "white",
    cursor: "pointer",
    fontSize: "13px",
    padding: "2px 6px",
  },
  bannerPhoneCancelBtn: {
    background: "none",
    border: "1px solid rgba(255,255,255,0.3)",
    borderRadius: "4px",
    color: "rgba(255,255,255,0.7)",
    cursor: "pointer",
    fontSize: "13px",
    padding: "2px 6px",
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
    maxWidth: "1600px",
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
    gridTemplateColumns: "1fr",
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
    flex: "2 1 0",
    minWidth: 0,
  },
  buttonRow: {
    display: "flex",
    justifyContent: "flex-end",
    gap: "12px",
    marginTop: "16px",
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
  codeRow: {
    display: "flex",
    gap: "8px",
    margin: "12px 0 4px",
  },
  codeInput: {
    flex: 1,
    padding: "8px 12px",
    border: `1px solid #ccc`,
    borderRadius: "6px",
    fontSize: "14px",
    fontFamily: "monospace",
    letterSpacing: "0.1em",
    textTransform: "uppercase",
    outline: "none",
  },
  codeBtn: {
    padding: "8px 18px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "700",
    cursor: "pointer",
  },
  codeError: {
    fontSize: "13px",
    color: "#c00",
    margin: "0 0 8px 0",
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
  pagination: {
    display: "flex", alignItems: "center", justifyContent: "center",
    gap: "16px", marginTop: "16px",
  },
  pageBtn: {
    padding: "8px 20px", backgroundColor: BUYER_COLOR, color: "white",
    border: "none", borderRadius: "8px", fontSize: "13px", fontWeight: "600", cursor: "pointer",
  },
  pageInfo: { fontSize: "13px", color: "#444" },
  modalOverlay: {
    position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: "rgba(0,0,0,0.5)", display: "flex",
    alignItems: "center", justifyContent: "center", zIndex: 1000,
  },
  ratingModal: {
    backgroundColor: "white", borderRadius: "12px", padding: "36px",
    width: "400px", boxShadow: "0 8px 32px rgba(0,0,0,0.2)",
    display: "flex", flexDirection: "column", alignItems: "center", gap: "16px",
  },
  modalTitle: { fontSize: "22px", fontWeight: "700", color: BROWN, margin: 0 },
  modalSubtitle: { fontSize: "14px", color: "#666", margin: 0, textAlign: "center" },
  ratingCodeInput: {
    width: "100%", padding: "10px 14px", border: "2px solid #ddd",
    borderRadius: "6px", fontSize: "15px", boxSizing: "border-box",
    letterSpacing: "0.05em", outline: "none",
  },
  starRow: { display: "flex", gap: "8px" },
  scoreLabel: { fontSize: "14px", color: "#555", margin: 0 },
  modalError: { color: "#c00", fontSize: "14px", margin: 0 },
  modalSuccess: { color: BUYER_COLOR, fontSize: "14px", fontWeight: "600", margin: 0 },
  modalButtons: { display: "flex", gap: "12px", width: "100%" },
  ratingCancelBtn: {
    flex: 1, padding: "10px", backgroundColor: "white", color: BROWN,
    border: `2px solid ${BROWN}`, borderRadius: "6px", fontSize: "15px",
    fontWeight: "600", cursor: "pointer",
  },
  ratingSubmitBtn: {
    flex: 1, padding: "10px", backgroundColor: BUYER_COLOR, color: "white",
    border: "none", borderRadius: "6px", fontSize: "15px", fontWeight: "600", cursor: "pointer",
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
  locationPrompt: {
    display: "flex",
    alignItems: "center",
    gap: "15px",
    padding: "15px 20px",
    backgroundColor: "#fff3cd",
    border: "1px solid #ffc107",
    borderRadius: "8px",
    marginBottom: "20px",
  },
  promptIcon: {
    fontSize: "32px",
  },
  promptText: {
    flex: 1,
  },
  promptButton: {
    padding: "8px 16px",
    backgroundColor: "#28a745",
    color: "white",
    border: "none",
    borderRadius: "5px",
    cursor: "pointer",
    fontWeight: "600",
    fontSize: "14px",
  },
};

export default BuyerDashboard;
