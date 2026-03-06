/*
 * GroupCowDiagram — cow diagram for the group detail page.
 * Identical SVG layout to CowDiagram.jsx but supports three fill states:
 *   selectedCuts   { [cutDisplayName]: number }  → GREEN  (my claimed cuts)
 *   othersQty      { [cutDisplayName]: number }  → from 0–2; drives grey overlay + slot limits
 *   everything else                              → RED    (still available)
 *
 * Interactive mode (when onToggle is provided):
 *   - Cuts where othersQty[cut] >= 2 are grey and unclickable.
 *   - Cuts where othersQty[cut] === 1 can be selected but capped at qty 1.
 *   - All other cuts behave like the preferences CowDiagram (qty 1 or 2).
 *
 * Read-only mode (no onToggle): same visual but no click/qty controls.
 *
 * This component is intentionally separate from CowDiagram.jsx.
 */

const RED_HI  = '#b03030';
const GREEN   = '#4a7c59';
const GREY    = '#888888';
const WHITE   = '#ffffff';

const COW_POINTS =
  '473.006,155.496 421.754,102.028 444.061,87.801 445.363,76.713 435.016,76.096 ' +
  '408.182,95.397 374.697,95.426 354.199,82.91 346.857,84.409 346.965,92.546 ' +
  '360.178,107.18 358.436,114.867 202.938,131.59 41.869,127.092 24.512,134.368 ' +
  '11.727,150.614 5.149,188.614 3.987,215.93 0,241.437 1.096,266.842 5.58,267.759 ' +
  '12.098,260.583 17.397,236.746 13.102,203.678 14.596,203.453 24.692,253.795 ' +
  '17.815,297.485 24.094,384.681 24.692,394.337 30.811,399.557 57.866,399.599 ' +
  '66.584,396.962 42.379,357.545 54.387,307.489 60.676,307.5 82.619,374.053 ' +
  '93.93,391.139 99.51,394.574 129.375,394.596 133.207,389.801 107.354,373.92 ' +
  '89.825,319.936 94.219,311.172 104.516,312.128 110.981,327.958 120.346,312.528 ' +
  '129.42,328.024 137.106,311.198 159.729,293.808 240.008,286.12 253.482,302.593 ' +
  '264.838,383.123 270.812,394.694 298.166,394.683 301.115,390.347 284.489,367.206 ' +
  '282.491,323.141 291.469,328.124 300.926,357.395 312.953,395.403 318.346,399.584 ' +
  '342.82,399.599 344.852,394.072 337.697,387.45 314.619,342.349 319.939,295.083 ' +
  '345.799,275.954 383.469,205.985 426.826,185.969 466.697,181.43 470.115,177.724 ' +
  '475.695,161.998';

const UPPER_Y = 75;
const MID_Y   = 228;
const LOWER_Y = 308;

const CUTS = [
  { id: 'Round',         rect: [0,   UPPER_Y, 72,  MID_Y - UPPER_Y], label: [32,  178], size: 11 },
  { id: 'Sirloin',       rect: [72,  UPPER_Y, 63,  MID_Y - UPPER_Y], label: [103, 175], size: 10 },
  { id: 'Short Loin',    rect: [135, UPPER_Y, 65,  MID_Y - UPPER_Y], label: [167, 170], size: 9  },
  { id: 'Rib',           rect: [200, UPPER_Y, 90,  MID_Y - UPPER_Y], label: [245, 172], size: 14 },
  { id: 'Chuck',         rect: [290, UPPER_Y, 80,  MID_Y - UPPER_Y], label: [330, 166], size: 12 },
  { id: 'Neck',          rect: [370, UPPER_Y, 106, MID_Y - UPPER_Y], label: [418, 153], size: 10 },
  { id: 'Flank',         rect: [72,  MID_Y, 128, LOWER_Y - MID_Y],   label: [136, 267], size: 10 },
  { id: 'Plate',         rect: [200, MID_Y, 90,  LOWER_Y - MID_Y],   label: [245, 267], size: 10 },
  { id: 'Brisket',       rect: [290, MID_Y, 80,  LOWER_Y - MID_Y],   label: [330, 267], size: 9  },
  { id: 'Shank (Front)', rect: [15,  LOWER_Y, 120, 97],               label: [68,  355], size: 8  },
  { id: 'Shank (Rear)',  rect: [253, LOWER_Y, 92,  97],               label: [299, 352], size: 8  },
];

const DIVIDERS = [
  [72,  UPPER_Y, 72,  LOWER_Y],
  [135, UPPER_Y, 135, MID_Y  ],
  [200, UPPER_Y, 200, LOWER_Y],
  [290, UPPER_Y, 290, LOWER_Y],
  [370, UPPER_Y, 370, MID_Y  ],
  [72,  MID_Y,   370, MID_Y  ],
];

function getLines(id) {
  if (id === 'Shank (Front)') return ['SHANK', 'FRONT'];
  if (id === 'Shank (Rear)')  return ['SHANK', 'REAR'];
  return id.toUpperCase().split(' ');
}

// ── Qty +/– control (reused from CowDiagram) ─────────────────────────────────
function QtyControl({ qty, maxQty, onMinus, onPlus }) {
  const atMax = qty >= maxQty;
  const BTN = {
    width: 18, height: 18, rx: 3,
    fill: 'rgba(255,255,255,0.18)',
    stroke: 'rgba(255,255,255,0.55)', strokeWidth: 1,
  };
  return (
    <g>
      {/* minus */}
      <rect {...BTN} x={-26} y={-9} fill="transparent" pointerEvents="all"
        style={{ cursor: 'pointer' }}
        onClick={(e) => { e.stopPropagation(); onMinus(); }}
      />
      <rect {...BTN} x={-26} y={-9} pointerEvents="none" />
      <line x1={-22} y1={0} x2={-12} y2={0}
        stroke="white" strokeWidth="2" strokeLinecap="round" pointerEvents="none" />

      {/* qty label */}
      <text x={0} y={4} textAnchor="middle"
        fontSize={11} fill="white" fontWeight="700" pointerEvents="none">
        {qty}
      </text>

      {/* plus */}
      <rect {...BTN} x={8} y={-9} fill="transparent" pointerEvents="all"
        style={{ cursor: atMax ? 'not-allowed' : 'pointer' }}
        onClick={(e) => { e.stopPropagation(); if (!atMax) onPlus(); }}
      />
      <rect {...BTN} x={8} y={-9} pointerEvents="none" />
      <line x1={12} y1={0} x2={22} y2={0}
        stroke={atMax ? 'rgba(255,255,255,0.35)' : 'white'}
        strokeWidth="2" strokeLinecap="round" pointerEvents="none" />
      <line x1={17} y1={-5} x2={17} y2={5}
        stroke={atMax ? 'rgba(255,255,255,0.35)' : 'white'}
        strokeWidth="2" strokeLinecap="round" pointerEvents="none" />
    </g>
  );
}

/*
 * Props:
 *   selectedCuts     { [cutDisplayName]: number }  — my claimed cuts (green)
 *   othersQty        { [cutDisplayName]: number }  — others' claimed qty per cut (0–2)
 *   onToggle(id)     — if provided, enables interactive mode
 *   onQuantityChange(id, delta) — delta +1 or -1
 */
function GroupCowDiagram({
  selectedCuts = {},
  othersQty = {},
  onToggle,
  onQuantityChange,
}) {
  const interactive = typeof onToggle === 'function';

  return (
    <svg
      viewBox="-5 70 485 340"
      style={{ width: '100%', maxWidth: 720, userSelect: 'none' }}
      aria-label="Beef cuts diagram"
    >
      <defs>
        <clipPath id="groupCowClip">
          <polygon points={COW_POINTS} />
        </clipPath>

        <linearGradient id="groupCowGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%"   stopColor="#9e2020" />
          <stop offset="100%" stopColor="#5c0f0f" />
        </linearGradient>

        <filter id="groupLblShadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="0.8" stdDeviation="1.2"
            floodColor="rgba(0,0,0,0.65)" />
        </filter>
      </defs>

      {/* ── COW BODY ── */}
      <polygon points={COW_POINTS} fill="url(#groupCowGrad)" />

      {/* ── CUT HIGHLIGHT REGIONS (clipped) ── */}
      <g clipPath="url(#groupCowClip)">
        {CUTS.map(({ id, rect: [x, y, w, h] }) => {
          const myQty    = selectedCuts[id] || 0;
          const otherQty = othersQty[id]    || 0;
          const fullyTaken = otherQty >= 2;
          const isMine   = myQty > 0;

          const fill    = isMine ? GREEN : fullyTaken ? GREY : RED_HI;
          const opacity = isMine ? 0.88  : fullyTaken ? 0.72 : 0;

          const maxQty  = 2 - otherQty;    // slots remaining for this user
          const clickable = interactive && !fullyTaken;

          return (
            <rect
              key={id} x={x} y={y} width={w} height={h}
              fill={fill} fillOpacity={opacity}
              pointerEvents={clickable ? 'all' : 'none'}
              style={{ cursor: clickable ? 'pointer' : 'default', transition: 'fill-opacity 0.1s' }}
              onClick={clickable ? () => onToggle(id) : undefined}
            />
          );
        })}
      </g>

      {/* ── SELECTED CUT BORDERS (my cuts) ── */}
      <g clipPath="url(#groupCowClip)" pointerEvents="none">
        {CUTS.map(({ id, rect: [x, y, w, h] }) =>
          (selectedCuts[id] || 0) > 0 ? (
            <rect key={id} x={x} y={y} width={w} height={h}
              fill="none"
              stroke="rgba(255,255,255,0.55)"
              strokeWidth="2" strokeDasharray="5 3"
            />
          ) : null
        )}
      </g>

      {/* ── FULLY-CLAIMED-BY-OTHERS BORDERS ── */}
      <g clipPath="url(#groupCowClip)" pointerEvents="none">
        {CUTS.map(({ id, rect: [x, y, w, h] }) => {
          const otherQty = othersQty[id] || 0;
          const isMine   = (selectedCuts[id] || 0) > 0;
          return otherQty >= 2 && !isMine ? (
            <rect key={id} x={x} y={y} width={w} height={h}
              fill="none"
              stroke="rgba(255,255,255,0.3)"
              strokeWidth="2" strokeDasharray="5 3"
            />
          ) : null;
        })}
      </g>

      {/* ── DIVIDERS (clipped) ── */}
      <g clipPath="url(#groupCowClip)" pointerEvents="none">
        {DIVIDERS.map(([x1, y1, x2, y2], i) => (
          <line key={i} x1={x1} y1={y1} x2={x2} y2={y2}
            stroke="rgba(255,255,255,0.45)" strokeWidth="1.6" />
        ))}
      </g>

      {/* ── COW OUTLINE ── */}
      <polygon points={COW_POINTS} fill="none"
        stroke="rgba(0,0,0,0.3)" strokeWidth="0.8"
        pointerEvents="none" />

      {/* ── CUT LABELS ── */}
      <g pointerEvents="none" filter="url(#groupLblShadow)"
        fontFamily="'Helvetica Neue',Arial,sans-serif" fontWeight="800"
        fill={WHITE} letterSpacing="0.6">
        {CUTS.map(({ id, label: [cx, cy], size }) => {
          const lines = getLines(id);
          const lineH = size + 2.5;
          return (
            <text key={id} textAnchor="middle">
              {lines.map((line, i) => (
                <tspan key={i} x={cx} fontSize={size}
                  y={cy + (i - (lines.length - 1) / 2) * lineH}>
                  {line}
                </tspan>
              ))}
            </text>
          );
        })}
      </g>

      {/* ── QTY CONTROLS (interactive mode only, for selected cuts) ── */}
      {interactive && CUTS.map(({ id, label: [cx, cy] }) => {
        const qty    = selectedCuts[id] || 0;
        const maxQty = 2 - (othersQty[id] || 0);
        if (qty === 0) return null;
        return (
          <g key={id} transform={`translate(${cx}, ${cy + 16})`}>
            <QtyControl
              qty={qty}
              maxQty={maxQty}
              onMinus={() => onQuantityChange(id, -1)}
              onPlus={() => onQuantityChange(id, +1)}
            />
          </g>
        );
      })}
    </svg>
  );
}

export default GroupCowDiagram;
