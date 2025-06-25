import ArrowBackIos from "@mui/icons-material/ArrowBackIos";
import ArrowForwardIos from "@mui/icons-material/ArrowForwardIos";
import { Box, IconButton, CircularProgress, Stack, Typography } from "@mui/material";
import { subDays, isToday, addDays } from "date-fns";
import { currentDate, currentHydration, hydrationGoal } from "../../helper/signals";
import { AppDarkTheme } from "../../helper/theme";
import { dateToDisplayString, getUnitStringLargeWithValue } from "../../helper/unitHelper";
import { CircularProgressWithGap } from "../CircularProgressWithGap";

export const HydrationDisplay = (
    {
        isLoading,
        progress = 0
    }: {
        isLoading: boolean;
        progress: number;
    }
) => {
    return (
        <Box sx={{
            display: 'flex',
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
        }}>
            <IconButton aria-label="Minus one day" onClick={(e) => {
                e.stopPropagation();
                currentDate.value = subDays(currentDate.peek(), 1);
            }}>
                <ArrowBackIos />
            </IconButton>
            {isLoading ? (
                <CircularProgress size={200} />
            ) : (
                <CircularProgressWithGap
                    progress={progress}
                    size={200}
                    strokeWidth={18}
                    progressColor={AppDarkTheme.palette.primary.main}
                    trackColor={AppDarkTheme.palette.surfaceContainerLow.main}
                    gapAngle={80}
                >
                    <Stack alignItems="center" spacing={0.5}>
                        <Typography variant="body1" color="text.secondary">
                            {dateToDisplayString(currentDate.value)}
                        </Typography>
                        <Typography
                            variant="h3"
                            component="div"
                            fontWeight="700"
                            color="primary"
                            sx={{ lineHeight: 0.8 }}
                        >
                            {getUnitStringLargeWithValue(currentHydration.value || 0)}
                        </Typography>

                        <Typography variant="h5" color="text.secondary">
                            / {getUnitStringLargeWithValue(hydrationGoal.value || 1)}
                        </Typography>
                    </Stack>
                </CircularProgressWithGap>
            )}
            <IconButton aria-label="Plus one day" disabled={isToday(currentDate.value)} onClick={(e) => {
                e.stopPropagation();
                if (isToday(currentDate.peek())) return;
                currentDate.value = addDays(currentDate.peek(), 1);
            }}>
                <ArrowForwardIos />
            </IconButton>
        </Box>
    )
}
