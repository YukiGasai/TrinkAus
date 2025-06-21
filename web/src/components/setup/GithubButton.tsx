import { Box, Button, Typography } from "@mui/material";
import GitHubIcon from '@mui/icons-material/GitHub';
import { darkThemeColors } from "../../helper/theme";



export const GithubButton = () => (
    <Button
        variant="contained"
        size="small"
        sx={{ mt: 2, borderRadius: 2, background: '#010409', color: '#c9d1d9', '&:hover': { backgroundColor: darkThemeColors.surfaceContainer } }}
        onClick={() => {
            window.open('https://github.com/YukiGasai/TrinkAus', '_blank');
        }}
    >
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Typography variant="body1" sx={{ mr: 1, fontWeight: 'bold' }}>
                View on GitHub
            </Typography>
            <GitHubIcon />
        </Box>
    </Button>
)
