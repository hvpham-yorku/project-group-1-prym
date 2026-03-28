import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyGroups, createGroup } from '../api/groups';

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

//Page for creating a new buyer group. Lets you pick a name and
//certifications, then sends it off to the backend. Also checks
//if you're already in a group first since we only allow one at a time.
function CreateGroupPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [name, setName]               = useState('');
  const [selectedCerts, setSelectedCerts] = useState(new Set());
  const [loading, setLoading]         = useState(false);
  const [checkingMembership, setCheckingMembership] = useState(true);
  const [alreadyInGroup, setAlreadyInGroup] = useState(false);
  const [error, setError]             = useState('');

  // Block the page if the user is already in a group
  useEffect(() => {
    if (!user?.id) return;
    getMyGroups(user.id)
      .then((groups) => setAlreadyInGroup(groups.length > 0))
      .catch(() => {})
      .finally(() => setCheckingMembership(false));
  }, [user?.id]);

  //flips a cert on or off in the set when you click the checkbox
  const toggleCert = (cert) => {
    setSelectedCerts((prev) => {
      const next = new Set(prev);
      if (next.has(cert)) next.delete(cert);
      else next.add(cert);
      return next;
    });
  };

  //fires on form submit, joins selected certs into a comma string and
  //sends the create request, then navigates to the new group page
  const handleCreate = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please enter a group name.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const certStr = [...selectedCerts].join(',');
      const group = await createGroup(user.id, name.trim(), certStr);
      navigate(`/buyer/groups/${group.groupId}`);
    } catch (err) {
      setError(err.message || 'Failed to create group.');
      setLoading(false);
    }
  };

  if (checkingMembership) {
    return (
      <div style={styles.page}>
        <p style={{ padding: '48px', color: '#666' }}>Loading...</p>
      </div>
    );
  }

  return (
    <div style={styles.page}>

      {/* Header */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <button style={styles.backBtn} onClick={() => navigate('/buyer/profile')}>
            ← Back
          </button>
          <div>
            <h1 style={styles.bannerTitle}>Create a New Group</h1>
            <p style={styles.bannerSub}>
              Give your group a name and choose any certifications you require.
            </p>
          </div>
        </div>
      </div>

      <div style={styles.content}>

        {alreadyInGroup ? (
          <div style={styles.blockedCard}>
            <p style={styles.blockedTitle}>You are already in a group.</p>
            <p style={styles.blockedSub}>
              You can only be in one group at a time. Leave your current group to create a new one.
            </p>
            <button style={styles.linkBtn} onClick={() => navigate('/buyer/profile')}>
              Go to my profile →
            </button>
          </div>
        ) : (
          <form onSubmit={handleCreate} style={styles.form}>

            {error && <div style={styles.errorBox}>{error}</div>}

            {/* Group name */}
            <div style={styles.field}>
              <label style={styles.label} htmlFor="groupName">Group Name</label>
              <input
                id="groupName"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Halal Beef Club"
                style={styles.input}
                maxLength={80}
                required
              />
            </div>

            {/* Certifications */}
            <div style={styles.field}>
              <label style={styles.label}>
                Certifications <span style={styles.optional}>(optional)</span>
              </label>
              <p style={styles.fieldHint}>
                Select any certifications this group requires. These will be shown on the group page.
              </p>
              <div style={styles.certGrid}>
                {CERT_OPTIONS.map(({ value, label }) => (
                  <label key={value} style={styles.certOption}>
                    <input
                      type="checkbox"
                      checked={selectedCerts.has(value)}
                      onChange={() => toggleCert(value)}
                      style={{ marginRight: '8px', accentColor: BUYER_COLOR, width: 16, height: 16 }}
                    />
                    {label}
                  </label>
                ))}
              </div>
            </div>

            <button
              type="submit"
              style={{ ...styles.createBtn, ...(loading ? styles.createBtnDisabled : {}) }}
              disabled={loading}
            >
              {loading ? 'Creating...' : 'Create Group'}
            </button>

          </form>
        )}

      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: '100vh',
    backgroundColor: '#f5f5f0',
  },
  banner: {
    backgroundColor: BUYER_COLOR,
    padding: '40px 0',
  },
  bannerInner: {
    maxWidth: '680px',
    margin: '0 auto',
    padding: '0 48px',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '20px',
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
    flexShrink: 0,
    marginTop: '4px',
  },
  bannerTitle: {
    fontSize: '26px',
    fontWeight: '700',
    color: 'white',
    margin: '0 0 6px 0',
  },
  bannerSub: {
    fontSize: '14px',
    color: 'rgba(255,255,255,0.8)',
    margin: 0,
  },
  content: {
    maxWidth: '680px',
    margin: '0 auto',
    padding: '40px 48px 80px',
  },
  blockedCard: {
    backgroundColor: 'white',
    borderRadius: '8px',
    padding: '36px 28px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    border: '1px solid #e8e4e0',
    textAlign: 'center',
  },
  blockedTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#333',
    margin: '0 0 8px 0',
  },
  blockedSub: {
    fontSize: '13px',
    color: '#888',
    margin: '0 0 20px 0',
  },
  linkBtn: {
    background: 'none',
    border: 'none',
    color: BUYER_COLOR,
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    padding: 0,
    textDecoration: 'underline',
  },
  form: {
    backgroundColor: 'white',
    borderRadius: '10px',
    padding: '36px 32px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    border: '1px solid #e8e4e0',
    display: 'flex',
    flexDirection: 'column',
    gap: '28px',
  },
  errorBox: {
    backgroundColor: '#fee',
    color: '#c00',
    padding: '12px 16px',
    borderRadius: '6px',
    fontSize: '14px',
  },
  field: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  label: {
    fontSize: '13px',
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    color: BUYER_COLOR,
  },
  optional: {
    fontWeight: '400',
    textTransform: 'none',
    letterSpacing: 0,
    color: '#aaa',
    fontSize: '12px',
  },
  fieldHint: {
    fontSize: '13px',
    color: '#888',
    margin: '0 0 4px 0',
  },
  input: {
    fontSize: '15px',
    padding: '10px 12px',
    border: '1px solid #d0ccc8',
    borderRadius: '6px',
    outline: 'none',
    color: '#222',
    backgroundColor: '#fafafa',
    width: '100%',
    boxSizing: 'border-box',
  },
  certGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
    gap: '10px',
    marginTop: '4px',
  },
  certOption: {
    display: 'flex',
    alignItems: 'center',
    fontSize: '14px',
    color: '#333',
    cursor: 'pointer',
    padding: '8px 12px',
    borderRadius: '6px',
    border: '1px solid #e0ddd9',
    backgroundColor: '#f9f9f7',
  },
  createBtn: {
    padding: '13px 0',
    backgroundColor: BUYER_COLOR,
    color: 'white',
    border: 'none',
    borderRadius: '7px',
    fontSize: '15px',
    fontWeight: '700',
    cursor: 'pointer',
    width: '100%',
  },
  createBtnDisabled: {
    opacity: 0.6,
    cursor: 'not-allowed',
  },
};

export default CreateGroupPage;
