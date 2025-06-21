import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type HydrationResponse, type Result } from "../types";

export async function addHydration(amount: number, date: string | null = null): Promise<Result<number, string>> {
    if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

    let url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/hydration?token=${authToken.value}&hydration=${amount}`;
    if (date) {
        url += `&date=${date}`;
    }

    try {
        const response = await fetch(url, {
            method: 'POST',
        })
        if (!response.ok) {
            const error = await response.text();
            console.error('Error updating hydration:', error);
            return fail(`Failed to update hydration: ${response.statusText} - ${error}`);
        }
        const data = await response.json() as HydrationResponse;
        return succeed(data.hydration);

    } catch (error) {
        return fail(`Error updating hydration: ${error instanceof Error ? error.message : String(error)}`);
    }
}
