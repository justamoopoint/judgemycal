import { useCallback, useState } from "react";
import { agent, storage } from "../data/graph";
import * as calorieEstimator from "../domain/calorieEstimator";
import type {
  LoggedMeal,
  MealEstimate,
  Persona,
  SwapResult,
  WorkoutSession,
} from "../domain/models";
import * as safetyGuard from "../domain/safetyGuard";

/**
 * One state hook for the whole app — a single persona and a single agent
 * session shared across all three capabilities.
 *
 * Safety: every piece of user free text passes the SafetyGuard BEFORE anything
 * else happens. On a distress signal nothing is sent anywhere and the support
 * message takes over the screen — the in-browser twin of the backend's
 * non-overridable before-model callback.
 */
export function useAppState() {
  const [persona, setPersonaState] = useState<Persona>(() => storage.getPersona());
  const [meals, setMeals] = useState<LoggedMeal[]>(() => storage.getMeals());
  const [workouts, setWorkouts] = useState<WorkoutSession[]>(() => storage.getWorkouts());
  const [estimate, setEstimate] = useState<MealEstimate | null>(null);
  const [swaps, setSwaps] = useState<SwapResult | null>(null);
  const [workout, setWorkout] = useState<WorkoutSession | null>(null);
  const [basket, setBasket] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [supportMessage, setSupportMessage] = useState<string | null>(null);

  /** True when the text tripped the safety floor (and now owns the screen). */
  const guarded = useCallback((text: string): boolean => {
    if (safetyGuard.isDistress(text)) {
      setSupportMessage(safetyGuard.SUPPORT_MESSAGE);
      return true;
    }
    return false;
  }, []);

  const setPersona = useCallback((p: Persona) => {
    storage.setPersona(p);
    setPersonaState(p);
  }, []);

  const estimateMeal = useCallback(
    async (imageJpegBase64: string | null, note: string) => {
      if (guarded(note)) return;
      setBusy(true);
      try {
        setEstimate(await agent.estimateMeal(persona, imageJpegBase64, note));
      } finally {
        setBusy(false);
      }
    },
    [persona, guarded],
  );

  /** One-tap portion correction: recompute locally, keep the honest band. */
  const correctPortion = useCallback((itemName: string, grams: number) => {
    setEstimate((est) =>
      est ? calorieEstimator.withPortion(est, itemName, grams) : est,
    );
  }, []);

  const logMeal = useCallback(() => {
    setEstimate((est) => {
      if (est) {
        setMeals(
          storage.addMeal({
            name: est.items[0]?.name ?? "meal",
            kcal: est.totalKcal,
            kcalLow: est.totalLow,
            kcalHigh: est.totalHigh,
            timestampMillis: Date.now(),
          }),
        );
      }
      return null;
    });
  }, []);

  const addBasketItem = useCallback(
    (item: string) => {
      if (guarded(item)) return;
      const trimmed = item.trim();
      if (trimmed) setBasket((b) => [...b, trimmed]);
    },
    [guarded],
  );

  const removeBasketItem = useCallback((index: number) => {
    setBasket((b) => b.filter((_, i) => i !== index));
  }, []);

  const suggestSwaps = useCallback(async () => {
    if (basket.length === 0) return;
    setBusy(true);
    try {
      setSwaps(await agent.suggestSwaps(persona, basket));
    } finally {
      setBusy(false);
    }
  }, [basket, persona]);

  const buildWorkout = useCallback(
    async (goal: string, minutes: number, equipment: string) => {
      if (guarded(goal) || guarded(equipment)) return;
      setBusy(true);
      try {
        const session = await agent.buildWorkout(persona, goal, minutes, equipment);
        setWorkout(session);
        setWorkouts(storage.addWorkout(session));
      } finally {
        setBusy(false);
      }
    },
    [persona, guarded],
  );

  return {
    persona,
    setPersona,
    meals,
    workouts,
    estimate,
    setEstimate,
    swaps,
    workout,
    basket,
    busy,
    supportMessage,
    dismissSupportMessage: () => setSupportMessage(null),
    estimateMeal,
    correctPortion,
    logMeal,
    addBasketItem,
    removeBasketItem,
    suggestSwaps,
    buildWorkout,
  };
}

export type AppState = ReturnType<typeof useAppState>;
