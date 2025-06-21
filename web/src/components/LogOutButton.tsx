import { Button } from "@mui/material";

export const LogOutButton = () => {
    const handleLogout = () => {
        localStorage.removeItem('serverIP');
        localStorage.removeItem('authToken');
        window.location.reload();
    };

    return (
        <Button
            variant="outlined"
            color="primary"
            onClick={handleLogout}
            style={{ marginTop: "10px" }}
        >
            Quit
        </Button>
    );
}
