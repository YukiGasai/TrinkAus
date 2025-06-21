import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type AddHydrationAmountsResponse, type Result } from "../types";

export async function getAddHydrationAmounts(): Promise<Result<AddHydrationAmountsResponse, string>> {
        if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

        const url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/addHydrationAmounts?token=${authToken.value}`;

        try {
        const response = await fetch(url, {
            method: 'GET',
        })
        if (!response.ok) {
            const error = await response.text();
            console.error('Error details:', error);
            return fail(`Failed to fetch add hydartion amounts: ${response.statusText} - ${error}`);
        }

        const data = await response.json() as AddHydrationAmountsResponse;
        return succeed(data);

    } catch (error) {
        return fail(`Error fetching add hydration amounts: ${error instanceof Error ? error.message : String(error)}`);
    }
}
