import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { logout } from "../api/auth";
import { useState, useEffect, useRef } from "react";
import { getSellerProfile, updateSellerProfile, getCertifications, setCertifications, getSellerCowTypes, addCowType, deleteCowType } from "../api/seller";
import { getSellerPendingRequests, getSellerAssociations, respondToAssociation, respondToDisassociation, getAssociationMessages } from "../api/association";
import EditAccountModal from "../components/EditAccountModal";
import { generateRatingCode } from "../api/ratings";
import { ALL_CERTS } from "../constants/certifications";
import { Client } from "@stomp/stompjs";

function SellerDashboard() {
  const { user, clearUser } = useAuth();
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
  const [cowTypes, setCowTypes] = useState([]);
  const [newCow, setNewCow] = useState({ breed: "ANGUS", description: "", pricePerPound: "", availableCount: "" });
  const [showAddCow, setShowAddCow] = useState(false);

  // Association state
  const [pendingRequests, setPendingRequests] = useState([]);
  const [activeAssociations, setActiveAssociations] = useState([]);
  const [assocError, setAssocError] = useState("");
  const [denyNotes, setDenyNotes] = useState({}); // associationId → note string
  const stompClientRef = useRef(null);
  const activeAssociationsRef = useRef([]);
  const subscribedGroupsRef = useRef(new Set());

  // Chat state — keyed by groupId
  const [chatMessages, setChatMessages] = useState({});   // { [groupId]: [...] }
  const [chatInputs, setChatInputs] = useState({});       // { [groupId]: "" }
  const chatRefs = useRef({});                            // { [groupId]: scrollRef }

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        const [data, certs, cows, pending, active] = await Promise.all([
          getSellerProfile(user.id),
          getCertifications(user.id),
          getSellerCowTypes(user.id),
          getSellerPendingRequests(user.id),
          getSellerAssociations(user.id),
        ]);
        setProfile(data);
        setCertList(certs);
        setSelectedCerts(certs.map((c) => c.name));
        setCowTypes(cows);
        setPendingRequests(pending);
        setActiveAssociations(active);
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

  // Fetch message history whenever active associations change
  useEffect(() => {
    if (!user?.id || activeAssociations.length === 0) return;
    activeAssociations.forEach((assoc) => {
      getAssociationMessages(user.id, assoc.associationId)
        .then(({ groupId, messages }) => {
          setChatMessages((prev) => ({ ...prev, [groupId]: messages }));
        })
        .catch(() => {});
    });
  }, [activeAssociations, user?.id]);

  // Auto-scroll each chat container when new messages arrive
  useEffect(() => {
    Object.entries(chatRefs.current).forEach(([, ref]) => {
      if (ref) ref.scrollTop = ref.scrollHeight;
    });
  }, [chatMessages]);

  // Keep ref in sync so onConnect can read current associations without a dependency
  useEffect(() => {
    activeAssociationsRef.current = activeAssociations;
  }, [activeAssociations]);

  // Subscribe to any newly added group chat channels without reconnecting
  useEffect(() => {
    const client = stompClientRef.current;
    if (!client?.connected) return;
    activeAssociations.forEach((assoc) => {
      if (!subscribedGroupsRef.current.has(assoc.groupId)) {
        subscribedGroupsRef.current.add(assoc.groupId);
        client.subscribe(`/topic/group/${assoc.groupId}`, (frame) => {
          const msg = JSON.parse(frame.body);
          setChatMessages((prev) => ({
            ...prev,
            [assoc.groupId]: [...(prev[assoc.groupId] || []), msg],
          }));
        });
      }
    });
  }, [activeAssociations]);

  // WebSocket connection — only reconnects when the seller profile changes
  useEffect(() => {
    if (!profile?.id) return;
    const wsProtocol = window.location.protocol === "https:" ? "wss" : "ws";
    const client = new Client({
      brokerURL: `${wsProtocol}://${window.location.host}/ws/websocket`,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/seller/${profile.id}/notifications`, () => {
          Promise.all([
            getSellerPendingRequests(user.id),
            getSellerAssociations(user.id),
          ]).then(([pending, active]) => {
            setPendingRequests(pending);
            setActiveAssociations(active);
          }).catch(() => {});
        });
        // Subscribe to groups that were already active at connect time
        activeAssociationsRef.current.forEach((assoc) => {
          if (!subscribedGroupsRef.current.has(assoc.groupId)) {
            subscribedGroupsRef.current.add(assoc.groupId);
            client.subscribe(`/topic/group/${assoc.groupId}`, (frame) => {
              const msg = JSON.parse(frame.body);
              setChatMessages((prev) => ({
                ...prev,
                [assoc.groupId]: [...(prev[assoc.groupId] || []), msg],
              }));
            });
          }
        });
      },
    });
    client.activate();
    stompClientRef.current = client;
    const subscribedGroups = subscribedGroupsRef.current;
    return () => {
      client.deactivate();
      subscribedGroups.clear();
    };
  }, [profile?.id, user?.id]);

  const handleSendMessage = (groupId) => {
    const content = (chatInputs[groupId] || "").trim();
    if (!content || !stompClientRef.current?.connected) return;
    stompClientRef.current.publish({
      destination: `/app/chat/${groupId}`,
      body: JSON.stringify({ content }),
    });
    setChatInputs((prev) => ({ ...prev, [groupId]: "" }));
  };

  const handleRespondAssociation = async (associationId, action) => {
    setAssocError("");
    try {
      await respondToAssociation(user.id, associationId, action, denyNotes[associationId] || "");
      const [pending, active] = await Promise.all([
        getSellerPendingRequests(user.id),
        getSellerAssociations(user.id),
      ]);
      setPendingRequests(pending);
      setActiveAssociations(active);
      setDenyNotes((prev) => { const n = { ...prev }; delete n[associationId]; return n; });
    } catch (err) {
      setAssocError(err.message || "Failed to respond.");
    }
  };

  const handleRespondDisassociation = async (associationId, action) => {
    setAssocError("");
    try {
      await respondToDisassociation(user.id, associationId, action);
      const [pending, active] = await Promise.all([
        getSellerPendingRequests(user.id),
        getSellerAssociations(user.id),
      ]);
      setPendingRequests(pending);
      setActiveAssociations(active);
    } catch (err) {
      setAssocError(err.message || "Failed to respond.");
    }
  };

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

  const handleAddCow = async () => {
    setError("");
    try {
      const created = await addCowType(user.id, {
        breed: newCow.breed,
        description: newCow.description,
        pricePerPound: parseFloat(newCow.pricePerPound),
        availableCount: parseInt(newCow.availableCount),
      });
      setCowTypes((prev) => [...prev, created]);
      setNewCow({ breed: "ANGUS", description: "", pricePerPound: "", availableCount: "" });
      setShowAddCow(false);
    } catch {
      setError("Failed to add cattle type.");
    }
  };

  const handleDeleteCow = async (cowTypeId) => {
    setError("");
    try {
      await deleteCowType(user.id, cowTypeId);
      setCowTypes((prev) => prev.filter((c) => c.id !== cowTypeId));
    } catch {
      setError("Failed to remove cattle type.");
    }
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
    } catch {
      setError("Failed to generate rating code.");
    }
  };

  const [activeTab, setActiveTab] = useState("profile");

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

      {/* Tab Bar */}
      <div style={styles.tabBar}>
        <div style={styles.tabBarInner}>
          {[
            { key: "profile", label: "Farm Profile" },
            { key: "cattle", label: "Cattle" },
            { key: "groups", label: "Group Requests", badge: pendingRequests.length },
          ].map((tab) => (
            <button
              key={tab.key}
              style={{ ...styles.tabBtn, ...(activeTab === tab.key ? styles.tabBtnActive : {}) }}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
              {tab.badge > 0 && <span style={styles.tabBadge}>{tab.badge}</span>}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div style={styles.content}>
        {error && <div style={styles.error}>{error}</div>}

        {activeTab === "profile" && <div style={styles.mainLayout}>
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
        </div>}

        {activeTab === "cattle" && <div>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "16px" }}>
            <h2 style={styles.sectionHeading}>Your Cattle</h2>
            <button style={styles.primaryButton} onClick={() => setShowAddCow((v) => !v)}>
              {showAddCow ? "Cancel" : "+ Add Cattle"}
            </button>
          </div>

          {showAddCow && (
            <div style={{ ...styles.fieldCard, marginBottom: "16px" }}>
              <p style={styles.fieldLabel}>New Cattle Type</p>
              <div style={styles.cowFormGrid}>
                <div>
                  <p style={styles.cowFormLabel}>Breed</p>
                  <select
                    value={newCow.breed}
                    onChange={(e) => setNewCow((p) => ({ ...p, breed: e.target.value }))}
                    style={styles.cowSelect}
                  >
                    {["WAGYU","ANGUS","GRASS_FED","HERITAGE","CONVENTIONAL"].map((b) => (
                      <option key={b} value={b}>{b.replace("_", " ")}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <p style={styles.cowFormLabel}>Price per lb ($)</p>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={newCow.pricePerPound}
                    onChange={(e) => setNewCow((p) => ({ ...p, pricePerPound: e.target.value }))}
                    placeholder="e.g. 8.50"
                    style={styles.fieldInput}
                  />
                </div>
                <div>
                  <p style={styles.cowFormLabel}>Available Count</p>
                  <input
                    type="number"
                    min="0"
                    value={newCow.availableCount}
                    onChange={(e) => setNewCow((p) => ({ ...p, availableCount: e.target.value }))}
                    placeholder="e.g. 10"
                    style={styles.fieldInput}
                  />
                </div>
                <div style={{ gridColumn: "1 / -1" }}>
                  <p style={styles.cowFormLabel}>Description</p>
                  <input
                    type="text"
                    value={newCow.description}
                    onChange={(e) => setNewCow((p) => ({ ...p, description: e.target.value }))}
                    placeholder="e.g. Pasture-raised, hormone-free"
                    style={styles.fieldInput}
                  />
                </div>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "16px" }}>
                <button style={styles.primaryButton} onClick={handleAddCow}>Save Cattle</button>
              </div>
            </div>
          )}

          {cowTypes.length === 0 ? (
            <p style={{ color: "#bbb", fontStyle: "italic" }}>No cattle listed yet. Add one above.</p>
          ) : (
            <div style={styles.cowGrid}>
              {cowTypes.map((ct) => (
                <div key={ct.id} style={styles.cowCard}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                    <span style={styles.cowBreed}>{ct.breed.replace("_", " ")}</span>
                    <button style={styles.removeCowBtn} onClick={() => handleDeleteCow(ct.id)}>✕</button>
                  </div>
                  <p style={styles.cowDesc}>{ct.description || "—"}</p>
                  <div style={{ display: "flex", justifyContent: "space-between", marginTop: "auto" }}>
                    <span style={styles.cowPrice}>${ct.pricePerPound?.toFixed(2)}/lb</span>
                    <span style={styles.cowAvail}>{ct.availableCount} available</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>}

        {activeTab === "groups" && <div>
          <h2 style={styles.sectionHeading}>Group Requests</h2>
          {assocError && <div style={styles.assocError}>{assocError}</div>}

          {/* Pending association requests */}
          {pendingRequests.length > 0 && (
            <div style={{ marginBottom: "24px" }}>
              <p style={styles.assocSubheading}>Pending Requests ({pendingRequests.length})</p>
              <div style={styles.assocList}>
                {pendingRequests.map((req) => (
                  <div key={req.associationId} style={styles.assocCard}>
                    <div style={styles.assocCardHeader}>
                      <span style={styles.assocGroupName}>{req.groupName}</span>
                      <span style={styles.assocMemberCount}>{req.memberCount} member{req.memberCount !== 1 ? "s" : ""}</span>
                    </div>
                    {req.certifications?.length > 0 && (
                      <div style={styles.assocCerts}>
                        {req.certifications.map((c) => (
                          <span key={c} style={styles.assocCertTag}>{c}</span>
                        ))}
                      </div>
                    )}
                    <div style={styles.assocMembers}>
                      {req.members?.map((m, i) => (
                        <span key={i} style={styles.assocMemberChip}>
                          {m.firstName} {m.lastName}
                          {m.claimedCuts ? ` — ${m.claimedCuts}` : ""}
                        </span>
                      ))}
                    </div>
                    <div style={styles.assocActions}>
                      <input
                        type="text"
                        placeholder="Optional note (shown on denial)"
                        value={denyNotes[req.associationId] || ""}
                        onChange={(e) => setDenyNotes((prev) => ({ ...prev, [req.associationId]: e.target.value }))}
                        style={styles.assocNoteInput}
                      />
                      <button style={styles.assocApproveBtn} onClick={() => handleRespondAssociation(req.associationId, "APPROVE")}>
                        Approve
                      </button>
                      <button style={styles.assocDenyBtn} onClick={() => handleRespondAssociation(req.associationId, "DENY")}>
                        Deny
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Active associations */}
          {activeAssociations.length > 0 && (
            <div>
              <p style={styles.assocSubheading}>Active Associations ({activeAssociations.length})</p>
              <div style={styles.assocList}>
                {activeAssociations.map((assoc) => (
                  <div key={assoc.associationId} style={styles.assocCard}>
                    <div style={styles.assocCardHeader}>
                      <span style={styles.assocGroupName}>{assoc.groupName}</span>
                      <span style={{
                        ...styles.assocStatusBadge,
                        ...(assoc.status === "PENDING_DISASSOCIATION" ? styles.assocStatusPending : styles.assocStatusActive),
                      }}>
                        {assoc.status === "PENDING_DISASSOCIATION" ? "Disassociation Requested" : "Associated"}
                      </span>
                    </div>
                    {assoc.certifications?.length > 0 && (
                      <div style={styles.assocCerts}>
                        {assoc.certifications.map((c) => (
                          <span key={c} style={styles.assocCertTag}>{c}</span>
                        ))}
                      </div>
                    )}
                    <div style={styles.assocMembers}>
                      {assoc.members?.map((m, i) => (
                        <span key={i} style={styles.assocMemberChip}>
                          {m.firstName} {m.lastName}
                        </span>
                      ))}
                    </div>
                    {assoc.status === "PENDING_DISASSOCIATION" && (
                      <div style={{ display: "flex", gap: "10px", marginTop: "12px" }}>
                        <button style={styles.assocApproveBtn} onClick={() => handleRespondDisassociation(assoc.associationId, "CONFIRM")}>
                          Confirm Disassociation
                        </button>
                        <button style={styles.assocDenyBtn} onClick={() => handleRespondDisassociation(assoc.associationId, "DENY")}>
                          Stay Associated
                        </button>
                      </div>
                    )}

                    {/* Group Chat */}
                    <div style={{ marginTop: "16px", borderTop: "1px solid #f0ede9", paddingTop: "14px" }}>
                      <p style={{ ...styles.assocSubheading, marginBottom: "8px" }}>Group Chat</p>
                      <div
                        ref={(el) => { chatRefs.current[assoc.groupId] = el; }}
                        style={styles.sellerChatMessages}
                      >
                        {(chatMessages[assoc.groupId] || []).length === 0 && (
                          <p style={{ fontSize: "13px", color: "#bbb", fontStyle: "italic", margin: 0 }}>
                            No messages yet.
                          </p>
                        )}
                        {(chatMessages[assoc.groupId] || []).map((msg) => {
                          const isMe = msg.senderRole === "SELLER";
                          return (
                            <div
                              key={msg.id ?? msg.sentAt}
                              style={{ ...styles.sellerChatRow, justifyContent: isMe ? "flex-end" : "flex-start" }}
                            >
                              <div style={{ ...styles.sellerChatBubble, ...(isMe ? styles.sellerBubbleMe : styles.sellerBubbleOther) }}>
                                {!isMe && <p style={styles.sellerChatSender}>{msg.senderName}</p>}
                                <p style={styles.sellerChatContent}>{msg.content}</p>
                                <p style={styles.sellerChatTime}>
                                  {new Date(msg.sentAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                                </p>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                      <div style={styles.sellerChatInputRow}>
                        <input
                          style={styles.sellerChatInput}
                          type="text"
                          placeholder="Type a message..."
                          value={chatInputs[assoc.groupId] || ""}
                          onChange={(e) => setChatInputs((prev) => ({ ...prev, [assoc.groupId]: e.target.value }))}
                          onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSendMessage(assoc.groupId); } }}
                          maxLength={1000}
                        />
                        <button
                          style={{
                            ...styles.sellerChatSendBtn,
                            ...(!chatInputs[assoc.groupId]?.trim() || !stompClientRef.current?.connected
                              ? { opacity: 0.5, cursor: "not-allowed" } : {}),
                          }}
                          onClick={() => handleSendMessage(assoc.groupId)}
                          disabled={!chatInputs[assoc.groupId]?.trim() || !stompClientRef.current?.connected}
                        >
                          Send
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {pendingRequests.length === 0 && activeAssociations.length === 0 && (
            <p style={{ color: "#bbb", fontStyle: "italic" }}>No group requests or active associations.</p>
          )}
        </div>}
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
  tabBar: {
    backgroundColor: "white",
    borderBottom: "1px solid #e0dbd5",
    boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
  },
  tabBarInner: {
    maxWidth: "1200px",
    margin: "0 auto",
    padding: "0 48px",
    display: "flex",
    gap: "0",
  },
  tabBtn: {
    background: "none",
    border: "none",
    borderBottom: "3px solid transparent",
    padding: "14px 20px",
    fontSize: "14px",
    fontWeight: "600",
    color: "#888",
    cursor: "pointer",
    display: "flex",
    alignItems: "center",
    gap: "7px",
    transition: "color 0.15s, border-color 0.15s",
  },
  tabBtnActive: {
    color: SELLER_COLOR,
    borderBottom: `3px solid ${SELLER_COLOR}`,
  },
  tabBadge: {
    backgroundColor: "#c0392b",
    color: "white",
    borderRadius: "99px",
    fontSize: "11px",
    fontWeight: "700",
    padding: "1px 6px",
    lineHeight: "16px",
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
  sectionHeading: {
    fontSize: "20px",
    fontWeight: "700",
    color: SELLER_COLOR,
    margin: 0,
  },
  cowGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
    gap: "16px",
  },
  cowCard: {
    backgroundColor: "white",
    borderRadius: "8px",
    padding: "20px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    minHeight: "140px",
  },
  cowBreed: {
    fontSize: "16px",
    fontWeight: "700",
    color: SELLER_COLOR,
    textTransform: "capitalize",
  },
  cowDesc: {
    fontSize: "13px",
    color: "#666",
    margin: 0,
    flexGrow: 1,
  },
  cowPrice: {
    fontSize: "15px",
    fontWeight: "700",
    color: GREEN,
  },
  cowAvail: {
    fontSize: "13px",
    color: "#999",
  },
  removeCowBtn: {
    background: "none",
    border: "none",
    color: "#c00",
    fontSize: "16px",
    cursor: "pointer",
    padding: "0 4px",
    lineHeight: 1,
  },
  cowFormGrid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr 1fr",
    gap: "16px",
    marginTop: "8px",
  },
  cowFormLabel: {
    fontSize: "11px",
    fontWeight: "700",
    textTransform: "uppercase",
    color: "#888",
    margin: "0 0 4px 0",
    letterSpacing: "0.05em",
  },
  cowSelect: {
    width: "100%",
    padding: "6px 4px",
    border: "none",
    borderBottom: `2px solid ${SELLER_COLOR}`,
    backgroundColor: "transparent",
    fontSize: "15px",
    color: "#222",
    outline: "none",
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
  assocError: {
    backgroundColor: "#fee", color: "#c00", padding: "10px 14px",
    borderRadius: "6px", fontSize: "13px", marginBottom: "16px",
  },
  assocSubheading: {
    fontSize: "13px", fontWeight: "700", textTransform: "uppercase",
    letterSpacing: "0.06em", color: "#888", margin: "0 0 10px 0",
  },
  assocList: { display: "flex", flexDirection: "column", gap: "12px" },
  assocCard: {
    backgroundColor: "white", borderRadius: "8px", padding: "16px 20px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)", border: "1px solid #e8e4e0",
  },
  assocCardHeader: {
    display: "flex", justifyContent: "space-between", alignItems: "center",
    marginBottom: "8px", flexWrap: "wrap", gap: "8px",
  },
  assocGroupName: { fontSize: "16px", fontWeight: "700", color: "#222" },
  assocMemberCount: { fontSize: "12px", color: "#888" },
  assocCerts: { display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "8px" },
  assocCertTag: {
    backgroundColor: "#eaf3ee", color: GREEN, border: `1px solid ${GREEN}`,
    borderRadius: "99px", padding: "2px 10px", fontSize: "11px", fontWeight: "600",
  },
  assocMembers: { display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "4px" },
  assocMemberChip: {
    backgroundColor: "#f5f5f0", border: "1px solid #e0dbd5",
    borderRadius: "99px", padding: "3px 10px", fontSize: "12px", color: "#444",
  },
  assocActions: {
    display: "flex", alignItems: "center", gap: "10px",
    marginTop: "12px", flexWrap: "wrap",
  },
  assocNoteInput: {
    flex: 1, minWidth: "180px", padding: "6px 10px",
    border: "1px solid #ddd", borderRadius: "5px", fontSize: "13px", outline: "none",
  },
  assocApproveBtn: {
    padding: "7px 16px", backgroundColor: GREEN, color: "white",
    border: "none", borderRadius: "5px", fontSize: "13px", fontWeight: "600", cursor: "pointer",
  },
  assocDenyBtn: {
    padding: "7px 16px", backgroundColor: "white", color: "#c0392b",
    border: "2px solid #c0392b", borderRadius: "5px", fontSize: "13px",
    fontWeight: "600", cursor: "pointer",
  },
  assocStatusBadge: {
    fontSize: "11px", fontWeight: "700", letterSpacing: "0.05em",
    textTransform: "uppercase", padding: "3px 10px", borderRadius: "99px",
  },
  assocStatusActive: { backgroundColor: "#e8f5e9", color: "#2e7d32" },
  assocStatusPending: { backgroundColor: "#fff8e1", color: "#f57f17" },
  sellerChatMessages: {
    height: "220px", overflowY: "auto", display: "flex",
    flexDirection: "column", gap: "6px", padding: "6px 0", marginBottom: "10px",
  },
  sellerChatRow: { display: "flex" },
  sellerChatBubble: {
    maxWidth: "75%", padding: "7px 11px", borderRadius: "10px", wordBreak: "break-word",
  },
  sellerBubbleMe: {
    backgroundColor: SELLER_COLOR, color: "white", borderBottomRightRadius: "3px",
  },
  sellerBubbleOther: {
    backgroundColor: "#f0ede9", color: "#222", borderBottomLeftRadius: "3px",
  },
  sellerChatSender: { fontSize: "10px", fontWeight: "700", margin: "0 0 2px 0", opacity: 0.65 },
  sellerChatContent: { fontSize: "13px", margin: 0, lineHeight: 1.4 },
  sellerChatTime: { fontSize: "9px", margin: "3px 0 0 0", opacity: 0.55, textAlign: "right" },
  sellerChatInputRow: { display: "flex", gap: "8px" },
  sellerChatInput: {
    flex: 1, padding: "8px 10px", borderRadius: "6px",
    border: "1px solid #ddd", fontSize: "13px", outline: "none",
  },
  sellerChatSendBtn: {
    padding: "8px 16px", backgroundColor: SELLER_COLOR, color: "white",
    border: "none", borderRadius: "6px", fontSize: "13px", fontWeight: "600", cursor: "pointer",
  },
};

export default SellerDashboard;
