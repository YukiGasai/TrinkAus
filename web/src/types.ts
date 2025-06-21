
export interface HydrationResponse {
    hydration: number;
}

export interface GoalResponse {
    goal: number;
}

export interface UnitResponse {
    isMetric: boolean;
}

export interface AddHydrationAmountsResponse {
    small: number;
    medium: number;
    large: number;
}


export interface Streak {
    length: number;
    startDate: string | null;
}

export interface StreaksResponse {
    longestStreak: Streak;
    currentStreak: Streak;
}

export interface HydrationHistoryResponse {
    history: Record<string, number>;
}

export type Failure<E = unknown> = {
  _tag: "Failure";
  error: E;
};

export type Success<T = void> = {
  _tag: "Success";
  data: T;
};

export type Result<T = void, E = unknown> = Failure<E> | Success<T>;

export function succeed<T = void>(data: T): Success<T> {
  return {
    _tag: "Success",
    data: data,
  };
}

export function fail<E = unknown>(error: E): Failure<E> {
  return {
    _tag: "Failure",
    error: error,
  };
}

