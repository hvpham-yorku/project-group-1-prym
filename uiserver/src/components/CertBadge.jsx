// Shared certification badge component used on farm listing cards.
// Centralizes badge colors and labels so they don't need to be copy-pasted
// across FarmListingsPage, SavedFarms, and FarmListing.
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

const badgeBase = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  height: 25,
  padding: '4px 10px',
  borderRadius: 99,
  fontSize: '12px',
  fontWeight: '700',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

export default function CertBadge({ certName }) {
  const style = BADGE_STYLES[certName] || { backgroundColor: '#eee', color: '#555' };
  const label = CERT_LABELS[certName] || certName;
  return <span style={{ ...badgeBase, ...style }}>{label}</span>;
}
