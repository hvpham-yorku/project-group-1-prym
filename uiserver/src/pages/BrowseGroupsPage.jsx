//page where buyers can browse and join existing groups, also has cert filtering
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAvailableGroups, getMyGroups, joinGroup } from '../api/groups';

const BUYER_COLOR = '#4a7c59';

const CERT_OPTIONS = [
  { value: 'KOSHER',                   label: 'Kosher' },
  { value: 'HALAL',                    label: 'Halal' },
  { value: 'ORGANIC',                  label: 'Organic' },
  { value: 'GRASS_FED',                label: 'Grass-Fed' },
  { value: 'NON_GMO',                  label: 'Non-GMO' },
  { value: 'ANIMAL_WELFARE_APPROVED',  label: 'Animal Welfare Approved' },
  { value: 'CONVENTIONAL',             label: 'Conventional' },
];

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

function BrowseGroupsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [groups, setGroups]         = useState([]);
  const [loading, setLoading]       = useState(true);
  const [alreadyInGroup, setAlreadyInGroup] = useState(false);
  const [selectedCerts, setSelectedCerts] = useState(new Set());
  const [joiningId, setJoiningId]   = useState(null);
  const [error, setError]           = useState('');

  useEffect(() => {
    const load = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        const [available, mine] = await Promise.all([
          getAvailableGroups(user.id),
          getMyGroups(user.id),
        ]);
        setAlreadyInGroup(mine.length > 0);
        setGroups(available);
      } catch (err) {
        setError(err.message || 'Failed to load groups.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user?.id]);

  const toggleCert = (cert) => {
    setSelectedCerts((prev) => {
      const next = new Set(prev);
      if (next.has(cert)) next.delete(cert);
      else next.add(cert);
      return next;
    });
  };

  const filtered = groups.filter((g) => {
    if (selectedCerts.size === 0) return true;
    return [...selectedCerts].every((c) => g.certifications.includes(c));
  });

  const handleJoin = async (groupId) => {
    setJoiningId(groupId);
    setError('');
    try {
      await joinGroup(user.id, groupId);
      navigate(`/buyer/groups/${groupId}`);
    } catch (err) {
      setError(err.message || 'Failed to join group.');
      setJoiningId(null);
    }
  };

  return (
    <div style={styles.page}>

      {/* Header */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <button style={styles.backBtn} onClick={() => navigate('/buyer/profile')}>
            ← Back
          </button>
          <div>
            <h1 style={styles.bannerName}>Find a Group to Join</h1>
            <p style={styles.bannerSub}>
              Browse existing groups and join one to start claiming your cuts.
            </p>
          </div>
        </div>
      </div>

      <div style={styles.content}>

        {error && <div style={styles.errorBox}>{error}</div>}

        {alreadyInGroup && (
          <div style={styles.warningBanner}>
            You are already in a group. Leave it before joining another.{' '}
            <button style={styles.inlineLink} onClick={() => navigate('/buyer/profile')}>
              View my group
            </button>
          </div>
        )}

        {/* Cert filter */}
        <div style={styles.filterCard}>
          <p style={styles.filterLabel}>Filter by certification</p>
          <div style={styles.filterRow}>
            {CERT_OPTIONS.map(({ value, label }) => (
              <label key={value} style={styles.certCheckbox}>
                <input
                  type="checkbox"
                  checked={selectedCerts.has(value)}
                  onChange={() => toggleCert(value)}
                  style={{ marginRight: '6px', accentColor: BUYER_COLOR }}
                />
                {label}
              </label>
            ))}
          </div>
        </div>

        {loading ? (
          <p style={styles.emptyText}>Loading groups...</p>
        ) : filtered.length === 0 ? (
          <div style={styles.emptyCard}>
            <p style={styles.emptyCardTitle}>No groups found.</p>
            <p style={styles.emptyCardSub}>
              {groups.length === 0
                ? 'No groups exist yet.'
                : 'Try adjusting your certification filters.'}
            </p>
            <button style={styles.linkBtn} onClick={() => navigate('/buyer/create-group')}>
              Start your own group →
            </button>
          </div>
        ) : (
          <div style={styles.list}>
            {filtered.map((g) => (
              <div
                key={g.groupId}
                style={{ ...styles.row, cursor: alreadyInGroup ? 'default' : 'pointer' }}
                onClick={alreadyInGroup ? undefined : () => navigate(`/buyer/groups/${g.groupId}`)}
              >
                <div style={styles.rowLeft}>
                  <div>
                    <span style={styles.groupIdChip}>#{g.groupId}</span>
                    <span style={styles.groupName}>{g.groupName}</span>
                  </div>
                  <div style={styles.certBadges}>
                    {g.certifications.map((c) =>
                      CERT_LABELS[c] ? (
                        <span key={c} style={{ ...styles.badge, ...(BADGE_STYLES[c] || {}) }}>
                          {CERT_LABELS[c]}
                        </span>
                      ) : null
                    )}
                  </div>
                </div>

                <div style={styles.rowMid}>
                  <span style={styles.memberCount}>
                    {g.memberCount} member{g.memberCount !== 1 ? 's' : ''}
                  </span>
                </div>

                <div style={styles.rowRight}>
                  {alreadyInGroup ? (
                    <span style={styles.lockedNote}>Already in a group</span>
                  ) : (
                    <button
                      style={{
                        ...styles.joinBtn,
                        ...(joiningId === g.groupId ? styles.joinBtnDisabled : {}),
                      }}
                      onClick={(e) => { e.stopPropagation(); handleJoin(g.groupId); }}
                      disabled={joiningId === g.groupId}
                    >
                      {joiningId === g.groupId ? 'Joining...' : 'Join'}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  );
}

const styles = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0' },
  banner: { backgroundColor: BUYER_COLOR, padding: '40px 0' },
  bannerInner: {
    maxWidth: '900px', margin: '0 auto', padding: '0 48px',
    display: 'flex', alignItems: 'flex-start', gap: '20px',
  },
  backBtn: {
    background: 'none', border: '2px solid rgba(255,255,255,0.6)',
    borderRadius: '6px', color: 'white', fontSize: '14px', fontWeight: '600',
    padding: '6px 14px', cursor: 'pointer', flexShrink: 0, marginTop: '4px',
  },
  bannerName: {
    fontSize: '26px', fontWeight: '700', color: 'white', margin: '0 0 6px 0',
  },
  bannerSub: { fontSize: '14px', color: 'rgba(255,255,255,0.8)', margin: 0 },
  content: {
    maxWidth: '900px', margin: '0 auto', padding: '32px 48px 64px',
    display: 'flex', flexDirection: 'column', gap: '16px',
  },
  errorBox: {
    backgroundColor: '#fee', color: '#c00',
    padding: '12px 16px', borderRadius: '6px', fontSize: '14px',
  },
  warningBanner: {
    backgroundColor: '#fff8e1', border: '1px solid #ffe082',
    borderRadius: '8px', padding: '12px 18px', fontSize: '14px', color: '#795548',
  },
  inlineLink: {
    background: 'none', border: 'none', color: BUYER_COLOR,
    fontSize: '14px', fontWeight: '600', cursor: 'pointer',
    padding: 0, textDecoration: 'underline',
  },
  filterCard: {
    backgroundColor: 'white', borderRadius: '8px', padding: '18px 24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0',
  },
  filterLabel: {
    fontSize: '11px', fontWeight: '700', letterSpacing: '0.07em',
    textTransform: 'uppercase', color: BUYER_COLOR, margin: '0 0 10px 0',
  },
  filterRow: { display: 'flex', gap: '18px', flexWrap: 'wrap' },
  certCheckbox: {
    display: 'flex', alignItems: 'center', fontSize: '14px', color: '#333', cursor: 'pointer',
  },
  emptyText: { fontSize: '14px', color: '#bbb', fontStyle: 'italic' },
  emptyCard: {
    backgroundColor: 'white', borderRadius: '8px', padding: '36px 24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0', textAlign: 'center',
  },
  emptyCardTitle: {
    fontSize: '15px', color: '#555', margin: '0 0 8px 0', fontWeight: '600',
  },
  emptyCardSub: { fontSize: '13px', color: '#888', margin: '0 0 16px 0' },
  linkBtn: {
    background: 'none', border: 'none', color: BUYER_COLOR,
    fontSize: '14px', fontWeight: '600', cursor: 'pointer', padding: 0, textDecoration: 'underline',
  },
  list: { display: 'flex', flexDirection: 'column', gap: '8px' },
  row: {
    backgroundColor: 'white', borderRadius: '8px', padding: '16px 20px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)', border: '1px solid #e8e4e0',
    display: 'flex', alignItems: 'center', gap: '16px',
  },
  rowLeft: {
    flex: 3, display: 'flex', flexDirection: 'column', gap: '6px',
  },
  groupIdChip: {
    fontSize: '11px', fontWeight: '700', color: '#999',
    marginRight: '8px', letterSpacing: '0.05em',
  },
  groupName: { fontSize: '15px', fontWeight: '700', color: '#222' },
  certBadges: { display: 'flex', gap: '4px', flexWrap: 'wrap' },
  badge: {
    display: 'inline-block', padding: '2px 8px', borderRadius: '99px',
    fontSize: '10px', fontWeight: '700', letterSpacing: '0.05em', textTransform: 'uppercase',
  },
  rowMid: { flex: 1 },
  memberCount: { fontSize: '13px', color: '#666' },
  rowRight: { flex: 1, display: 'flex', justifyContent: 'flex-end' },
  joinBtn: {
    padding: '8px 18px', backgroundColor: BUYER_COLOR, color: 'white',
    border: 'none', borderRadius: '6px', fontSize: '13px', fontWeight: '600', cursor: 'pointer',
  },
  joinBtnDisabled: { opacity: 0.6, cursor: 'not-allowed' },
  lockedNote: { fontSize: '12px', color: '#bbb', fontStyle: 'italic' },
};

export default BrowseGroupsPage;
