import './App.css'
import { SetupScreen } from './components/setup/SetupScreen';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { AppDarkTheme } from './helper/theme';
import { isConfigured } from './helper/signals';
import { MainScreen } from './components/main/MainScreen';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import Box from '@mui/material/Box';


function App() {

return (
    <ThemeProvider theme={AppDarkTheme}>
         <LocalizationProvider dateAdapter={AdapterDateFns}>
         <ToastContainer />
      <CssBaseline />
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            overflowY: 'auto',
            p: { xs: 2, sm: 3 },
          }}
        >
          {isConfigured.value == true ? (
            <MainScreen />
          ) : (
            <SetupScreen />
          )}
        </Box>
      </Box>
      </LocalizationProvider>
    </ThemeProvider>
  );
}


export default App
