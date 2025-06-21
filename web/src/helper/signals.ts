import { signal, computed } from "@preact/signals-react";
import type { Streak } from "../types";

export const currentDate = signal<Date>(new Date());
export const historyDate = signal<Date>(new Date());

export const serverIP = signal<string>(localStorage.getItem('serverIP') || "");
export const authToken = signal<string>(localStorage.getItem('authToken') || "");
export const isConfigured = computed(() => {
    return serverIP.value.length > 0 && authToken.value.length > 0;
})


// Configurations from the phone
export const isMetric = signal<boolean>(true);
export const hydrationGoal = signal<number | null>(null);
export const addHydrationAmountSmall = signal<number>(0);
export const addHydrationAmountMedium = signal<number>(0);
export const addHydrationAmountLarge = signal<number>(0);

// Historyic data from the phone
export const currentHydration = signal<number | null>(null);
export const longestStreak = signal<Streak | null>(null);
export const currentStreak = signal<Streak | null>(null);


export const hydrationHistory = signal<Record<string, number> | null>(null);
