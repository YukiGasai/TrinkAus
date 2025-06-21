import { useEffect, useMemo, useState } from "react"
import { getHydration } from "../../api/getHydration"
import { toast } from "react-toastify";
import { Box, CircularProgress, Typography, Card, Divider } from "@mui/material";
import { AddButtons } from "./AddButtons";
import { getGoal } from "../../api/getGoal";
import { getUnit } from "../../api/getUnit";
import { batch, computed, useSignalEffect } from "@preact/signals-react";
import { addHydrationAmountLarge, addHydrationAmountMedium, addHydrationAmountSmall, currentDate, currentHydration, currentStreak, historyDate, hydrationGoal, hydrationHistory, isMetric, longestStreak } from "../../helper/signals";
import { getAddHydrationAmounts } from "../../api/getAddHydrationAmounts";
import { getStreaks } from "../../api/getStreaks";
import { StreakDisplay } from "./StreakDisplay";
import { getHydrationHistory } from "../../api/getHydrationHistory";
import { HeatmapCalendar } from "../HeatmapCalendar";
import { LogOutButton } from "../LogOutButton";
import { AddWaterModal } from "./AddWaterModal";
import { dateToString } from "../../helper/unitHelper";
import { HydrationDisplay } from "./HydrationDisplay";
import { MonthSelector } from "./MonthSelector";


export function MainScreen() {

    const [open, setOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    async function updateOnlyHydration(date: Date) {
        setIsLoading(true);
        const hydration = await getHydration(dateToString(date));
        if (hydration._tag === "Failure") {
            toast.error(hydration.error);
            return;
        }
        currentHydration.value = hydration.data;
        setIsLoading(false);
    }

    async function updateOnlyHistory(date: Date) {
        const history = await getHydrationHistory(dateToString(date));
        if (history._tag === "Failure") {
            toast.error(history.error);
            return;
        }
        hydrationHistory.value = history.data.history;
    }

    async function loadDataInit() {
        setIsLoading(true);

        // Get current goal
        const _goal = await getGoal()
        if (_goal._tag === "Failure") {
            toast.error(_goal.error);
            return;
        }

        // Get current unit system
        const _unit = await getUnit()
        if (_unit._tag === "Failure") {
            toast.error(_unit.error);
            return;
        }

        // Get the add hydration amount
        const _amounts = await getAddHydrationAmounts()
        if (_amounts._tag === "Failure") {
            toast.error(_amounts.error);
            return;
        }

        // Get the sreaks
        const _streaks = await getStreaks()
        if (_streaks._tag === "Failure") {
            toast.error(_streaks.error);
            return;
        }

        batch(() => {
            hydrationGoal.value = _goal.data;
            isMetric.value = _unit.data;
            addHydrationAmountSmall.value = _amounts.data.small;
            addHydrationAmountMedium.value = _amounts.data.medium;
            addHydrationAmountLarge.value = _amounts.data.large;

            currentStreak.value = _streaks.data.currentStreak;
            longestStreak.value = _streaks.data.longestStreak;
        })
        setIsLoading(false);
    }


    useEffect(() => {
        loadDataInit()
    }, [])

    useSignalEffect(() => {
        updateOnlyHydration(currentDate.value);
    })


    useSignalEffect(() => {
        updateOnlyHistory(historyDate.value);
    })


    const progress = computed(() => {
        if (hydrationGoal.value === null || hydrationGoal.value === 0) {
            return 0;
        }
        return ((currentHydration.value || 0) / hydrationGoal.value) * 100;
    });

    const { minValue, maxValue } = useMemo(() => {

        const values = Object.entries(hydrationHistory.value || {})
            .map(([, value]) => value);

        if (values.length === 0) {
            return { minValue: 0, maxValue: 0 }; // Default if no data
        }

        return {
            minValue: Math.min(...values),
            maxValue: Math.max(...values),
        };
    }, [historyDate.value, hydrationHistory.value]);

    return (
        <>
            {currentHydration.value !== null ? (

                <div style={{
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center',
                }}>
                    <AddWaterModal open={open} setOpen={setOpen} />
                    <Box sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        width: 'fit-content',
                    }}>
                        <Card sx={{
                            padding: 2,
                            margin: 2,
                            width: 'fit-content',
                        }}
                            onClick={() => {
                                setOpen(true);
                            }}>
                            <HydrationDisplay
                                isLoading={isLoading}
                                progress={progress.value}
                            />
                            <Typography variant="body1" color="primary" sx={{ textAlign: 'center' }}>
                                {progress.value.toFixed(0)}% to the goal
                            </Typography>

                        </Card>
                        <AddButtons />
                        <Divider sx={{ color: "#FFFFFF", height: 3, width: '100%', marginTop: 2 }} />
                        <StreakDisplay />
                        <MonthSelector />
                        {hydrationHistory.value == null ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                                <CircularProgress color="secondary" />
                            </Box>
                        ) : (
                            <HeatmapCalendar
                                yearMonth={historyDate.value}
                                minValue={minValue}
                                maxValue={maxValue}
                                data={hydrationHistory.value!}
                                onDayClick={(date) => {
                                    currentDate.value = date;
                                }}
                            />
                        )}
                        <LogOutButton />
                    </Box>
                </div>
            ) : (
                <Card sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 400, padding: 2, marginTop: 4 }}>
                    <CircularProgress color="secondary" />
                    <Typography variant="h6" component="div" sx={{ marginTop: 2 }}>
                        Loading hydration data...
                    </Typography>
                    <LogOutButton />
                </Card>
            )}
        </>
    )
}
