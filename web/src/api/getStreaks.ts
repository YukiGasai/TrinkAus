import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type Result, type StreaksResponse } from "../types";

export async function getStreaks(): Promise<Result<StreaksResponse, string>> {
    if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

    const url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/streaks?token=${authToken.value}`;

    try {
        const response = await fetch(url, {
            method: 'GET',
        })
        if (!response.ok) {
            const error = await response.text();
            console.error('Error details:', error);
            return fail(`Failed to fetch steaks: ${response.statusText} - ${error}`);
        }

        const data = await response.json() as StreaksResponse;
        return succeed(data);

    } catch (error) {
        return fail(`Error fetching streaks: ${error instanceof Error ? error.message : String(error)}`);
    }
}
