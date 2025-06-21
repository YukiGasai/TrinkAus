import '@mui/material/styles';

declare module '@mui/material/styles' {
  // Allow for custom color roles
  interface Palette {
    tertiary: Palette['primary'];
    surfaceContainer: Palette['primary'];
    surfaceContainerLow: Palette['primary'];
    surfaceContainerHighest: Palette['primary'];
    outline: Palette['primary'];
    outlineVariant: Palette['primary'];
  }
  interface PaletteOptions {
    tertiary?: PaletteOptions['primary'];
    surfaceContainer?: PaletteOptions['primary'];
    surfaceContainerLow?: PaletteOptions['primary'];
    surfaceContainerHighest?: PaletteOptions['primary'];
    outline?: PaletteOptions['primary'];
    outlineVariant?: PaletteOptions['primary'];
  }
}

// Allow for new color prop on components
declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    tertiary: true;
  }
}
