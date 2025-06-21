import { createTheme } from '@mui/material/styles';

export const darkThemeColors = {
  primary: '#a9c7ff',
  onPrimary: '#09305f',
  primaryContainer: '#274777',
  onPrimaryContainer: '#d6e3ff',
  secondary: '#bec7dc',
  onSecondary: '#283141',
  tertiary: '#dcbce1',
  onTertiary: '#3f2845',
  error: '#ffb4ab',
  onError: '#690005',
  background: '#111318',
  onBackground: '#e2e2e9',
  surface: '#111318',
  onSurface: '#e2e2e9',
  surfaceVariant: 'rgb(68 71 78)',
  onSurfaceVariant: 'rgb(196 198 207)',
  outline: 'rgb(142 144 153)',
  outlineVariant: 'rgb(68 71 78)',
  inverseOnSurface: 'rgb(46 48 54)',
  inverseSurface: 'rgb(226 226 233)',
  surfaceContainerLow: 'rgb(25 28 32)',
  surfaceContainer: 'rgb(29 32 36)',
  surfaceContainerHighest: 'rgb(51 53 58)',
};

export const AppDarkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: darkThemeColors.primary,
      contrastText: darkThemeColors.onPrimary,
    },
    secondary: {
      main: darkThemeColors.secondary,
      contrastText: darkThemeColors.onSecondary,
    },
    tertiary: {
      main: darkThemeColors.tertiary,
      contrastText: darkThemeColors.onTertiary,
    },
    error: {
      main: darkThemeColors.error,
      contrastText: darkThemeColors.onError,
    },
    background: {
      default: darkThemeColors.background,
      paper: darkThemeColors.surfaceContainer, // Cards and elevated surfaces use this
    },
    text: {
      primary: darkThemeColors.onSurface,
      secondary: darkThemeColors.onSurfaceVariant,
    },
    // Custom roles
    surfaceContainer: {
        main: darkThemeColors.surfaceContainer,
        contrastText: darkThemeColors.onSurface
    },
    surfaceContainerLow: {
        main: darkThemeColors.surfaceContainerLow,
        contrastText: darkThemeColors.onSurface
    },
    surfaceContainerHighest: {
        main: darkThemeColors.surfaceContainerHighest,
        contrastText: darkThemeColors.onSurface
    },
    outline: {
        main: darkThemeColors.outline
    },
    outlineVariant: {
        main: darkThemeColors.outlineVariant
    }
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  components: {
    // Default component props and styles
    MuiCard: {
      defaultProps: {
        elevation: 0, // M3 prefers no shadow, using bg color for depth
      },
      styleOverrides: {
        root: {
          // Use a higher surface color for cards to make them stand out
          backgroundImage: 'none', // Important override for dark mode
          backgroundColor: darkThemeColors.surfaceContainer,
          borderRadius: '12px',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: '1.5rem',
          textTransform: 'none',
          fontWeight: 'bold',
          padding: '8px 24px',
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        // Style the border of TextFields
        notchedOutline: {
          borderColor: darkThemeColors.outline,
        },
        root: {
            '&:hover .MuiOutlinedInput-notchedOutline': {
                borderColor: darkThemeColors.onSurfaceVariant,
            },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                borderColor: darkThemeColors.primary,
            },
        }
      },
    },
  },
});
