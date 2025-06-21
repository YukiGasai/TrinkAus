import React from 'react';
import {
    Box,
    Button,
    Card,
    CardContent,
    Container,
    Stack,
    TextField,
    Typography,
    InputAdornment,
} from '@mui/material';
import LanIcon from '@mui/icons-material/Lan';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { authToken, serverIP } from '../../helper/signals';
import { GithubButton } from './GithubButton';

export function SetupScreen() {

    const [serverIpInputValue, setserverIpInputValue] = React.useState(serverIP.value);
    const [authTokenInputValue, setAuthTokenInputValue] = React.useState(authToken.value);

    const handleSave = (event: React.FormEvent) => {
        event.preventDefault();

        if (!serverIpInputValue || !authTokenInputValue) {
            alert("Please fill in all fields.");
            return;
        }
        localStorage.setItem("serverIP", serverIpInputValue);
        localStorage.setItem("authToken", authTokenInputValue);
        serverIP.value = serverIpInputValue;
        authToken.value = authTokenInputValue;
    };

    return (
        <Container component="main" maxWidth="sm" sx={{ mt: { xs: 4, md: 8 } }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Typography component="h1" variant="h2" gutterBottom>
                    TrinkAus Web
                </Typography>
                <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 4 }}>
                    Connect to your local server.
                </Typography>

                {/* The Card will automatically use the `surfaceContainer` color from the theme */}
                <Card sx={{ width: '100%' }}>
                    <Box>
                        <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                            <Stack spacing={3}>
                                <TextField
                                    required
                                    fullWidth
                                    label="Server IP Address"
                                    value={serverIpInputValue}
                                    onChange={(e) => setserverIpInputValue(e.target.value)}
                                    placeholder="e.g., 192.168.1.100"
                                    InputProps={{
                                        startAdornment: (
                                            <InputAdornment position="start">
                                                <LanIcon />
                                            </InputAdornment>
                                        ),
                                    }}
                                />
                                <TextField
                                    required
                                    fullWidth
                                    label="Auth Token"
                                    type="password"
                                    value={authTokenInputValue}
                                    onChange={(e) => setAuthTokenInputValue(e.target.value)}
                                    InputProps={{
                                        startAdornment: (
                                            <InputAdornment position="start">
                                                <VpnKeyIcon />
                                            </InputAdornment>
                                        ),
                                    }}
                                />
                            </Stack>
                        </CardContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', p: 2, pt: 0 }}>
                            <Typography variant="body2" textAlign='center' color="error" sx={{ ml: 6, mr: 6, mb: 2 }}>
                                Due to a unsecure conection, the data send is not encrypted. Only use this app on a trusted network.
                            </Typography>
                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                sx={{ width: '80%' }}
                                onClick={handleSave}
                            >
                                Save & Connect
                            </Button>
                        </Box>
                    </Box>
                </Card>
                <GithubButton />
            </Box>
        </Container>
    );
}
