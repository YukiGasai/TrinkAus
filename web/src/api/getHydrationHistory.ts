import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type HydrationHistoryResponse, type Result } from "../types";

export async function getHydrationHistory(date: string | null = null): Promise<Result<HydrationHistoryResponse, string>> {
    if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

    let url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/history?token=${authToken.value}`;
    if (date) {
        url += `&date=${date}`;
    }

    try {
        const response = await fetch(url, {
            method: 'GET',
        })
        if (!response.ok) {
            const error = await response.text();
            console.error('Error details:', error);
            return fail(`Failed to fetch add hydartion history: ${response.statusText} - ${error}`);
        }

        const data = await response.json() as HydrationHistoryResponse;
        return succeed(data);

    } catch (error) {
        return fail(`Error fetching add hydration history: ${error instanceof Error ? error.message : String(error)}`);
    }
}
