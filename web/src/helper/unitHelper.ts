import { isMetric } from "./signals";

export function getUnitString(): string {
    return isMetric.value ? "ml" : "oz";
}

export function getUnitStringLarge(): string {
    return isMetric.value ? "L" : "oz";
}

export function getUnitStringWithValue(value: number): string {
    return `${displayNumber(value)} ${getUnitString()}`;
}

export function getUnitStringLargeWithValue(value: number): string {
    if (!isMetric.value) {
        return `${displayNumber(value)} oz`;
    }

    return `${displayNumber(value / 1000)} ${getUnitStringLarge()}`;
}

export function displayLargeNumber(number: number): string {
    if (!isMetric.value) {
        return number.toFixed(0)
    }
    return displayNumber(number)
}

export function displayNumber(number: number): string {
  return number.toFixed(3).replace(/\.?0+$/, "");
}



export function isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() &&
        date.getMonth() === today.getMonth() &&
        date.getFullYear() === today.getFullYear();
}

export function dateToString(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

export function dateToDisplayString(date: Date): string {

    if (isToday(date)) {
        return "Today";
    }
    // if yesterday
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    if (date.getDate() === yesterday.getDate() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getFullYear() === yesterday.getFullYear()) {
        return "Yesterday";
    }
    // If the day is within the last 7 days, return the day of the week
    const daysAgo = Math.floor((new Date().getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
    if (daysAgo < 7) {
        const daysOfWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
        return daysOfWeek[date.getDay()];
    }
    const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
    return date.toLocaleDateString(undefined, options);
}
