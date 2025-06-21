import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type HydrationResponse, type Result } from "../types";

export async function getHydration(date: string | null = null): Promise<Result<number, string>> {
        if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

        let url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/hydration?token=${authToken.value}`;
        if (date) {
            url += `&date=${date}`;
        }

        try {
        const response = await fetch(url, {
            method: 'GET',
        })
        if (!response.ok) {
            console.error('Failed to fetch hydration history:', response.statusText);
            const error = await response.text();
            console.error('Error details:', error);
            return fail(`Failed to fetch hydration: ${response.statusText} - ${error}`);
        }

        const data = await response.json() as HydrationResponse;
        return succeed(data.hydration);

    } catch (error) {
        return fail(`Error fetching hydration: ${error instanceof Error ? error.message : String(error)}`);
    }
}
