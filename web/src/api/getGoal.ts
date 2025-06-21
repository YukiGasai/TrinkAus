import { API_PORT, API_PROTOCOL } from "../helper/constants";
import { authToken, isConfigured, serverIP } from "../helper/signals";
import { fail, succeed, type GoalResponse, type Result } from "../types";

export async function getGoal(): Promise<Result<number, string>> {
        if (!isConfigured.value) return fail("Not configured yet. Please set up the server IP and auth token.");

        const url = `${API_PROTOCOL}://${serverIP.value}:${API_PORT}/goal?token=${authToken.value}`;

        try {
        const response = await fetch(url, {
            method: 'GET',
        })
        if (!response.ok) {
            const error = await response.text();
            console.error('Error details:', error);
            return fail(`Failed to fetch goal: ${response.statusText} - ${error}`);
        }

        const data = await response.json() as GoalResponse;
        return succeed(data.goal);

    } catch (error) {
        return fail(`Error fetching goal: ${error instanceof Error ? error.message : String(error)}`);
    }
}
