import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getFarm, saveFarm, getSavedFarms, removeSavedFarm, getCowTypes } from '../api/farm';
import { submitRating } from '../api/ratings';

const GREEN = '#4a7c59';
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
  KOSHER:                  'Kosher',
  HALAL:                   'Halal',
  ORGANIC:                 'Organic',
  GRASS_FED:               'Grass-Fed',
  NON_GMO:                 'Non-GMO',
  ANIMAL_WELFARE_APPROVED: 'Animal Welfare Approved',
  CONVENTIONAL:            'Conventional',
};

//Individual farm detail page. Shows a single farm's info, certs, and cows.
//The farmname param is actually the farm id from the URL, naming is a bit misleading
function FarmListing() {
  const { user } = useAuth();
  const navigate = useNavigate();
  let { farmname } = useParams();

  const [farm, setFarm] = useState(null);
  const [savedIds, setSavedIds] = useState(new Set());
  const [cowTypes, setCowTypes] = useState([]);

  const [showRatingModal, setShowRatingModal] = useState(false);
  const [ratingCode, setRatingCode] = useState('');
  const [ratingScore, setRatingScore] = useState(0);
  const [hoveredStar, setHoveredStar] = useState(0);
  const [ratingError, setRatingError] = useState('');
  const [ratingSuccess, setRatingSuccess] = useState('');
  const [ratingSubmitting, setRatingSubmitting] = useState(false);

  //fetch the farm data when the component loads or the id changes
  useEffect(() => {
    getFarm(farmname).then((f) => {
      setFarm(f);
      if (f) getCowTypes(f.id).then(setCowTypes).catch(console.error);
    }).catch(console.error);
    getSavedFarms().then((saved) => setSavedIds(new Set(saved.map((f) => f.id)))).catch(console.error);
  }, [farmname]);

  async function handleSave(e) {
    e.preventDefault();
    await saveFarm(farm);
    setSavedIds((prev) => new Set([...prev, farm.id]));
  }

  function closeRatingModal() {
    setShowRatingModal(false);
    setRatingCode('');
    setRatingScore(0);
    setRatingError('');
    setRatingSuccess('');
  }

  async function handleSubmitRating() {
    setRatingError('');
    setRatingSuccess('');
    if (!ratingCode.trim()) { setRatingError('Please enter the code from the seller.'); return; }
    if (ratingScore === 0) { setRatingError('Please select a star rating.'); return; }
    setRatingSubmitting(true);
    try {
      const result = await submitRating(user.id, ratingCode.trim(), ratingScore);
      if (result.error) {
        setRatingError(result.error);
      } else {
        setRatingSuccess('Rating submitted! Thank you.');
      }
    } catch {
      setRatingError('Failed to submit rating. Please try again.');
    } finally {
      setRatingSubmitting(false);
    }
  }

  async function handleUnsave(e) {
    e.preventDefault();
    await removeSavedFarm(farm);
    setSavedIds((prev) => { const next = new Set(prev); next.delete(farm.id); return next; });
  }

  if (!farm) return <div style={styles.loadingPage}><p style={styles.loadingText}>Loading...</p></div>;

  const isSaved = savedIds.has(farm.id);
  const rating = Math.round(farm.averageRating);

  return (
    <div style={styles.page}>
      {/* ── Banner ── */}
      <div style={styles.banner}>
        <div style={styles.bannerInner}>
          <button style={styles.backBtn} onClick={() => navigate(-1)}>← Back</button>

          <div style={{ flex: 1 }}>
            <h1 style={styles.shopName}>{farm.shopName || 'Unnamed Farm'}</h1>
            <div style={styles.ratingRow}>
              <span style={styles.stars}>{'★'.repeat(rating)}{'☆'.repeat(5 - rating)}</span>
              <span style={styles.ratingText}>
                {farm.averageRating.toFixed(1)} · {farm.totalRatings} rating{farm.totalRatings !== 1 ? 's' : ''}
              </span>
            </div>
            {farm.certifications.length > 0 && (
              <div style={styles.certBadges}>
                {farm.certifications.map((c) => (
                  <span key={c.id} style={{ ...styles.badge, ...(BADGE_STYLES[c.name] || { backgroundColor: '#eee', color: '#555' }) }}>
                    {CERT_LABELS[c.name] || c.name}
                  </span>
                ))}
              </div>
            )}
          </div>

          <button
            style={isSaved ? styles.unsaveBtn : styles.saveBtn}
            onClick={isSaved ? handleUnsave : handleSave}
          >
            {isSaved ? 'Saved' : 'Save Farm'}
          </button>
        </div>
      </div>

      {/* ── Content ── */}
      <div style={styles.content}>

        {/* ── Info row ── */}
        <div style={styles.infoRow}>
          {/* Description */}
          <div style={styles.descCard}>
            <h2 style={styles.cardTitle}>About This Farm</h2>
            <p style={styles.descText}>{farm.description || 'No description provided.'}</p>
          </div>

          {/* Details sidebar */}
          <div style={styles.detailsCard}>
            <h2 style={styles.cardTitle}>Farm Details</h2>
            {(farm.user?.firstName || farm.user?.lastName) && (
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Owner</span>
                <span style={styles.detailValue}>{farm.user.firstName} {farm.user.lastName}</span>
              </div>
            )}
            {farm.shopAddress && (
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Address</span>
                <span style={styles.detailValue}>{farm.shopAddress}</span>
              </div>
            )}
            {farm.user?.email && (
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Email</span>
                <span style={styles.detailValue}>{farm.user.email}</span>
              </div>
            )}
            {farm.user?.phoneNumber && (
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Phone</span>
                <span style={styles.detailValue}>{farm.user.phoneNumber}</span>
              </div>
            )}
          </div>
        </div>

        {/* ── Available Cattle ── */}
        <div style={styles.cattleSection}>
          <h2 style={styles.sectionTitle}>Available Cattle</h2>
          {cowTypes.length === 0 ? (
            <p style={styles.emptyText}>No cattle listed yet.</p>
          ) : (
            <div style={styles.cowGrid}>
              {cowTypes.map((ct) => (
                <div key={ct.id} style={styles.cowCard}>
                  <div style={styles.cowBreed}>{ct.breed.replace(/_/g, ' ')}</div>
                  <p style={styles.cowDesc}>{ct.description}</p>
                  <div style={styles.cowMeta}>
                    <span style={styles.cowPrice}>${ct.pricePerPound.toFixed(2)}/lb</span>
                    <span style={styles.cowAvail}>{ct.availableCount} available</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

      {/* ── Bottom action bar ── */}
      <div style={styles.bottomBar}>
        <Link to="/buyer/farmlistings" style={{ textDecoration: 'none' }}>
          <button style={styles.ghostBtn}>← All Farms</button>
        </Link>
        <button style={styles.rateBtn} onClick={() => setShowRatingModal(true)}>Rate Farm</button>
      </div>
      {/* ── Rating Modal ── */}
      {showRatingModal && (
        <div style={styles.modalOverlay}>
          <div style={styles.ratingModal}>
            <h2 style={styles.modalTitle}>Rate This Farm</h2>
            <p style={styles.modalSubtitle}>Enter the code you received from the seller</p>
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
                  style={{ fontSize: '40px', cursor: 'pointer', color: star <= (hoveredStar || ratingScore) ? '#f5a623' : '#ccc', transition: 'color 0.1s' }}
                  onMouseEnter={() => setHoveredStar(star)}
                  onMouseLeave={() => setHoveredStar(0)}
                  onClick={() => setRatingScore(star)}
                >★</span>
              ))}
            </div>
            <p style={styles.scoreLabel}>
              {ratingScore > 0 ? `You selected: ${ratingScore} star${ratingScore !== 1 ? 's' : ''}` : 'Select a rating'}
            </p>
            {ratingError && <p style={styles.modalError}>{ratingError}</p>}
            {ratingSuccess && <p style={styles.modalSuccess}>{ratingSuccess}</p>}
            <div style={styles.modalButtons}>
              <button style={styles.cancelBtn} onClick={closeRatingModal}>Cancel</button>
              <button
                style={{ ...styles.submitBtn, ...(ratingSubmitting ? { opacity: 0.6, cursor: 'not-allowed' } : {}) }}
                onClick={handleSubmitRating}
                disabled={ratingSubmitting}
              >{ratingSubmitting ? 'Submitting...' : 'Submit Rating'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { minHeight: '100vh', backgroundColor: '#f5f5f0', display: 'flex', flexDirection: 'column' },
  loadingPage: { minHeight: '100vh', backgroundColor: '#f5f5f0', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  loadingText: { color: '#888', fontSize: '16px' },

  // Banner
  banner: { backgroundColor: GREEN, padding: '40px 0' },
  bannerInner: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '0 48px',
    display: 'flex',
    alignItems: 'center',
    gap: '24px',
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
  },
  shopName: { fontSize: '28px', fontWeight: '700', color: 'white', margin: '0 0 8px 0' },
  ratingRow: { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' },
  stars: { color: '#fdd835', fontSize: '20px', letterSpacing: '2px' },
  ratingText: { color: 'rgba(255,255,255,0.85)', fontSize: '14px', fontWeight: '500' },
  certBadges: { display: 'flex', gap: '6px', flexWrap: 'wrap' },
  badge: {
    display: 'inline-block',
    padding: '3px 10px',
    borderRadius: '99px',
    fontSize: '11px',
    fontWeight: '700',
    letterSpacing: '0.05em',
    textTransform: 'uppercase',
  },
  saveBtn: {
    flexShrink: 0,
    padding: '10px 20px',
    backgroundColor: 'white',
    color: GREEN,
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
  },
  unsaveBtn: {
    flexShrink: 0,
    padding: '10px 20px',
    backgroundColor: 'rgba(255,255,255,0.15)',
    color: 'white',
    border: '2px solid rgba(255,255,255,0.6)',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
  },

  // Content area
  content: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '32px 48px 48px',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
    flex: 1,
    width: '100%',
    boxSizing: 'border-box',
  },
  infoRow: { display: 'flex', gap: '24px', alignItems: 'flex-start' },

  // Description card
  descCard: {
    flex: 2,
    backgroundColor: 'white',
    borderRadius: '10px',
    padding: '24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    border: '1px solid #e8e4e0',
  },
  cardTitle: { fontSize: '16px', fontWeight: '700', color: BROWN, margin: '0 0 14px 0', textTransform: 'uppercase', letterSpacing: '0.05em' },
  descText: { fontSize: '15px', color: '#444', lineHeight: 1.7, margin: 0 },

  // Details sidebar
  detailsCard: {
    flex: 1,
    minWidth: '220px',
    backgroundColor: 'white',
    borderRadius: '10px',
    padding: '24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    border: '1px solid #e8e4e0',
    display: 'flex',
    flexDirection: 'column',
    gap: '14px',
  },
  detailRow: { display: 'flex', flexDirection: 'column', gap: '2px' },
  detailLabel: { fontSize: '11px', fontWeight: '700', color: GREEN, textTransform: 'uppercase', letterSpacing: '0.06em' },
  detailValue: { fontSize: '14px', color: '#333' },

  // Cattle section
  cattleSection: {
    backgroundColor: 'white',
    borderRadius: '10px',
    padding: '24px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    border: '1px solid #e8e4e0',
  },
  sectionTitle: { fontSize: '18px', fontWeight: '700', color: BROWN, margin: '0 0 18px 0' },
  emptyText: { fontSize: '14px', color: '#bbb', fontStyle: 'italic', margin: 0 },
  cowGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
    gap: '16px',
  },
  cowCard: {
    backgroundColor: '#f9f7f5',
    borderRadius: '8px',
    padding: '18px 20px',
    border: '1px solid #e8e4e0',
    borderTop: `3px solid ${GREEN}`,
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  cowBreed: { fontSize: '17px', fontWeight: '700', color: BROWN, textTransform: 'capitalize' },
  cowDesc: { fontSize: '13px', color: '#666', lineHeight: 1.5, margin: 0, flexGrow: 1 },
  cowMeta: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' },
  cowPrice: { fontSize: '16px', fontWeight: '700', color: GREEN },
  cowAvail: { fontSize: '12px', color: '#888', backgroundColor: '#f0ede9', padding: '3px 8px', borderRadius: '99px' },

  // Bottom bar
  bottomBar: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '0 48px 40px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    boxSizing: 'border-box',
  },
  ghostBtn: {
    padding: '10px 20px',
    backgroundColor: 'white',
    color: BROWN,
    border: `2px solid #d0c9c0`,
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  rateBtn: {
    padding: '10px 24px',
    backgroundColor: GREEN,
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
  },

  // Rating modal
  modalOverlay: {
    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
    alignItems: 'center', justifyContent: 'center', zIndex: 1000,
  },
  ratingModal: {
    backgroundColor: 'white', borderRadius: '12px', padding: '36px',
    width: '400px', boxShadow: '0 8px 32px rgba(0,0,0,0.2)',
    display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px',
  },
  modalTitle: { fontSize: '22px', fontWeight: '700', color: BROWN, margin: 0 },
  modalSubtitle: { fontSize: '14px', color: '#666', margin: 0, textAlign: 'center' },
  codeInput: {
    width: '100%', padding: '10px 14px', border: '2px solid #ddd',
    borderRadius: '6px', fontSize: '15px', boxSizing: 'border-box',
    letterSpacing: '0.05em', outline: 'none',
  },
  starRow: { display: 'flex', gap: '8px' },
  scoreLabel: { fontSize: '14px', color: '#555', margin: 0 },
  modalError: { color: '#c00', fontSize: '14px', margin: 0 },
  modalSuccess: { color: GREEN, fontSize: '14px', fontWeight: '600', margin: 0 },
  modalButtons: { display: 'flex', gap: '12px', width: '100%' },
  cancelBtn: {
    flex: 1, padding: '10px', backgroundColor: 'white', color: BROWN,
    border: `2px solid ${BROWN}`, borderRadius: '6px', fontSize: '15px',
    fontWeight: '600', cursor: 'pointer',
  },
  submitBtn: {
    flex: 1, padding: '10px', backgroundColor: GREEN, color: 'white',
    border: 'none', borderRadius: '6px', fontSize: '15px', fontWeight: '600', cursor: 'pointer',
  },
};

export default FarmListing;
