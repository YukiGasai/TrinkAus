import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type Result, type UnitResponse } from "../types";

export async function getUnit(): Promise<Result<boolean, string>> {
        if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

        const url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/unit?token=${authToken.value}`;

        try {
        const response = await fetch(url, {
            method: 'GET',
        })
        if (!response.ok) {
            const error = await response.text();
            console.error('Error details:', error);
            return fail(`Failed to fetch hydration unit: ${response.statusText} - ${error}`);
        }

        const data = await response.json() as UnitResponse;
        return succeed(data.isMetric);

    } catch (error) {
        return fail(`Error fetching hydration unit: ${error instanceof Error ? error.message : String(error)}`);
    }
}
