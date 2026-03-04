import { useState } from 'react';

/*
 * Cow silhouette (public-domain polygon).
 * Cow faces RIGHT — rump at low-x (left), head/horns at high-x (right).
 *
 * Anatomy landmarks:
 *   Back line:  y≈115–132, x≈42–358
 *   Belly line: y≈285–312, x≈90–320
 *   Rump curve: x≈0–42,   y≈127–267
 *   Front legs (near rump): x≈17–135, y≈297–400
 *   Rear legs  (near head): x≈253–345, y≈286–400
 *   Head/horns: x≈346–475, y≈77–182
 *
 * Props:
 *   selectedCuts: { [cutId: string]: number }  — map of selected cut → quantity (1 or 2)
 *   onToggle(id)                                — add (qty=1) or remove a cut
 *   onQuantityChange(id, delta)                 — delta = +1 or -1; parent handles deselect if qty < 1
 */

const RED_HI = '#b03030';
const GREEN  = '#4a7c59';
const WHITE  = '#ffffff';

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

/*
 * Cut layout (rump → head, left → right):
 *
 * UPPER (y: UPPER_Y → MID_Y):
 *   Round | Sirloin | Short Loin | Rib | Chuck | Neck
 *   x:  0      72       135       200    290    370
 *
 * LOWER (y: MID_Y → LOWER_Y):
 *   (Round) | Flank | Plate | Brisket | (Neck)
 *             x: 72    200     290
 *
 * SHANKS (y: LOWER_Y → 405):
 *   Front (near rump) x: 15–135
 *   Rear  (near head) x: 253–345
 */

const UPPER_Y = 75;
const MID_Y   = 228;
const LOWER_Y = 308;

const CUTS = [
  // ── Upper row ──────────────────────────────────────────────────────────
  { id: 'Round',         rect: [0,   UPPER_Y, 72,  MID_Y - UPPER_Y], label: [32,  178], size: 11 },
  { id: 'Sirloin',       rect: [72,  UPPER_Y, 63,  MID_Y - UPPER_Y], label: [103, 175], size: 10 },
  { id: 'Short Loin',    rect: [135, UPPER_Y, 65,  MID_Y - UPPER_Y], label: [167, 170], size: 9  },
  { id: 'Rib',           rect: [200, UPPER_Y, 90,  MID_Y - UPPER_Y], label: [245, 172], size: 14 },
  { id: 'Chuck',         rect: [290, UPPER_Y, 80,  MID_Y - UPPER_Y], label: [330, 166], size: 12 },
  { id: 'Neck',          rect: [370, UPPER_Y, 106, MID_Y - UPPER_Y], label: [418, 153], size: 10 },
  // ── Lower row ──────────────────────────────────────────────────────────
  { id: 'Flank',         rect: [72,  MID_Y, 128, LOWER_Y - MID_Y],   label: [136, 267], size: 10 },
  { id: 'Plate',         rect: [200, MID_Y, 90,  LOWER_Y - MID_Y],   label: [245, 267], size: 10 },
  { id: 'Brisket',       rect: [290, MID_Y, 80,  LOWER_Y - MID_Y],   label: [330, 267], size: 9  },
  // ── Shanks (legs) ──────────────────────────────────────────────────────
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

// Counter pill: shows  − qty +  for a selected cut
// Rendered at (cx, cy) in SVG coordinates (NOT inside clipPath so it's never clipped)
function Counter({ cx, cy, qty, onMinus, onPlus }) {
  const PW = 50, PH = 18;
  const atMin = qty === 1;
  const atMax = qty === 2;

  return (
    <g transform={`translate(${cx}, ${cy})`}>
      {/* Pill background */}
      <rect
        x={-PW / 2} y={-PH / 2} width={PW} height={PH} rx={PH / 2}
        fill="white" fillOpacity="0.95" stroke={GREEN} strokeWidth="1.4"
      />

      {/* Minus / deselect button */}
      <rect
        x={-PW / 2} y={-PH / 2} width={18} height={PH} rx={PH / 2}
        fill="transparent" pointerEvents="all"
        style={{ cursor: 'pointer' }}
        onClick={(e) => { e.stopPropagation(); onMinus(); }}
      />
      <text
        x={-PW / 2 + 9} y="1"
        textAnchor="middle" dominantBaseline="middle"
        fontSize="13" fontWeight="800"
        fill={atMin ? '#cc2222' : GREEN}
        pointerEvents="none"
        fontFamily="'Helvetica Neue',Arial,sans-serif"
      >
        {atMin ? '×' : '−'}
      </text>

      {/* Quantity display */}
      <text
        x="0" y="1"
        textAnchor="middle" dominantBaseline="middle"
        fontSize="11" fontWeight="800" fill="#222"
        pointerEvents="none"
        fontFamily="'Helvetica Neue',Arial,sans-serif"
      >
        {qty}
      </text>

      {/* Plus button */}
      <rect
        x={PW / 2 - 18} y={-PH / 2} width={18} height={PH} rx={PH / 2}
        fill="transparent" pointerEvents="all"
        style={{ cursor: atMax ? 'not-allowed' : 'pointer' }}
        onClick={(e) => { e.stopPropagation(); if (!atMax) onPlus(); }}
      />
      <text
        x={PW / 2 - 9} y="1"
        textAnchor="middle" dominantBaseline="middle"
        fontSize="13" fontWeight="800"
        fill={atMax ? 'rgba(0,0,0,0.25)' : GREEN}
        pointerEvents="none"
        fontFamily="'Helvetica Neue',Arial,sans-serif"
      >
        +
      </text>
    </g>
  );
}

function CowDiagram({ selectedCuts, onToggle, onQuantityChange }) {
  const [hovered, setHovered] = useState(null);

  return (
    <svg
      viewBox="-5 70 485 340"
      style={{ width: '100%', maxWidth: 720, userSelect: 'none' }}
      aria-label="Interactive beef cuts — click a section to select"
    >
      <defs>
        <clipPath id="cowClip">
          <polygon points={COW_POINTS} />
        </clipPath>

        <linearGradient id="cowGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%"   stopColor="#9e2020" />
          <stop offset="100%" stopColor="#5c0f0f" />
        </linearGradient>

        <filter id="lblShadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="0.8" stdDeviation="1.2"
            floodColor="rgba(0,0,0,0.65)" />
        </filter>
      </defs>

      {/* ── COW BODY ── */}
      <polygon points={COW_POINTS} fill="url(#cowGrad)" />

      {/* ── CUT HIGHLIGHT REGIONS (clipped) ── */}
      <g clipPath="url(#cowClip)">
        {CUTS.map(({ id, rect: [x, y, w, h] }) => {
          const selected = id in selectedCuts;
          const isHov    = hovered === id && !selected;
          return (
            <rect
              key={id} x={x} y={y} width={w} height={h}
              fill={selected ? GREEN : RED_HI}
              fillOpacity={selected ? 0.88 : isHov ? 0.38 : 0}
              pointerEvents="all"
              onClick={() => onToggle(id)}
              onMouseEnter={() => setHovered(id)}
              onMouseLeave={() => setHovered(null)}
              style={{ cursor: 'pointer', transition: 'fill-opacity 0.1s' }}
            />
          );
        })}
      </g>

      {/* ── SELECTED CUT BORDERS (clipped) ── */}
      <g clipPath="url(#cowClip)" pointerEvents="none">
        {CUTS.map(({ id, rect: [x, y, w, h] }) =>
          id in selectedCuts ? (
            <rect key={id} x={x} y={y} width={w} height={h}
              fill="none"
              stroke="rgba(255,255,255,0.55)"
              strokeWidth="2" strokeDasharray="5 3"
            />
          ) : null
        )}
      </g>

      {/* ── DIVIDERS (clipped) ── */}
      <g clipPath="url(#cowClip)" pointerEvents="none">
        {DIVIDERS.map(([x1, y1, x2, y2], i) => (
          <line key={i} x1={x1} y1={y1} x2={x2} y2={y2}
            stroke="rgba(255,255,255,0.45)" strokeWidth="1.6" />
        ))}
      </g>

      {/* ── COW OUTLINE ── */}
      <polygon points={COW_POINTS} fill="none"
        stroke="rgba(0,0,0,0.3)" strokeWidth="0.8"
        pointerEvents="none" />

      {/* ── CUT LABELS (shift up when selected to make room for counter) ── */}
      <g pointerEvents="none" filter="url(#lblShadow)"
        fontFamily="'Helvetica Neue',Arial,sans-serif" fontWeight="800"
        fill={WHITE} letterSpacing="0.6">
        {CUTS.map(({ id, label: [cx, cy], size }) => {
          const selected = id in selectedCuts;
          const lines    = getLines(id);
          const lineH    = size + 2.5;
          const labelY   = selected ? cy - 10 : cy;
          return (
            <text key={id} textAnchor="middle">
              {lines.map((line, i) => (
                <tspan key={i} x={cx} fontSize={size}
                  y={labelY + (i - (lines.length - 1) / 2) * lineH}>
                  {line}
                </tspan>
              ))}
            </text>
          );
        })}
      </g>

      {/* ── QUANTITY COUNTERS (not clipped — always fully visible) ── */}
      {CUTS.map(({ id, label: [cx, cy] }) => {
        if (!(id in selectedCuts)) return null;
        const qty = selectedCuts[id];
        return (
          <Counter
            key={`counter-${id}`}
            cx={cx}
            cy={cy + 12}
            qty={qty}
            onMinus={() => onQuantityChange(id, -1)}
            onPlus={()  => onQuantityChange(id, +1)}
          />
        );
      })}
    </svg>
  );
}

export default CowDiagram;
