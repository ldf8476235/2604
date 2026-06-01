import { useMemo } from "react";

export type DemoState = "loading" | "error" | "empty" | "success";

export function useScenario(defaultState: DemoState = "success"): DemoState {
  return useMemo(() => {
    const params = new URLSearchParams(window.location.search);
    const state = params.get("state");
    if (state === "loading" || state === "error" || state === "empty" || state === "success") {
      return state;
    }
    return defaultState;
  }, [defaultState]);
}

