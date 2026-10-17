import type { ReactNode } from "react";

const skins = ["#f2c6a0", "#d9a273", "#a8744a", "#7b4f2f"];
const hairs = ["#2b2118", "#4a3322", "#1c1c28", "#6b4423"];

function Person({
  x,
  y,
  size = 1,
  tone = 0,
  hair = 0,
  shirt = "fill-accent-solid",
}: {
  x: number;
  y: number;
  size?: number;
  tone?: number;
  hair?: number;
  shirt?: string;
}) {
  return (
    <g transform={`translate(${x} ${y}) scale(${size})`}>
      <path d="M-26 34 C-26 8 -12 2 0 2 C12 2 26 8 26 34 Z" className={shirt} />
      <circle cx="0" cy="-14" r="15" fill={skins[tone % skins.length]} />
      <path d="M-15 -16 C-15 -32 15 -32 15 -16 C10 -24 -8 -25 -15 -16 Z" fill={hairs[hair % hairs.length]} />
    </g>
  );
}

function Check({ x, y, r = 14 }: { x: number; y: number; r?: number }) {
  return (
    <g>
      <circle cx={x} cy={y} r={r} className="fill-ok" />
      <path
        d={`M${x - r * 0.42} ${y + r * 0.02} L${x - r * 0.1} ${y + r * 0.34} L${x + r * 0.46} ${y - r * 0.32}`}
        fill="none"
        stroke="#fff"
        strokeWidth={r * 0.2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </g>
  );
}

function Float({ children, delay = 0, slow = false }: { children: ReactNode; delay?: number; slow?: boolean }) {
  return (
    <g className={slow ? "art-float art-float-slow" : "art-float"} style={{ animationDelay: `${delay}s` }}>
      {children}
    </g>
  );
}

function Sparkle({ x, y, s = 1, delay = 0 }: { x: number; y: number; s?: number; delay?: number }) {
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <g className="art-twinkle" style={{ animationDelay: `${delay}s` }}>
        <path d="M0 -9 C1 -3 3 -1 9 0 C3 1 1 3 0 9 C-1 3 -3 1 -9 0 C-3 -1 -1 -3 0 -9 Z" className="fill-accent-2" />
      </g>
    </g>
  );
}

function Svg({ children, viewBox = "0 0 480 400" }: { children: ReactNode; viewBox?: string }) {
  return (
    <svg viewBox={viewBox} role="presentation" aria-hidden="true" className="h-full w-full">
      {children}
    </svg>
  );
}

const shadow = { filter: "drop-shadow(0 10px 18px rgb(15 20 50 / 0.16))" };

export function PermanentScene() {
  return (
    <Svg>
      <circle cx="250" cy="205" r="168" className="fill-accent-solid" opacity="0.08" />
      <circle cx="250" cy="205" r="116" className="fill-accent-solid" opacity="0.07" />
      <path
        d="M36 346 C120 346 128 262 214 262 S334 160 438 96"
        fill="none"
        className="art-dash stroke-accent-solid"
        strokeWidth="3"
        strokeLinecap="round"
        strokeDasharray="2 11"
      />
      <circle cx="48" cy="346" r="13" className="fill-surface stroke-accent-solid" strokeWidth="3" />
      <circle cx="46" cy="344" r="5" fill="none" className="stroke-accent-solid" strokeWidth="2.5" />
      <path d="M50 348 L56 354" className="stroke-accent-solid" strokeWidth="2.5" strokeLinecap="round" />
      <Float>
        <g style={shadow}>
          <rect
            x="96"
            y="120"
            width="214"
            height="148"
            rx="18"
            className="fill-surface stroke-line"
            strokeWidth="1.5"
          />
          <circle cx="144" cy="170" r="30" className="fill-accent-solid" opacity="0.12" />
          <Person x={144} y={178} size={0.95} tone={1} hair={0} shirt="fill-accent-solid" />
          <rect x="190" y="152" width="96" height="11" rx="5.5" className="fill-fg" opacity="0.78" />
          <rect x="190" y="174" width="70" height="8" rx="4" className="fill-muted" opacity="0.55" />
          <rect x="190" y="192" width="84" height="8" rx="4" className="fill-muted" opacity="0.3" />
          <rect x="116" y="226" width="54" height="22" rx="11" className="fill-accent-solid" opacity="0.16" />
          <rect x="178" y="226" width="64" height="22" rx="11" className="fill-accent-solid" opacity="0.16" />
          <rect x="250" y="226" width="46" height="22" rx="11" className="fill-accent-solid" opacity="0.16" />
        </g>
        <Check x={304} y={124} r={17} />
      </Float>
      <Float delay={1.2} slow>
        <g style={shadow}>
          <rect x="318" y="48" width="124" height="96" rx="16" className="fill-surface stroke-line" strokeWidth="1.5" />
          <path d="M318 64 a16 16 0 0 1 16 -16 h92 a16 16 0 0 1 16 16 v14 h-124 Z" className="fill-accent-solid" />
          {[0, 1, 2, 3, 4].map((col) =>
            [0, 1].map((row) => (
              <rect
                key={`${col}-${row}`}
                x={332 + col * 20}
                y={90 + row * 22}
                width="13"
                height="13"
                rx="4"
                className={col === 3 && row === 0 ? "fill-accent-solid" : "fill-muted"}
                opacity={col === 3 && row === 0 ? 1 : 0.22}
              />
            )),
          )}
        </g>
      </Float>
      <Float delay={2.1}>
        <g style={shadow}>
          <rect
            x="300"
            y="276"
            width="146"
            height="74"
            rx="16"
            className="fill-surface stroke-line"
            strokeWidth="1.5"
          />
          <circle cx="336" cy="313" r="19" className="fill-warn" />
          <circle cx="336" cy="313" r="11" fill="none" stroke="#fff" strokeWidth="2.5" opacity="0.9" />
          <rect x="366" y="299" width="62" height="9" rx="4.5" className="fill-fg" opacity="0.7" />
          <rect x="366" y="318" width="44" height="8" rx="4" className="fill-muted" opacity="0.4" />
        </g>
      </Float>
      <Sparkle x={70} y={150} s={1.2} />
      <Sparkle x={420} y={230} s={0.9} delay={1} />
      <Sparkle x={210} y={70} s={0.8} delay={2} />
    </Svg>
  );
}

export function ContractScene() {
  const ticks = Array.from({ length: 12 }, (_, i) => i);
  return (
    <Svg>
      <circle cx="240" cy="210" r="172" className="fill-accent-solid" opacity="0.07" />
      <Float slow>
        <g style={shadow}>
          <circle cx="196" cy="206" r="118" className="fill-surface stroke-line" strokeWidth="1.5" />
          <circle
            cx="196"
            cy="206"
            r="96"
            fill="none"
            className="stroke-accent-solid"
            opacity="0.14"
            strokeWidth="16"
          />
          <circle
            cx="196"
            cy="206"
            r="96"
            fill="none"
            className="stroke-accent-solid"
            strokeWidth="16"
            strokeLinecap="round"
            strokeDasharray="410 604"
            transform="rotate(-90 196 206)"
          />
          {ticks.map((i) => (
            <line
              key={i}
              x1="196"
              y1="106"
              x2="196"
              y2={i % 3 === 0 ? 118 : 112}
              className="stroke-muted"
              opacity="0.6"
              strokeWidth={i % 3 === 0 ? 3 : 1.5}
              strokeLinecap="round"
              transform={`rotate(${i * 30} 196 206)`}
            />
          ))}
          <line x1="196" y1="206" x2="196" y2="150" className="stroke-fg" strokeWidth="5" strokeLinecap="round" />
          <line
            x1="196"
            y1="206"
            x2="236"
            y2="226"
            className="art-spin stroke-accent-solid"
            strokeWidth="4"
            strokeLinecap="round"
          />
          <circle cx="196" cy="206" r="8" className="fill-fg" />
        </g>
      </Float>
      <Float delay={1}>
        <g style={shadow}>
          <rect
            x="326"
            y="62"
            width="128"
            height="116"
            rx="16"
            className="fill-surface stroke-line"
            strokeWidth="1.5"
          />
          <path d="M326 78 a16 16 0 0 1 16 -16 h96 a16 16 0 0 1 16 16 v14 h-128 Z" className="fill-accent-solid" />
          {[0, 1, 2, 3].map((col) =>
            [0, 1, 2].map((row) => (
              <rect
                key={`${col}-${row}`}
                x={341 + col * 27}
                y={102 + row * 24}
                width="17"
                height="15"
                rx="4"
                className={(col + row) % 4 === 1 ? "fill-ok" : "fill-muted"}
                opacity={(col + row) % 4 === 1 ? 0.9 : 0.2}
              />
            )),
          )}
        </g>
      </Float>
      <Float delay={2} slow>
        <g style={shadow}>
          <rect
            x="304"
            y="226"
            width="150"
            height="110"
            rx="18"
            className="fill-surface stroke-line"
            strokeWidth="1.5"
          />
          <Person x={346} y={268} size={0.8} tone={0} hair={2} shirt="fill-warn" />
          <Person x={392} y={272} size={0.8} tone={2} hair={1} shirt="fill-accent-solid" />
          <Person x={432} y={268} size={0.7} tone={3} hair={0} shirt="fill-ok" />
          <rect x="324" y="316" width="110" height="7" rx="3.5" className="fill-muted" opacity="0.3" />
        </g>
      </Float>
      <Sparkle x={52} y={96} s={1.1} />
      <Sparkle x={430} y={210} s={0.8} delay={1.4} />
      <Sparkle x={120} y={352} s={0.9} delay={0.6} />
    </Svg>
  );
}

export function RpoScene() {
  const stream = [
    { x: 150, delay: 0, tone: 0, shirt: "fill-warn" },
    { x: 206, delay: 0.6, tone: 1, shirt: "fill-accent-solid" },
    { x: 262, delay: 1.2, tone: 2, shirt: "fill-ok" },
    { x: 318, delay: 1.8, tone: 3, shirt: "fill-accent-2" },
  ];
  return (
    <Svg>
      <circle cx="240" cy="205" r="170" className="fill-accent-solid" opacity="0.07" />
      {stream.map((item) => (
        <g key={item.x} className="art-drop" style={{ animationDelay: `${item.delay}s` }}>
          <Person x={item.x} y={40} size={0.55} tone={item.tone} hair={item.tone} shirt={item.shirt} />
        </g>
      ))}
      <g style={shadow}>
        <path
          d="M84 106 H396 L292 236 V318 C292 326 286 332 278 332 H202 C194 332 188 326 188 318 V236 Z"
          className="fill-surface stroke-line"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
        <path d="M110 138 H370 L276 224 H204 Z" className="fill-accent-solid" opacity="0.1" />
        <rect x="120" y="120" width="240" height="8" rx="4" className="fill-muted" opacity="0.25" />
        <rect x="170" y="150" width="140" height="8" rx="4" className="fill-muted" opacity="0.25" />
        <rect x="208" y="178" width="64" height="8" rx="4" className="fill-muted" opacity="0.25" />
      </g>
      <Float>
        <g style={shadow}>
          <circle cx="240" cy="368" r="24" className="fill-surface stroke-accent-solid" strokeWidth="3" />
          <Person x={240} y={374} size={0.5} tone={1} hair={0} shirt="fill-accent-solid" />
        </g>
        <Check x={262} y={348} r={11} />
      </Float>
      <Float delay={1.4} slow>
        <g style={shadow}>
          <rect x="22" y="206" width="104" height="82" rx="14" className="fill-surface stroke-line" strokeWidth="1.5" />
          <rect x="38" y="224" width="72" height="9" rx="4.5" className="fill-fg" opacity="0.7" />
          <rect x="38" y="244" width="56" height="7" rx="3.5" className="fill-muted" opacity="0.4" />
          <rect x="38" y="260" width="64" height="7" rx="3.5" className="fill-muted" opacity="0.3" />
        </g>
      </Float>
      <Float delay={0.8}>
        <g style={shadow}>
          <rect
            x="352"
            y="244"
            width="108"
            height="84"
            rx="14"
            className="fill-surface stroke-line"
            strokeWidth="1.5"
          />
          <Person x={384} y={282} size={0.62} tone={0} hair={3} shirt="fill-accent-solid" />
          <Person x={424} y={284} size={0.62} tone={2} hair={1} shirt="fill-warn" />
          <rect x="368" y="310" width="76" height="6" rx="3" className="fill-muted" opacity="0.3" />
        </g>
      </Float>
      <Sparkle x={60} y={120} s={1} delay={0.3} />
      <Sparkle x={430} y={140} s={1.1} delay={1.3} />
    </Svg>
  );
}

export function MatchArt() {
  return (
    <Svg viewBox="0 0 440 210">
      <rect x="0" y="0" width="440" height="210" rx="22" className="fill-accent-solid" opacity="0.07" />
      <path
        d="M96 108 C150 40 210 40 220 104 S300 176 344 108"
        fill="none"
        className="art-dash stroke-accent-solid"
        strokeWidth="3"
        strokeLinecap="round"
        strokeDasharray="2 11"
      />
      <Float slow>
        <g style={shadow}>
          <rect x="40" y="70" width="86" height="86" rx="20" className="fill-surface stroke-line" strokeWidth="1.5" />
          <rect x="62" y="96" width="42" height="42" rx="6" className="fill-accent-solid" opacity="0.9" />
          {[0, 1, 2].map((col) =>
            [0, 1, 2].map((row) => (
              <rect
                key={`${col}-${row}`}
                x={68 + col * 12}
                y={102 + row * 12}
                width="7"
                height="7"
                rx="2"
                fill="#fff"
                opacity="0.85"
              />
            )),
          )}
        </g>
      </Float>
      <Float delay={1} slow>
        <g style={shadow}>
          <rect x="314" y="70" width="86" height="86" rx="20" className="fill-surface stroke-line" strokeWidth="1.5" />
          <Person x={357} y={112} size={0.85} tone={1} hair={1} shirt="fill-warn" />
        </g>
      </Float>
      <Check x={220} y={104} r={22} />
      <Sparkle x={190} y={168} s={0.9} delay={0.5} />
      <Sparkle x={262} y={42} s={0.8} delay={1.5} />
    </Svg>
  );
}
