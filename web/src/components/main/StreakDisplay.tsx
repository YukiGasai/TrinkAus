import { Box, Card, CircularProgress, Typography } from "@mui/material"
import type { Streak } from "../../types"
import { currentStreak, longestStreak } from "../../helper/signals"

export const StreakDisplay = () => {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', flexWrap: 'wrap', padding: 2 }}>
            {StreakItem("Longest Streak", longestStreak.value)}
            {StreakItem("Current Streak", currentStreak.value)}
        </Box>
    )
}

const StreakItem = (label: string, streak: Streak | null) => {
    return (
        <Card sx={{ textAlign: 'center', padding: 2, margin: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <Typography component="div" color="textSecondary">
                {label}
            </Typography>
            {streak === null ?
                <CircularProgress size={24} />
                :
                <Typography variant="h4" component="div" color="textPrimary" sx={{ fontWeight: 'bold' }}>
                    {streak?.length || "0"}
                </Typography>
            }
            <Typography variant="body1" color="textSecondary">
                {streak?.startDate || "-"}
            </Typography>
        </Card>
    )

}
