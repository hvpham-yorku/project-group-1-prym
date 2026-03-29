import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
  getGroup,
  saveCuts,
  leaveGroup,
  joinGroup,
  regenerateInviteCode,
  getMatchingFarms,
} from "../api/groups";
import {
  getGroupAssociation,
  requestAssociation,
  cancelAssociation,
  requestDisassociation,
} from "../api/association";
import GroupCowDiagram from "../components/GroupCowDiagram";
import { Client } from "@stomp/stompjs";
import { submitRating } from "../api/ratings";

const BUYER_COLOR = "#4a7c59";
const BROWN = "#5c4033";

const BADGE_STYLES = {
  KOSHER: { backgroundColor: "#e3f2fd", color: "#1565c0" },
  HALAL: { backgroundColor: "#fff3e0", color: "#e65100" },
  ORGANIC: { backgroundColor: "#e8f5e9", color: "#2e7d32" },
  GRASS_FED: { backgroundColor: "#f1f8e9", color: "#558b2f" },
  NON_GMO: { backgroundColor: "#fce4ec", color: "#880e4f" },
  ANIMAL_WELFARE_APPROVED: { backgroundColor: "#ede7f6", color: "#4527a0" },
  CONVENTIONAL: { backgroundColor: "#f5f5f5", color: "#555555" },
};

const CERT_LABELS = {
  KOSHER: "Kosher",
  HALAL: "Halal",
  ORGANIC: "Organic",
  GRASS_FED: "Grass-Fed",
  NON_GMO: "Non-GMO",
  ANIMAL_WELFARE_APPROVED: "Animal Welfare Approved",
  CONVENTIONAL: "Conventional",
};

// "Chuck, Rib x2"  →  { Chuck: 1, Rib: 2 }
function parseCuts(str) {
  if (!str) return {};
  const result = {};
  str.split(", ").forEach((item) => {
    const m = item.match(/^(.+?) x(\d+)$/);
    if (m) result[m[1]] = parseInt(m[2], 10);
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

//The big group detail page. Shows the cow diagram, members list,
//chat panel, matching farms, invite code, rating modal... basically
//everything related to a single group lives here. It's a lot.
function GroupDetailPage() {
  const { groupId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  // Helper function to filter farms by distance
  const filterFarmsByDistance = (farmList) => {
    if (!distanceFilterEnabled) return farmList;
    return farmList.filter(f => !f.distance || f.distance <= maxDistance);
  };

  const [group, setGroup] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Local diagram state — reflects the user's unsaved selections
  const [selectedCuts, setSelectedCuts] = useState({});
  const [saveLoading, setSaveLoading] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [leaveLoading, setLeaveLoading] = useState(false);
  const [joinLoading, setJoinLoading] = useState(false);
  const [regenLoading, setRegenLoading] = useState(false);
  const [codeCopied, setCodeCopied] = useState(false);
  const [farms, setFarms] = useState(null);

  // Distance filter state
  const [maxDistance, setMaxDistance] = useState(100); // miles
  const [distanceFilterEnabled, setDistanceFilterEnabled] = useState(false);

  // Chat state
  const [messages, setMessages] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const stompClientRef = useRef(null);
  const messagesContainerRef = useRef(null);

  // Association state
  const [association, setAssociation] = useState(null); // null = none, {} = no active assoc
  const [assocLoading, setAssocLoading] = useState(false);
  const [assocError, setAssocError] = useState("");

  // Rating modal state
  const [showRatingModal, setShowRatingModal] = useState(false);
  const [ratingCode, setRatingCode] = useState("");
  const [ratingScore, setRatingScore] = useState(0);
  const [hoveredStar, setHoveredStar] = useState(0);
  const [ratingError, setRatingError] = useState("");
  const [ratingSuccess, setRatingSuccess] = useState("");
  const [ratingSubmitting, setRatingSubmitting] = useState(false);

  //grabs the group data and matching farms in parallel, then
  //pre-fills the diagram with whatever cuts the user already saved
  const fetchGroup = async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      const [data, farmsData, assocData] = await Promise.all([
        getGroup(user.id, groupId),
        getMatchingFarms(user.id, groupId),
        getGroupAssociation(user.id, groupId),
      ]);
      setGroup(data);
      setFarms(farmsData);
      setAssociation(assocData && assocData.associationId ? assocData : null);
      // Pre-fill diagram with the user's currently saved cuts
      setSelectedCuts(parseCuts(data.myClaimedCuts));
    } catch (err) {
      setError(err.message || "Failed to load group.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroup();
  }, [user?.id, groupId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Load message history and connect WebSocket when the user is a member
  useEffect(() => {
    if (!user?.id || !group?.alreadyJoined) return;

    // Fetch message history
    fetch(
      `/api/buyer/groups/${groupId}/messages?userId=${user.id}`,
      { credentials: "include" },
    )
      .then((r) => r.json())
      .then((data) => setMessages(Array.isArray(data) ? data : []))
      .catch(() => {});

    // Connect to WebSocket — chat + association status updates
    const wsProtocol = window.location.protocol === "https:" ? "wss" : "ws";
    const client = new Client({
      brokerURL: `${wsProtocol}://${window.location.host}/ws/websocket`,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/group/${groupId}`, (frame) => {
          const msg = JSON.parse(frame.body);
          setMessages((prev) => [...prev, msg]);
        });
        // Real-time association status updates for this group
        client.subscribe(`/topic/group/${groupId}/association`, (frame) => {
          const event = JSON.parse(frame.body);
          if (
            event.type === "ASSOCIATION_APPROVED" ||
            event.type === "ASSOCIATION_DENIED" ||
            event.type === "DISASSOCIATION_CONFIRMED" ||
            event.type === "DISASSOCIATION_DENIED"
          ) {
            // Re-fetch association state from server for accuracy
            getGroupAssociation(user.id, groupId)
              .then((a) => setAssociation(a && a.associationId ? a : null))
              .catch(() => {});
          }
        });
      },
    });
    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [user?.id, groupId, group?.alreadyJoined]);

  // Auto-scroll chat container to bottom when new messages arrive
  useEffect(() => {
    const el = messagesContainerRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  const handleRequestAssociation = async (sellerId) => {
    setAssocLoading(true);
    setAssocError("");
    try {
      const result = await requestAssociation(user.id, groupId, sellerId);
      setAssociation(result);
    } catch (err) {
      setAssocError(err.message || "Failed to send request.");
    } finally {
      setAssocLoading(false);
    }
  };

  const handleCancelAssociation = async () => {
    setAssocLoading(true);
    setAssocError("");
    try {
      await cancelAssociation(user.id, groupId);
      setAssociation(null);
    } catch (err) {
      setAssocError(err.message || "Failed to cancel request.");
    } finally {
      setAssocLoading(false);
    }
  };

  const handleRequestDisassociation = async () => {
    if (!window.confirm("Request disassociation from this seller? They must confirm before it takes effect.")) return;
    setAssocLoading(true);
    setAssocError("");
    try {
      const result = await requestDisassociation(user.id, groupId);
      setAssociation(result);
    } catch (err) {
      setAssocError(err.message || "Failed to request disassociation.");
    } finally {
      setAssocLoading(false);
    }
  };

  //publishes the chat message over the websocket stomp connection
  const handleSendMessage = () => {
    const content = chatInput.trim();
    if (!content || !stompClientRef.current?.connected) return;
    stompClientRef.current.publish({
      destination: `/app/chat/${groupId}`,
      body: JSON.stringify({ content }),
    });
    setChatInput("");
  };

  //enter key sends the message, shift+enter lets you do a newline
  const handleChatKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  //toggles a cut section on or off in the local diagram state
  const handleToggle = (id) => {
    setSelectedCuts((prev) => {
      const cuts = { ...prev };
      if (id in cuts) delete cuts[id];
      else cuts[id] = 1;
      return cuts;
    });
    setSaveSuccess(false);
  };

  //bumps quantity up or down, but caps it so you can't exceed what's
  //available after accounting for what other members already claimed
  const handleQtyChange = (id, delta) => {
    setSaveSuccess(false);
    setSelectedCuts((prev) => {
      const cuts = { ...prev };
      const maxQty = 2 - (group?.othersClaimedQty?.[id] || 0);
      const qty = (cuts[id] ?? 1) + delta;
      if (qty < 1) delete cuts[id];
      else if (qty <= maxQty) cuts[id] = qty;
      return cuts;
    });
  };

  //serializes the selected cuts back to a string and POSTs them
  const handleSave = async () => {
    setSaveLoading(true);
    setError("");
    setSaveSuccess(false);
    try {
      const cutsStr = serializeCuts(selectedCuts);
      const updated = await saveCuts(user.id, groupId, cutsStr || null);
      setGroup(updated);
      setSelectedCuts(parseCuts(updated.myClaimedCuts));
      setSaveSuccess(true);
    } catch (err) {
      setError(err.message || "Failed to save cuts.");
    } finally {
      setSaveLoading(false);
    }
  };

  const handleJoin = async () => {
    setJoinLoading(true);
    setError("");
    try {
      await joinGroup(user.id, groupId);
      await fetchGroup();
    } catch (err) {
      setError(err.message || "Failed to join group.");
      setJoinLoading(false);
    }
  };

  //copies invite code to clipboard with a little "Copied!" feedback
  const handleCopyCode = () => {
    navigator.clipboard.writeText(group.inviteCode);
    setCodeCopied(true);
    setTimeout(() => setCodeCopied(false), 2000);
  };

  //generates a fresh invite code, old one dies immediately
  const handleRegenCode = async () => {
    if (
      !window.confirm(
        "Generate a new invite code? The old one will stop working immediately.",
      )
    )
      return;
    setRegenLoading(true);
    setError("");
    try {
      const updated = await regenerateInviteCode(user.id, groupId);
      setGroup(updated);
    } catch (err) {
      setError(err.message || "Failed to regenerate code.");
    } finally {
      setRegenLoading(false);
    }
  };

  const handleLeave = async () => {
    if (!window.confirm("Are you sure you want to leave this group?")) return;
    setLeaveLoading(true);
    setError("");
    try {
      await leaveGroup(user.id, groupId);
      navigate("/buyer/profile");
    } catch (err) {
      setError(err.message || "Failed to leave group.");
      setLeaveLoading(false);
    }
  };

  //validates the code + star score and sends the rating to the backend
  const handleSubmitRating = async () => {
    setRatingError("");
    setRatingSuccess("");
    if (!ratingCode.trim()) {
      setRatingError("Please enter the code from the seller.");
      return;
    }
    if (ratingScore == 0) {
      setRatingError("Please select a star rating.");
      return;
    }
    setRatingSubmitting(true);
    try {
      const result = await submitRating(
        user.id,
        ratingCode.trim(),
        ratingScore,
      );
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
      setRatingError("Something went wrong. Please try again");
    } finally {
      setRatingSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div style={styles.page}>
        <p style={{ padding: "48px", color: "#666" }}>Loading...</p>
      </div>
    );
  }

  if (!group) {
    return (
      <div style={styles.page}>
        <p style={{ padding: "48px", color: "#c00" }}>
          {error || "Group not found."}
        </p>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      {/* ── Header Banner ── */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <button
            style={styles.backBtn}
            onClick={() => navigate("/buyer/profile")}
          >
            ← Back
          </button>
          <div style={{ flex: 1 }}>
            <div style={styles.groupIdLine}>Group ID: {group.groupId}</div>
            <h1 style={styles.groupName}>{group.groupName}</h1>
            {group.certifications.length > 0 && (
              <div style={styles.certBadges}>
                {group.certifications.map((c) =>
                  CERT_LABELS[c] ? (
                    <span
                      key={c}
                      style={{ ...styles.badge, ...(BADGE_STYLES[c] || {}) }}
                    >
                      {CERT_LABELS[c]}
                    </span>
                  ) : null,
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Content ── */}
      <div style={styles.content}>
        {error && <div style={styles.errorBox}>{error}</div>}
        {saveSuccess && (
          <div style={styles.successBox}>Your cuts have been saved!</div>
        )}

        {/* ── Two-column layout ── */}
        <div style={styles.mainLayout}>
          {/* Left: cow diagram */}
          <div style={styles.leftPanel}>
            <div style={styles.diagramCard}>
              <h2 style={styles.sectionTitle}>Divide the Cow</h2>

              {group.alreadyJoined ? (
                <>
                  <div style={styles.legend}>
                    <span style={styles.legendItem}>
                      <span
                        style={{
                          ...styles.legendDot,
                          backgroundColor: BUYER_COLOR,
                        }}
                      />{" "}
                      My cuts
                    </span>
                    <span style={styles.legendItem}>
                      <span
                        style={{ ...styles.legendDot, backgroundColor: "#888" }}
                      />{" "}
                      Taken by others
                    </span>
                    <span style={styles.legendItem}>
                      <span
                        style={{
                          ...styles.legendDot,
                          backgroundColor: "#b03030",
                        }}
                      />{" "}
                      Available
                    </span>
                  </div>

                  <GroupCowDiagram
                    selectedCuts={selectedCuts}
                    othersQty={group.othersClaimedQty || {}}
                    onToggle={handleToggle}
                    onQuantityChange={handleQtyChange}
                  />

                  {Object.keys(selectedCuts).length > 0 && (
                    <div style={styles.cutsTextRow}>
                      <span style={styles.cutsLabel}>Selected: </span>
                      {Object.entries(selectedCuts).map(
                        ([cut, qty], i, arr) => (
                          <span key={cut}>
                            <span style={styles.cutName}>
                              {cut}
                              {qty > 1 ? ` ×${qty}` : ""}
                            </span>
                            {i < arr.length - 1 && (
                              <span style={{ color: "#aaa" }}>, </span>
                            )}
                          </span>
                        ),
                      )}
                    </div>
                  )}

                  <div style={styles.saveRow}>
                    <button
                      style={{
                        ...styles.saveBtn,
                        ...(saveLoading ? styles.saveBtnDisabled : {}),
                      }}
                      onClick={handleSave}
                      disabled={saveLoading}
                    >
                      {saveLoading ? "Saving..." : "Save Changes"}
                    </button>
                    <span style={styles.saveHint}>
                      Saved cuts are locked in for your group members.
                    </span>
                  </div>
                </>
              ) : (
                <>
                  <div style={styles.legend}>
                    <span style={styles.legendItem}>
                      <span
                        style={{ ...styles.legendDot, backgroundColor: "#888" }}
                      />{" "}
                      Taken
                    </span>
                    <span style={styles.legendItem}>
                      <span
                        style={{
                          ...styles.legendDot,
                          backgroundColor: "#b03030",
                        }}
                      />{" "}
                      Available
                    </span>
                  </div>
                  <GroupCowDiagram
                    selectedCuts={{}}
                    othersQty={group.othersClaimedQty || {}}
                  />
                  <div style={styles.joinRow}>
                    <button
                      style={{
                        ...styles.joinBtn,
                        ...(joinLoading ? styles.joinBtnDisabled : {}),
                      }}
                      onClick={handleJoin}
                      disabled={joinLoading}
                    >
                      {joinLoading ? "Joining..." : "Join Group"}
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Members */}
            <div style={styles.membersCard}>
              <h2 style={styles.sectionTitle}>Members ({group.memberCount})</h2>
              {group.memberCount === 0 ? (
                <p style={styles.emptyText}>No members yet.</p>
              ) : (
                <div style={styles.memberGrid}>
                  {group.members.map((m, i) => (
                    <div key={i} style={styles.memberCard}>
                      <p style={styles.memberName}>{m.firstName}</p>
                      <p style={styles.memberCuts}>
                        {m.claimedCuts || "No cuts selected yet"}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Invite code */}
            {group.alreadyJoined && (
              <div style={styles.inviteCard}>
                <p style={styles.inviteLabel}>Invite Code</p>
                <div style={styles.inviteRow}>
                  <span style={styles.inviteCode}>{group.inviteCode}</span>
                  <button style={styles.copyBtn} onClick={handleCopyCode}>
                    {codeCopied ? "Copied!" : "Copy"}
                  </button>
                  {group.isCreator && (
                    <button
                      style={{
                        ...styles.regenBtn,
                        ...(regenLoading ? styles.regenBtnDisabled : {}),
                      }}
                      onClick={handleRegenCode}
                      disabled={regenLoading}
                    >
                      {regenLoading ? "Regenerating..." : "Regenerate"}
                    </button>
                  )}
                </div>
                <p style={styles.inviteHint}>
                  Share this code with friends so they can find and join this
                  group.
                </p>
              </div>
            )}

            {/* Leave group */}
            {group.alreadyJoined && (
              <div style={styles.leaveRow}>
                <button
                  style={{
                    ...styles.leaveBtn,
                    ...(leaveLoading ? styles.leaveBtnDisabled : {}),
                  }}
                  onClick={handleLeave}
                  disabled={leaveLoading}
                >
                  {leaveLoading ? "Leaving..." : "Leave Group"}
                </button>
              </div>
            )}
          </div>

          {/* Right: chat + matching farms */}
          <div style={styles.rightPanel}>
            {/* Group Chat — visible to members only */}
            {group.alreadyJoined && (
              <div style={styles.membersCard}>
                <h2 style={styles.sectionTitle}>Group Chat</h2>
                <div style={styles.chatMessages} ref={messagesContainerRef}>
                  {messages.length === 0 && (
                    <p style={styles.emptyText}>No messages yet. Say hello!</p>
                  )}
                  {messages.map((msg) => {
                    const isMe = msg.senderId === user.id;
                    const isSeller = msg.senderRole === "SELLER";
                    return (
                      <div
                        key={msg.id ?? msg.sentAt}
                        style={{
                          ...styles.chatBubbleRow,
                          justifyContent: isMe ? "flex-end" : "flex-start",
                        }}
                      >
                        <div
                          style={{
                            ...styles.chatBubble,
                            ...(isMe
                              ? styles.chatBubbleMe
                              : isSeller
                              ? styles.chatBubbleSeller
                              : styles.chatBubbleOther),
                          }}
                        >
                          {!isMe && (
                            <p style={styles.chatSender}>
                              {msg.senderName}
                              {isSeller && (
                                <span style={styles.sellerChatBadge}> SELLER</span>
                              )}
                            </p>
                          )}
                          <p style={styles.chatContent}>{msg.content}</p>
                          <p style={styles.chatTime}>
                            {new Date(msg.sentAt).toLocaleTimeString([], {
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                </div>
                <div style={styles.chatInputRow}>
                  <input
                    style={styles.chatInput}
                    type="text"
                    placeholder="Type a message..."
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    onKeyDown={handleChatKeyDown}
                    maxLength={1000}
                  />
                  <button
                    style={{
                      ...styles.chatSendBtn,
                      ...(!chatInput.trim() ||
                      !stompClientRef.current?.connected
                        ? styles.chatSendBtnDisabled
                        : {}),
                    }}
                    onClick={handleSendMessage}
                    disabled={
                      !chatInput.trim() || !stompClientRef.current?.connected
                    }
                  >
                    Send
                  </button>
                </div>
              </div>
            )}

            {/* Matching Farms */}
            {farms && (
              <div style={styles.membersCard}>
                <h2 style={styles.sectionTitle}>Matching Farms</h2>

                {/* Association status banner */}
                {assocError && (
                  <div style={{ ...styles.assocBanner, ...styles.assocBannerError }}>
                    {assocError}
                  </div>
                )}
                {association?.status === "PENDING_ASSOCIATION" && (
                  <div style={{ ...styles.assocBanner, ...styles.assocBannerPending }}>
                    <span>
                      Request sent to <strong>{association.shopName || "seller"}</strong> — waiting for their response.
                    </span>
                    {group.isCreator && (
                      <button
                        style={styles.assocCancelBtn}
                        onClick={handleCancelAssociation}
                        disabled={assocLoading}
                      >
                        Withdraw
                      </button>
                    )}
                  </div>
                )}
                {association?.status === "ASSOCIATED" && (
                  <div style={{ ...styles.assocBanner, ...styles.assocBannerActive }}>
                    <span>
                      Associated with <strong>{association.shopName || "seller"}</strong>
                    </span>
                    {group.isCreator && (
                      <button
                        style={styles.assocCancelBtn}
                        onClick={handleRequestDisassociation}
                        disabled={assocLoading}
                      >
                        Request Disassociation
                      </button>
                    )}
                  </div>
                )}
                {association?.status === "PENDING_DISASSOCIATION" && (
                  <div style={{ ...styles.assocBanner, ...styles.assocBannerPending }}>
                    Disassociation request sent to <strong>{association.shopName || "seller"}</strong> — awaiting their confirmation.
                  </div>
                )}

                {/* Distance Filter */}
                <div style={styles.distanceFilter}>
                  <label style={styles.filterLabel}>
                    <input
                      type="checkbox"
                      checked={distanceFilterEnabled}
                      onChange={(e) => setDistanceFilterEnabled(e.target.checked)}
                      style={styles.checkbox}
                    />
                    <span>Filter by distance</span>
                  </label>

                  {distanceFilterEnabled && (
                    <div style={styles.sliderContainer}>
                      <label style={styles.sliderLabel}>
                        Show farms within {maxDistance} miles
                      </label>
                      <input
                        type="range"
                        min="10"
                        max="500"
                        step="10"
                        value={maxDistance}
                        onChange={(e) => setMaxDistance(Number(e.target.value))}
                        style={styles.slider}
                      />
                    </div>
                  )}
                </div>

                <div style={styles.farmsSubheading}>
                  <span style={styles.perfectBadge}>Perfect Match</span>
                  <span style={styles.farmsSub}>
                    {filterFarmsByDistance(farms.perfectMatches).length === 0
                      ? distanceFilterEnabled && farms.perfectMatches.length > 0 ? "No farms within selected distance." : "No farms match all certifications."
                      : `${filterFarmsByDistance(farms.perfectMatches).length} farm${filterFarmsByDistance(farms.perfectMatches).length > 1 ? "s" : ""} meet${filterFarmsByDistance(farms.perfectMatches).length === 1 ? "s" : ""} all requirements`}
                  </span>
                </div>
                {filterFarmsByDistance(farms.perfectMatches).length > 0 && (
                  <div style={styles.farmList}>
                    {filterFarmsByDistance(farms.perfectMatches).map((f) => (
                      <div
                        key={f.sellerId}
                        style={{
                          ...styles.farmCard,
                          borderLeft: "4px solid #2e7d32",
                        }}
                      >
                        <div style={styles.farmHeader}>
                          <p style={styles.farmShop}>
                            {f.shopName || "Unnamed Farm"}
                          </p>
                          {f.distanceFormatted && (
                            <span style={styles.distanceBadge}>
                              📍 {f.distanceFormatted}
                            </span>
                          )}
                        </div>
                        <p style={styles.farmName}>{f.sellerName}</p>
                        <p style={styles.farmContact}>{f.email}</p>
                        {f.phoneNumber && (
                          <p style={styles.farmContact}>{f.phoneNumber}</p>
                        )}
                        {f.certifications.length > 0 && (
                          <div style={styles.farmCerts}>
                            {f.certifications.map((c) => (
                              <span
                                key={c}
                                style={{
                                  ...styles.badge,
                                  ...(BADGE_STYLES[c] || {
                                    backgroundColor: "#eee",
                                    color: "#555",
                                  }),
                                  fontSize: "10px",
                                }}
                              >
                                {CERT_LABELS[c] || c}
                              </span>
                            ))}
                          </div>
                        )}
                        {group.isCreator && !association && (
                          <button
                            style={styles.assocRequestBtn}
                            onClick={() => handleRequestAssociation(f.sellerId)}
                            disabled={assocLoading}
                          >
                            Request Association
                          </button>
                        )}
                        {association?.status === "ASSOCIATED" &&
                          association.sellerId === f.sellerId && (
                            <span style={styles.assocActiveBadge}>✓ Associated</span>
                          )}
                      </div>
                    ))}
                  </div>
                )}

                {filterFarmsByDistance(farms.partialMatches).length > 0 && (
                  <>
                    <div
                      style={{ ...styles.farmsSubheading, marginTop: "16px" }}
                    >
                      <span style={styles.partialBadge}>Partial Match</span>
                      <span style={styles.farmsSub}>
                        {filterFarmsByDistance(farms.partialMatches).length} farm
                        {filterFarmsByDistance(farms.partialMatches).length > 1 ? "s" : ""} with some
                        matching certifications
                      </span>
                    </div>
                    <div style={styles.farmList}>
                      {filterFarmsByDistance(farms.partialMatches).map((f) => (
                        <div
                          key={f.sellerId}
                          style={{
                            ...styles.farmCard,
                            borderLeft: "4px solid #f9a825",
                          }}
                        >
                          <div style={styles.farmHeader}>
                            <p style={styles.farmShop}>
                              {f.shopName || "Unnamed Farm"}
                            </p>
                            {f.distanceFormatted && (
                              <span style={styles.distanceBadge}>
                                📍 {f.distanceFormatted}
                              </span>
                            )}
                          </div>
                          <p style={styles.farmName}>{f.sellerName}</p>
                          <p style={styles.farmContact}>{f.email}</p>
                          {f.phoneNumber && (
                            <p style={styles.farmContact}>{f.phoneNumber}</p>
                          )}
                          <p style={styles.matchScore}>
                            {f.matchCount} of {f.totalRequired} certs matched
                          </p>
                          {f.certifications.length > 0 && (
                            <div style={styles.farmCerts}>
                              {f.certifications.map((c) => (
                                <span
                                  key={c}
                                  style={{
                                    ...styles.badge,
                                    ...(BADGE_STYLES[c] || {
                                      backgroundColor: "#eee",
                                      color: "#555",
                                    }),
                                    fontSize: "10px",
                                  }}
                                >
                                  {CERT_LABELS[c] || c}
                                </span>
                              ))}
                            </div>
                          )}
                          {group.isCreator && !association && (
                            <button
                              style={styles.assocRequestBtn}
                              onClick={() => handleRequestAssociation(f.sellerId)}
                              disabled={assocLoading}
                            >
                              Request Association
                            </button>
                          )}
                          {association?.status === "ASSOCIATED" &&
                            association.sellerId === f.sellerId && (
                              <span style={styles.assocActiveBadge}>✓ Associated</span>
                            )}
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </div>
            )}
            {/* Rate a Farm button — only visible to members */}
            {group.alreadyJoined && (
              <div
                style={{
                  marginTop: "16px",
                  borderTop: "1px solid #eee",
                  paddingTop: "16px",
                }}
              >
                <button
                  style={styles.rateBtn}
                  onClick={() => setShowRatingModal(true)}
                >
                  ⭐ Rate a Farm
                </button>
                <p style={styles.rateHint}>
                  Have a rating code from a seller? Submit your rating here.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/*Rating modal*/}
      {showRatingModal && (
        <div style={styles.modalOverlay}>
          <div style={styles.ratingModal}>
            <h2 style={styles.modalTitle}>Rate a Farm</h2>
            <p style={styles.modalSubtitle}>
              Enter the code you received from the seller
            </p>
            <input
              style={styles.codeInput}
              placeholder="Enter seller code (e.g. PRYM-ABC123)"
              value={ratingCode}
              onChange={(e) => setRatingCode(e.target.value.toUpperCase())}
            />
            <div style={styles.starRow}>
              {[1, 2, 3, 4, 5].map((star) => (
                <span
                  key={star}
                  style={{
                    fontSize: "40px",
                    cursor: "pointer",
                    color:
                      star <= (hoveredStar || ratingScore) ? "#f5a623" : "#ccc",
                    transition: "color 0.1s",
                  }}
                  onMouseEnter={() => setHoveredStar(star)}
                  onMouseLeave={() => setHoveredStar(0)}
                  onClick={() => setRatingScore(star)}
                >
                  ★
                </span>
              ))}
            </div>
            <p style={styles.scoreLabel}>
              {ratingScore > 0
                ? `You selected: ${ratingScore} star${ratingScore !== 1 ? "s" : ""}`
                : "Select a rating"}
            </p>
            {ratingError && <p style={styles.modalError}>{ratingError}</p>}
            {ratingSuccess && (
              <p style={styles.modalSuccess}>{ratingSuccess}</p>
            )}
            <div style={styles.modalButtons}>
              <button
                style={styles.cancelBtn}
                onClick={() => {
                  setShowRatingModal(false);
                  setRatingCode("");
                  setRatingScore(0);
                  setRatingError("");
                }}
              >
                Cancel
              </button>
              <button
                style={{
                  ...styles.submitBtn,
                  ...(ratingSubmitting
                    ? { opacity: 0.6, cursor: "not-allowed" }
                    : {}),
                }}
                onClick={handleSubmitRating}
                disabled={ratingSubmitting}
              >
                {ratingSubmitting ? "Submitting..." : "Submit Rating"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { minHeight: "100vh", backgroundColor: "#f5f5f0" },
  banner: { backgroundColor: BUYER_COLOR, padding: "40px 0" },
  bannerInner: {
    maxWidth: "1600px",
    margin: "0 auto",
    padding: "0 48px",
    display: "flex",
    alignItems: "flex-start",
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
    flexShrink: 0,
    marginTop: "8px",
  },
  groupIdLine: {
    fontSize: "12px",
    color: "rgba(255,255,255,0.65)",
    fontWeight: "600",
    letterSpacing: "0.05em",
    marginBottom: "4px",
  },
  groupName: {
    fontSize: "26px",
    fontWeight: "700",
    color: "white",
    margin: "0 0 10px 0",
  },
  certBadges: { display: "flex", gap: "6px", flexWrap: "wrap" },
  badge: {
    display: "inline-block",
    padding: "3px 10px",
    borderRadius: "99px",
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.05em",
    textTransform: "uppercase",
  },
  content: {
    maxWidth: "1600px",
    margin: "0 auto",
    padding: "32px 48px 72px",
    display: "flex",
    flexDirection: "column",
    gap: "24px",
  },
  mainLayout: {
    display: "flex",
    gap: "24px",
    alignItems: "flex-start",
  },
  leftPanel: {
    flex: 3,
    minWidth: 0,
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  rightPanel: {
    flex: 2,
    minWidth: 0,
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  errorBox: {
    backgroundColor: "#fee",
    color: "#c00",
    padding: "12px 16px",
    borderRadius: "6px",
    fontSize: "14px",
  },
  successBox: {
    backgroundColor: "#e8f5e9",
    color: "#2e7d32",
    padding: "12px 16px",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
  },
  diagramCard: {
    backgroundColor: "white",
    borderRadius: "10px",
    padding: "24px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
  },
  sectionTitle: {
    fontSize: "18px",
    fontWeight: "700",
    color: BROWN,
    margin: "0 0 14px 0",
  },
  legend: {
    display: "flex",
    gap: "20px",
    marginBottom: "14px",
    fontSize: "13px",
    color: "#555",
    flexWrap: "wrap",
  },
  legendItem: { display: "flex", alignItems: "center", gap: "6px" },
  legendDot: {
    width: "12px",
    height: "12px",
    borderRadius: "50%",
    display: "inline-block",
    opacity: 0.85,
  },
  cutsTextRow: { marginTop: "14px", fontSize: "14px", lineHeight: 1.6 },
  cutsLabel: {
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.07em",
    textTransform: "uppercase",
    color: BUYER_COLOR,
    marginRight: "6px",
  },
  cutName: { fontWeight: "600", color: "#222", fontSize: "14px" },
  saveRow: {
    marginTop: "18px",
    display: "flex",
    alignItems: "center",
    gap: "16px",
    flexWrap: "wrap",
  },
  saveBtn: {
    padding: "11px 28px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "700",
    cursor: "pointer",
  },
  saveBtnDisabled: { opacity: 0.6, cursor: "not-allowed" },
  saveHint: { fontSize: "12px", color: "#888", fontStyle: "italic" },
  emptyText: {
    fontSize: "14px",
    color: "#bbb",
    fontStyle: "italic",
    margin: 0,
  },
  membersCard: {
    backgroundColor: "white",
    borderRadius: "10px",
    padding: "20px 24px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
  },
  memberGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
    gap: "12px",
  },
  memberCard: {
    backgroundColor: "#f9f7f5",
    borderRadius: "8px",
    padding: "12px 16px",
    border: "1px solid #e8e4e0",
  },
  memberName: {
    fontSize: "15px",
    fontWeight: "700",
    color: "#222",
    margin: "0 0 4px 0",
  },
  memberCuts: { fontSize: "13px", color: "#555", margin: 0 },
  joinRow: { marginTop: "18px" },
  joinBtn: {
    padding: "11px 28px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "700",
    cursor: "pointer",
  },
  joinBtnDisabled: { opacity: 0.6, cursor: "not-allowed" },
  inviteCard: {
    backgroundColor: "white",
    borderRadius: "10px",
    padding: "20px 24px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
  },
  inviteLabel: {
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.07em",
    textTransform: "uppercase",
    color: BUYER_COLOR,
    margin: "0 0 10px 0",
  },
  inviteRow: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    flexWrap: "wrap",
  },
  inviteCode: {
    fontFamily: "monospace",
    fontSize: "22px",
    fontWeight: "700",
    letterSpacing: "0.15em",
    color: "#222",
    backgroundColor: "#f5f5f0",
    padding: "6px 16px",
    borderRadius: "6px",
    border: "1px solid #e0dbd5",
  },
  copyBtn: {
    padding: "7px 16px",
    backgroundColor: "white",
    color: BUYER_COLOR,
    border: `2px solid ${BUYER_COLOR}`,
    borderRadius: "6px",
    fontSize: "13px",
    fontWeight: "600",
    cursor: "pointer",
  },
  regenBtn: {
    padding: "7px 16px",
    backgroundColor: "white",
    color: "#888",
    border: "2px solid #ccc",
    borderRadius: "6px",
    fontSize: "13px",
    fontWeight: "600",
    cursor: "pointer",
  },
  regenBtnDisabled: { opacity: 0.6, cursor: "not-allowed" },
  inviteHint: {
    fontSize: "12px",
    color: "#999",
    margin: "10px 0 0 0",
    fontStyle: "italic",
  },
  farmsSubheading: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    marginBottom: "12px",
  },
  farmsSub: { fontSize: "13px", color: "#777" },
  perfectBadge: {
    backgroundColor: "#e8f5e9",
    color: "#2e7d32",
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.05em",
    textTransform: "uppercase",
    padding: "3px 10px",
    borderRadius: "99px",
  },
  partialBadge: {
    backgroundColor: "#fff8e1",
    color: "#f57f17",
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.05em",
    textTransform: "uppercase",
    padding: "3px 10px",
    borderRadius: "99px",
  },
  farmList: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    marginTop: "8px",
  },
  farmGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
    gap: "12px",
  },
  farmCard: {
    backgroundColor: "white",
    borderRadius: "8px",
    padding: "16px 20px",
    boxShadow: "0 1px 4px rgba(0,0,0,0.07)",
    border: "1px solid #e8e4e0",
  },
  farmShop: {
    fontSize: "15px",
    fontWeight: "700",
    color: "#222",
    margin: "0 0 2px 0",
  },
  farmName: { fontSize: "13px", color: "#555", margin: "0 0 6px 0" },
  farmContact: { fontSize: "12px", color: "#777", margin: "0 0 2px 0" },
  farmCerts: {
    display: "flex",
    gap: "4px",
    flexWrap: "wrap",
    marginTop: "8px",
  },
  matchScore: {
    fontSize: "12px",
    fontWeight: "600",
    color: "#f57f17",
    margin: "4px 0 0 0",
  },
  chatMessages: {
    height: "320px",
    overflowY: "auto",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    padding: "8px 0",
    marginBottom: "12px",
  },
  chatBubbleRow: { display: "flex" },
  chatBubble: {
    maxWidth: "75%",
    padding: "8px 12px",
    borderRadius: "12px",
    wordBreak: "break-word",
  },
  chatBubbleMe: {
    backgroundColor: BUYER_COLOR,
    color: "white",
    borderBottomRightRadius: "4px",
  },
  chatBubbleOther: {
    backgroundColor: "#f0ede9",
    color: "#222",
    borderBottomLeftRadius: "4px",
  },
  chatBubbleSeller: {
    backgroundColor: "#fff8e1",
    color: "#222",
    borderBottomLeftRadius: "4px",
    border: "1px solid #ffe082",
  },
  sellerChatBadge: {
    fontSize: "9px",
    fontWeight: "700",
    letterSpacing: "0.06em",
    color: "#f57f17",
    backgroundColor: "#fff3cd",
    padding: "1px 5px",
    borderRadius: "4px",
    marginLeft: "4px",
  },
  chatSender: {
    fontSize: "11px",
    fontWeight: "700",
    margin: "0 0 2px 0",
    opacity: 0.7,
  },
  chatContent: { fontSize: "14px", margin: 0, lineHeight: 1.4 },
  chatTime: {
    fontSize: "10px",
    margin: "4px 0 0 0",
    opacity: 0.6,
    textAlign: "right",
  },
  chatInputRow: { display: "flex", gap: "8px" },
  chatInput: {
    flex: 1,
    padding: "9px 12px",
    borderRadius: "6px",
    border: "1px solid #ddd",
    fontSize: "14px",
    outline: "none",
  },
  chatSendBtn: {
    padding: "9px 18px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  chatSendBtnDisabled: { opacity: 0.5, cursor: "not-allowed" },
  leaveRow: {},
  leaveBtn: {
    padding: "10px 24px",
    backgroundColor: "white",
    color: "#c0392b",
    border: "2px solid #c0392b",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  leaveBtnDisabled: { opacity: 0.6, cursor: "not-allowed" },
  rateBtn: {
    padding: "10px 20px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  rateHint: {
    fontSize: "12px",
    color: "#999",
    fontStyle: "italic",
    margin: "8px 0 0 0",
  },
  modalOverlay: {
    position: "fixed",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: "rgba(0,0,0,0.5)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
  },
  ratingModal: {
    backgroundColor: "white",
    borderRadius: "12px",
    padding: "36px",
    width: "400px",
    boxShadow: "0 8px 32px rgba(0,0,0,0.2)",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "16px",
  },
  modalTitle: {
    fontSize: "22px",
    fontWeight: "700",
    color: BROWN,
    margin: 0,
  },
  modalSubtitle: {
    fontSize: "14px",
    color: "#666",
    margin: 0,
    textAlign: "center",
  },
  codeInput: {
    width: "100%",
    padding: "10px 14px",
    border: "2px solid #ddd",
    borderRadius: "6px",
    fontSize: "15px",
    boxSizing: "border-box",
    letterSpacing: "0.05em",
    outline: "none",
  },
  starRow: { display: "flex", gap: "8px" },
  scoreLabel: { fontSize: "14px", color: "#555", margin: 0 },
  modalError: { color: "#c00", fontSize: "14px", margin: 0 },
  modalSuccess: {
    color: BUYER_COLOR,
    fontSize: "14px",
    fontWeight: "600",
    margin: 0,
  },
  modalButtons: { display: "flex", gap: "12px", width: "100%" },
  cancelBtn: {
    flex: 1,
    padding: "10px",
    backgroundColor: "white",
    color: BROWN,
    border: `2px solid ${BROWN}`,
    borderRadius: "6px",
    fontSize: "15px",
    fontWeight: "600",
    cursor: "pointer",
  },
  submitBtn: {
    flex: 1,
    padding: "10px",
    backgroundColor: BUYER_COLOR,
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "15px",
    fontWeight: "600",
    cursor: "pointer",
  },
  distanceFilter: {
    backgroundColor: "#f9f9f9", padding: "12px 16px", borderRadius: "6px",
    marginBottom: "16px", border: "1px solid #e0e0e0",
  },
  filterLabel: {
    display: "flex", alignItems: "center", gap: "8px", fontSize: "14px",
    fontWeight: "600", color: "#333", cursor: "pointer",
  },
  checkbox: {
    cursor: "pointer", width: "16px", height: "16px",
  },
  sliderContainer: {
    marginTop: "12px", display: "flex", flexDirection: "column", gap: "8px",
  },
  sliderLabel: {
    fontSize: "13px", fontWeight: "500", color: "#555",
  },
  slider: {
    width: "100%", height: "6px", borderRadius: "3px",
    background: `linear-gradient(to right, ${BUYER_COLOR}, ${BUYER_COLOR})`,
    outline: "none", cursor: "pointer",
  },
  farmHeader: {
    display: "flex", justifyContent: "space-between", alignItems: "center",
    marginBottom: "4px",
  },
  distanceBadge: {
    fontSize: "12px", color: "#666", backgroundColor: "#f0f0f0",
    padding: "3px 8px", borderRadius: "10px", fontWeight: "600",
  },
  assocBanner: {
    display: "flex", alignItems: "center", justifyContent: "space-between",
    gap: "12px", padding: "10px 14px", borderRadius: "6px",
    fontSize: "13px", marginBottom: "12px", flexWrap: "wrap",
  },
  assocBannerPending: {
    backgroundColor: "#fff8e1", border: "1px solid #ffe082", color: "#7b5800",
  },
  assocBannerActive: {
    backgroundColor: "#e8f5e9", border: "1px solid #a5d6a7", color: "#1b5e20",
  },
  assocBannerError: {
    backgroundColor: "#fee", border: "1px solid #f5c6c6", color: "#c00",
  },
  assocCancelBtn: {
    padding: "5px 12px", backgroundColor: "white", border: "1px solid #ccc",
    borderRadius: "5px", fontSize: "12px", fontWeight: "600",
    cursor: "pointer", color: "#555", flexShrink: 0,
  },
  assocRequestBtn: {
    marginTop: "10px", padding: "7px 14px", backgroundColor: BUYER_COLOR,
    color: "white", border: "none", borderRadius: "5px",
    fontSize: "12px", fontWeight: "600", cursor: "pointer",
  },
  assocActiveBadge: {
    display: "inline-block", marginTop: "10px", padding: "4px 10px",
    backgroundColor: "#e8f5e9", color: "#2e7d32", borderRadius: "99px",
    fontSize: "11px", fontWeight: "700",
  },
};

export default GroupDetailPage;
