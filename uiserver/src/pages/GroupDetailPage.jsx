import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getGroup, saveCuts, leaveGroup, joinGroup, regenerateInviteCode, getMatchingFarms } from '../api/groups';
import GroupCowDiagram from '../components/GroupCowDiagram';

const BUYER_COLOR = '#4a7c59';
const BROWN = '#5c4033';

const BADGE_STYLES = {
  KOSHER:                  { backgroundColor: '#e3f2fd', color: '#1565c0' },
  HALAL:                   { backgroundColor: '#fff3e0', color: '#e65100' },
  ORGANIC:                 { backgroundColor: '#e8f5e9', color: '#2e7d32' },
  GRASS_FED:               { backgroundColor: '#f1f8e9', color: '#558b2f' },
  NON_GMO:                 { backgroundColor: '#fce4ec', color: '#880e4f' },
  ANIMAL_WELFARE_APPROVED: { backgroundColor: '#ede7f6', color: '#4527a0' },
  CONVENTIONAL:            { backgroundColor: '#f5f5f5', color: '#555555' },
};

const CERT_LABELS = {
  KOSHER: 'Kosher', HALAL: 'Halal', ORGANIC: 'Organic', GRASS_FED: 'Grass-Fed',
  NON_GMO: 'Non-GMO', ANIMAL_WELFARE_APPROVED: 'Animal Welfare Approved',
  CONVENTIONAL: 'Conventional',
};

// "Chuck, Rib x2"  →  { Chuck: 1, Rib: 2 }
function parseCuts(str) {
  if (!str) return {};
  const result = {};
  str.split(', ').forEach((item) => {
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
    .join(', ');
}

function GroupDetailPage() {
  const { groupId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [group, setGroup]             = useState(null);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');

  // Local diagram state — reflects the user's unsaved selections
  const [selectedCuts, setSelectedCuts] = useState({});
  const [saveLoading, setSaveLoading] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [leaveLoading, setLeaveLoading] = useState(false);
  const [joinLoading, setJoinLoading] = useState(false);
  const [regenLoading, setRegenLoading] = useState(false);
  const [codeCopied, setCodeCopied] = useState(false);
  const [farms, setFarms] = useState(null);

  const fetchGroup = async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      const [data, farmsData] = await Promise.all([
        getGroup(user.id, groupId),
        getMatchingFarms(user.id, groupId),
      ]);
      setGroup(data);
      setFarms(farmsData);
      // Pre-fill diagram with the user's currently saved cuts
      setSelectedCuts(parseCuts(data.myClaimedCuts));
    } catch (err) {
      setError(err.message || 'Failed to load group.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroup();
  }, [user?.id, groupId]);

  // Diagram interaction
  const handleToggle = (id) => {
    setSelectedCuts((prev) => {
      const cuts = { ...prev };
      if (id in cuts) delete cuts[id];
      else cuts[id] = 1;
      return cuts;
    });
    setSaveSuccess(false);
  };

  const handleQtyChange = (id, delta) => {
    setSaveSuccess(false);
    setSelectedCuts((prev) => {
      const cuts = { ...prev };
      const maxQty = 2 - ((group?.othersClaimedQty?.[id]) || 0);
      const qty = (cuts[id] ?? 1) + delta;
      if (qty < 1) delete cuts[id];
      else if (qty <= maxQty) cuts[id] = qty;
      return cuts;
    });
  };

  const handleSave = async () => {
    setSaveLoading(true);
    setError('');
    setSaveSuccess(false);
    try {
      const cutsStr = serializeCuts(selectedCuts);
      const updated = await saveCuts(user.id, groupId, cutsStr || null);
      setGroup(updated);
      setSelectedCuts(parseCuts(updated.myClaimedCuts));
      setSaveSuccess(true);
    } catch (err) {
      setError(err.message || 'Failed to save cuts.');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleJoin = async () => {
    setJoinLoading(true);
    setError('');
    try {
      await joinGroup(user.id, groupId);
      await fetchGroup();
    } catch (err) {
      setError(err.message || 'Failed to join group.');
      setJoinLoading(false);
    }
  };

  const handleCopyCode = () => {
    navigator.clipboard.writeText(group.inviteCode);
    setCodeCopied(true);
    setTimeout(() => setCodeCopied(false), 2000);
  };

  const handleRegenCode = async () => {
    if (!window.confirm('Generate a new invite code? The old one will stop working immediately.')) return;
    setRegenLoading(true);
    setError('');
    try {
      const updated = await regenerateInviteCode(user.id, groupId);
      setGroup(updated);
    } catch (err) {
      setError(err.message || 'Failed to regenerate code.');
    } finally {
      setRegenLoading(false);
    }
  };

  const handleLeave = async () => {
    if (!window.confirm('Are you sure you want to leave this group?')) return;
    setLeaveLoading(true);
    setError('');
    try {
      await leaveGroup(user.id, groupId);
      navigate('/buyer/profile');
    } catch (err) {
      setError(err.message || 'Failed to leave group.');
      setLeaveLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={styles.page}>
        <p style={{ padding: '48px', color: '#666' }}>Loading...</p>
      </div>
    );
  }

  if (!group) {
    return (
      <div style={styles.page}>
        <p style={{ padding: '48px', color: '#c00' }}>{error || 'Group not found.'}</p>
      </div>
    );
  }

  return (
    <div style={styles.page}>

      {/* ── Header Banner ── */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <button style={styles.backBtn} onClick={() => navigate('/buyer/profile')}>
            ← Back
          </button>
          <div style={{ flex: 1 }}>
            <div style={styles.groupIdLine}>Group ID: {group.groupId}</div>
            <h1 style={styles.groupName}>{group.groupName}</h1>
            {group.certifications.length > 0 && (
              <div style={styles.certBadges}>
                {group.certifications.map((c) =>
                  CERT_LABELS[c] ? (
                    <span key={c} style={{ ...styles.badge, ...(BADGE_STYLES[c] || {}) }}>
                      {CERT_LABELS[c]}
                    </span>
                  ) : null
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Content ── */}
      <div style={styles.content}>

        {error && <div style={styles.errorBox}>{error}</div>}
        {saveSuccess && <div style={styles.successBox}>Your cuts have been saved!</div>}

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
                      <span style={{ ...styles.legendDot, backgroundColor: BUYER_COLOR }} /> My cuts
                    </span>
                    <span style={styles.legendItem}>
                      <span style={{ ...styles.legendDot, backgroundColor: '#888' }} /> Taken by others
                    </span>
                    <span style={styles.legendItem}>
                      <span style={{ ...styles.legendDot, backgroundColor: '#b03030' }} /> Available
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
                      {Object.entries(selectedCuts).map(([cut, qty], i, arr) => (
                        <span key={cut}>
                          <span style={styles.cutName}>{cut}{qty > 1 ? ` ×${qty}` : ''}</span>
                          {i < arr.length - 1 && <span style={{ color: '#aaa' }}>,  </span>}
                        </span>
                      ))}
                    </div>
                  )}

                  <div style={styles.saveRow}>
                    <button
                      style={{ ...styles.saveBtn, ...(saveLoading ? styles.saveBtnDisabled : {}) }}
                      onClick={handleSave}
                      disabled={saveLoading}
                    >
                      {saveLoading ? 'Saving...' : 'Save Changes'}
                    </button>
                    <span style={styles.saveHint}>Saved cuts are locked in for your group members.</span>
                  </div>
                </>
              ) : (
                <>
                  <div style={styles.legend}>
                    <span style={styles.legendItem}>
                      <span style={{ ...styles.legendDot, backgroundColor: '#888' }} /> Taken
                    </span>
                    <span style={styles.legendItem}>
                      <span style={{ ...styles.legendDot, backgroundColor: '#b03030' }} /> Available
                    </span>
                  </div>
                  <GroupCowDiagram
                    selectedCuts={{}}
                    othersQty={group.othersClaimedQty || {}}
                  />
                  <div style={styles.joinRow}>
                    <button
                      style={{ ...styles.joinBtn, ...(joinLoading ? styles.joinBtnDisabled : {}) }}
                      onClick={handleJoin}
                      disabled={joinLoading}
                    >
                      {joinLoading ? 'Joining...' : 'Join Group'}
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
                      <p style={styles.memberCuts}>{m.claimedCuts || 'No cuts selected yet'}</p>
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
                    {codeCopied ? 'Copied!' : 'Copy'}
                  </button>
                  {group.isCreator && (
                    <button
                      style={{ ...styles.regenBtn, ...(regenLoading ? styles.regenBtnDisabled : {}) }}
                      onClick={handleRegenCode}
                      disabled={regenLoading}
                    >
                      {regenLoading ? 'Regenerating...' : 'Regenerate'}
                    </button>
                  )}
                </div>
                <p style={styles.inviteHint}>Share this code with friends so they can find and join this group.</p>
              </div>
            )}

            {/* Leave group */}
            {group.alreadyJoined && (
              <div style={styles.leaveRow}>
                <button
                  style={{ ...styles.leaveBtn, ...(leaveLoading ? styles.leaveBtnDisabled : {}) }}
                  onClick={handleLeave}
                  disabled={leaveLoading}
                >
                  {leaveLoading ? 'Leaving...' : 'Leave Group'}
                </button>
              </div>
            )}

          </div>

          {/* Right: matching farms */}
          <div style={styles.rightPanel}>

            {/* Matching Farms */}
            {farms && (
              <div style={styles.membersCard}>
                <h2 style={styles.sectionTitle}>Matching Farms</h2>

                <div style={styles.farmsSubheading}>
                  <span style={styles.perfectBadge}>Perfect Match</span>
                  <span style={styles.farmsSub}>
                    {farms.perfectMatches.length === 0
                      ? 'No farms match all certifications.'
                      : `${farms.perfectMatches.length} farm${farms.perfectMatches.length > 1 ? 's' : ''} meet${farms.perfectMatches.length === 1 ? 's' : ''} all requirements`}
                  </span>
                </div>
                {farms.perfectMatches.length > 0 && (
                  <div style={styles.farmList}>
                    {farms.perfectMatches.map((f) => (
                      <div key={f.sellerId} style={{ ...styles.farmCard, borderLeft: '4px solid #2e7d32' }}>
                        <p style={styles.farmShop}>{f.shopName || 'Unnamed Farm'}</p>
                        <p style={styles.farmName}>{f.sellerName}</p>
                        <p style={styles.farmContact}>{f.email}</p>
                        {f.phoneNumber && <p style={styles.farmContact}>{f.phoneNumber}</p>}
                        {f.certifications.length > 0 && (
                          <div style={styles.farmCerts}>
                            {f.certifications.map((c) => (
                              <span key={c} style={{ ...styles.badge, ...(BADGE_STYLES[c] || { backgroundColor: '#eee', color: '#555' }), fontSize: '10px' }}>
                                {CERT_LABELS[c] || c}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}

                {farms.partialMatches.length > 0 && (
                  <>
                    <div style={{ ...styles.farmsSubheading, marginTop: '16px' }}>
                      <span style={styles.partialBadge}>Partial Match</span>
                      <span style={styles.farmsSub}>
                        {farms.partialMatches.length} farm{farms.partialMatches.length > 1 ? 's' : ''} with some matching certifications
                      </span>
                    </div>
                    <div style={styles.farmList}>
                      {farms.partialMatches.map((f) => (
                        <div key={f.sellerId} style={{ ...styles.farmCard, borderLeft: '4px solid #f9a825' }}>
                          <p style={styles.farmShop}>{f.shopName || 'Unnamed Farm'}</p>
                          <p style={styles.farmName}>{f.sellerName}</p>
                          <p style={styles.farmContact}>{f.email}</p>
                          {f.phoneNumber && <p style={styles.farmContact}>{f.phoneNumber}</p>}
                          <p style={styles.matchScore}>{f.matchCount} of {f.totalRequired} certs matched</p>
                          {f.certifications.length > 0 && (
                            <div style={styles.farmCerts}>
                              {f.certifications.map((c) => (
                                <span key={c} style={{ ...styles.badge, ...(BADGE_STYLES[c] || { backgroundColor: '#eee', color: '#555' }), fontSize: '10px' }}>
                                  {CERT_LABELS[c] || c}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </div>
            )}

          </div>
        </div>

      </div>
    </div>
  );
}

const styles = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  banner: { backgroundColor: BUYER_COLOR, padding: '40px 0' },
  bannerInner: {
    maxWidth: '1600px', margin: '0 auto', padding: '0 48px',
    display: 'flex', alignItems: 'flex-start', gap: '24px',
  },
  backBtn: {
    background: 'none', border: '2px solid rgba(255,255,255,0.6)',
    borderRadius: '6px', color: 'white', fontSize: '14px', fontWeight: '600',
    padding: '6px 14px', cursor: 'pointer', flexShrink: 0, marginTop: '8px',
  },
  groupIdLine: {
    fontSize: '12px', color: 'rgba(255,255,255,0.65)',
    fontWeight: '600', letterSpacing: '0.05em', marginBottom: '4px',
  },
  groupName: {
    fontSize: '26px', fontWeight: '700', color: 'white', margin: '0 0 10px 0',
  },
  certBadges: { display: 'flex', gap: '6px', flexWrap: 'wrap' },
  badge: {
    display: 'inline-block', padding: '3px 10px', borderRadius: '99px',
    fontSize: '11px', fontWeight: '700', letterSpacing: '0.05em',
    textTransform: 'uppercase',
  },
  content: {
    maxWidth: '1600px', margin: '0 auto', padding: '32px 48px 72px',
    display: 'flex', flexDirection: 'column', gap: '24px',
  },
  mainLayout: {
    display: 'flex', gap: '24px', alignItems: 'flex-start',
  },
  leftPanel: { flex: 3, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '16px' },
  rightPanel: { flex: 2, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '16px' },
  errorBox: {
    backgroundColor: '#fee', color: '#c00',
    padding: '12px 16px', borderRadius: '6px', fontSize: '14px',
  },
  successBox: {
    backgroundColor: '#e8f5e9', color: '#2e7d32',
    padding: '12px 16px', borderRadius: '6px', fontSize: '14px', fontWeight: '600',
  },
  diagramCard: {
    backgroundColor: 'white', borderRadius: '10px', padding: '24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0',
  },
  sectionTitle: {
    fontSize: '18px', fontWeight: '700', color: BROWN, margin: '0 0 14px 0',
  },
  legend: {
    display: 'flex', gap: '20px', marginBottom: '14px',
    fontSize: '13px', color: '#555', flexWrap: 'wrap',
  },
  legendItem: { display: 'flex', alignItems: 'center', gap: '6px' },
  legendDot: {
    width: '12px', height: '12px', borderRadius: '50%',
    display: 'inline-block', opacity: 0.85,
  },
  cutsTextRow: { marginTop: '14px', fontSize: '14px', lineHeight: 1.6 },
  cutsLabel: {
    fontSize: '11px', fontWeight: '700', letterSpacing: '0.07em',
    textTransform: 'uppercase', color: BUYER_COLOR, marginRight: '6px',
  },
  cutName: { fontWeight: '600', color: '#222', fontSize: '14px' },
  saveRow: {
    marginTop: '18px', display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap',
  },
  saveBtn: {
    padding: '11px 28px', backgroundColor: BUYER_COLOR, color: 'white',
    border: 'none', borderRadius: '6px', fontSize: '14px', fontWeight: '700', cursor: 'pointer',
  },
  saveBtnDisabled: { opacity: 0.6, cursor: 'not-allowed' },
  saveHint: { fontSize: '12px', color: '#888', fontStyle: 'italic' },
  emptyText: { fontSize: '14px', color: '#bbb', fontStyle: 'italic', margin: 0 },
  membersCard: {
    backgroundColor: 'white', borderRadius: '10px', padding: '20px 24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0',
  },
  memberGrid: {
    display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '12px',
  },
  memberCard: {
    backgroundColor: '#f9f7f5', borderRadius: '8px', padding: '12px 16px',
    border: '1px solid #e8e4e0',
  },
  memberName: { fontSize: '15px', fontWeight: '700', color: '#222', margin: '0 0 4px 0' },
  memberCuts: { fontSize: '13px', color: '#555', margin: 0 },
  joinRow: { marginTop: '18px' },
  joinBtn: {
    padding: '11px 28px', backgroundColor: BUYER_COLOR, color: 'white',
    border: 'none', borderRadius: '6px', fontSize: '14px', fontWeight: '700', cursor: 'pointer',
  },
  joinBtnDisabled: { opacity: 0.6, cursor: 'not-allowed' },
  inviteCard: {
    backgroundColor: 'white', borderRadius: '10px', padding: '20px 24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0',
  },
  inviteLabel: {
    fontSize: '11px', fontWeight: '700', letterSpacing: '0.07em',
    textTransform: 'uppercase', color: BUYER_COLOR, margin: '0 0 10px 0',
  },
  inviteRow: { display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' },
  inviteCode: {
    fontFamily: 'monospace', fontSize: '22px', fontWeight: '700',
    letterSpacing: '0.15em', color: '#222', backgroundColor: '#f5f5f0',
    padding: '6px 16px', borderRadius: '6px', border: '1px solid #e0dbd5',
  },
  copyBtn: {
    padding: '7px 16px', backgroundColor: 'white', color: BUYER_COLOR,
    border: `2px solid ${BUYER_COLOR}`, borderRadius: '6px',
    fontSize: '13px', fontWeight: '600', cursor: 'pointer',
  },
  regenBtn: {
    padding: '7px 16px', backgroundColor: 'white', color: '#888',
    border: '2px solid #ccc', borderRadius: '6px',
    fontSize: '13px', fontWeight: '600', cursor: 'pointer',
  },
  regenBtnDisabled: { opacity: 0.6, cursor: 'not-allowed' },
  inviteHint: { fontSize: '12px', color: '#999', margin: '10px 0 0 0', fontStyle: 'italic' },
  farmsSubheading: { display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' },
  farmsSub: { fontSize: '13px', color: '#777' },
  perfectBadge: {
    backgroundColor: '#e8f5e9', color: '#2e7d32',
    fontSize: '11px', fontWeight: '700', letterSpacing: '0.05em',
    textTransform: 'uppercase', padding: '3px 10px', borderRadius: '99px',
  },
  partialBadge: {
    backgroundColor: '#fff8e1', color: '#f57f17',
    fontSize: '11px', fontWeight: '700', letterSpacing: '0.05em',
    textTransform: 'uppercase', padding: '3px 10px', borderRadius: '99px',
  },
  farmList: { display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '8px' },
  farmGrid: {
    display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '12px',
  },
  farmCard: {
    backgroundColor: 'white', borderRadius: '8px', padding: '16px 20px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0',
  },
  farmShop: { fontSize: '15px', fontWeight: '700', color: '#222', margin: '0 0 2px 0' },
  farmName: { fontSize: '13px', color: '#555', margin: '0 0 6px 0' },
  farmContact: { fontSize: '12px', color: '#777', margin: '0 0 2px 0' },
  farmCerts: { display: 'flex', gap: '4px', flexWrap: 'wrap', marginTop: '8px' },
  matchScore: {
    fontSize: '12px', fontWeight: '600', color: '#f57f17', margin: '4px 0 0 0',
  },
  leaveRow: {},
  leaveBtn: {
    padding: '10px 24px', backgroundColor: 'white', color: '#c0392b',
    border: '2px solid #c0392b', borderRadius: '6px', fontSize: '14px',
    fontWeight: '600', cursor: 'pointer',
  },
  leaveBtnDisabled: { opacity: 0.6, cursor: 'not-allowed' },
};

export default GroupDetailPage;
