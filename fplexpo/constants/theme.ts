/**
 * FPL Tracker Design Tokens
 * Mirrors the Android Kotlin/Compose Stitch design system exactly.
 */

// ── Core palette ──────────────────────────────────────────────────────────────
export const Colors = {
  // Backgrounds
  background:        '#131313',
  surface:           '#1C1B1B',
  surfaceContainer:  '#20201F',
  surfaceHigh:       '#2A2A2A',
  surfaceHighest:    '#353535',

  // Primary (green)
  primary:           '#A1D494',
  primaryContainer:  '#2D5A27',
  onPrimary:         '#0A3909',

  // Secondary (gold/yellow)
  secondary:         '#FFE083',
  onSecondary:       '#3C2F00',

  // Tertiary (red)
  tertiary:          '#FFB3AD',
  tertiaryContainer: '#A40217',
  onTertiary:        '#FFDAD7',

  // Text & borders
  onSurface:         '#E5E2E1',
  onSurfaceVariant:  '#C2C9BB',
  outline:           '#8C9387',
  outlineVariant:    '#42493E',

  // Chip colours
  chipBB: '#FFE083', chipBBText: '#3C2F00',
  chipWC: '#2D5A27', chipWCText: '#A1D494',
  chipTC: '#353535', chipTCText: '#FFFFFF',
  chipFH: '#A40217', chipFHText: '#FFDAD7',

  // Status
  error:    '#FFB4AB',
  onError:  '#690005',
  rankUp:   '#A1D494',
  rankDown: '#FFB3AD',
  rankSame: '#8C9387',

  // Legacy tint aliases used by existing Expo components
  tint:     '#A1D494',
};

export const Spacing = {
  xs:   4,
  sm:   8,
  md:   12,
  lg:   16,
  xl:   20,
  xxl:  24,
  xxxl: 32,
};

export const Radius = {
  sm:   8,
  md:   12,
  lg:   16,
  xl:   20,
  xxl:  24,
  full: 999,
};

export function getDifficultyTheme(difficulty: number): { backgroundColor: string; color: string } {
  switch (difficulty) {
    case 1:
      return { backgroundColor: '#37A626', color: '#FFFFFF' };
    case 2:
      return { backgroundColor: '#00FF87', color: '#0B0B0B' };
    case 3:
      return { backgroundColor: '#E9E8E6', color: '#131313' };
    case 4:
      return { backgroundColor: '#FF0057', color: '#FFFFFF' };
    default:
      return { backgroundColor: '#8A003C', color: '#FFFFFF' };
  }
}
