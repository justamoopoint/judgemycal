import * as calorieEstimator from "../domain/calorieEstimator";
import type {
  MealEstimate,
  Persona,
  SwapResult,
  WorkoutSession,
} from "../domain/models";
import * as personaEngine from "../domain/personaEngine";
import * as swapEngine from "../domain/swapEngine";
import * as workoutBuilder from "../domain/workoutBuilder";
import { AgentApi, ApiError } from "./agentApi";
import * as mapper from "./responseMapper";
import type { WirePart } from "./wire";

/** The one seam between the UI and "where answers come from". */
export interface AgentService {
  estimateMeal(
    persona: Persona,
    imageJpegBase64: string | null,
    note: string,
  ): Promise<MealEstimate>;
  suggestSwaps(persona: Persona, items: string[]): Promise<SwapResult>;
  buildWorkout(
    persona: Persona,
    goal: string,
    minutes: number,
    equipment: string,
  ): Promise<WorkoutSession>;
}

/** The offline path: the in-browser engines, flagged `fromFallback`. */
export class FallbackAgentService implements AgentService {
  async estimateMeal(
    persona: Persona,
    imageJpegBase64: string | null,
    note: string,
  ): Promise<MealEstimate> {
    const seed = imageJpegBase64 ? imageJpegBase64.length : Date.now();
    const est = calorieEstimator.estimate(seed, note);
    return { ...est, personaReaction: personaEngine.react(persona, est) };
  }

  async suggestSwaps(_persona: Persona, items: string[]): Promise<SwapResult> {
    return swapEngine.suggestSwaps(items);
  }

  async buildWorkout(
    _persona: Persona,
    goal: string,
    minutes: number,
    equipment: string,
  ): Promise<WorkoutSession> {
    return workoutBuilder.buildSession(goal, minutes, equipment);
  }
}

export interface SessionStore {
  getSessionId(): string | null;
  setSessionId(id: string | null): void;
}

/**
 * Talks to the deployed agent over the ADK wire API. One persona + one session
 * across all capabilities; the session id is persisted so backend memory
 * survives page reloads, and the persona rides along as a stateDelta.
 */
export class BackendAgentService implements AgentService {
  constructor(
    private readonly api: AgentApi,
    private readonly sessionStore: SessionStore,
    /** Returns the signed-in Firebase UID, signing in anonymously if needed. */
    private readonly uidProvider: () => Promise<string>,
    private readonly appName = "judgemycal",
  ) {}

  async estimateMeal(
    persona: Persona,
    imageJpegBase64: string | null,
    note: string,
  ): Promise<MealEstimate> {
    const parts: WirePart[] = [
      {
        text:
          "Please estimate the calories in this meal photo." +
          (note.trim() ? ` Note from me: ${note}` : ""),
      },
    ];
    if (imageJpegBase64) {
      parts.push({ inlineData: { mimeType: "image/jpeg", data: imageJpegBase64 } });
    }
    const events = await this.runTurn(persona, parts);
    const structured = mapper.functionResponse(events, "estimate_meal");
    if (!structured) throw new Error("no estimate_meal result in agent response");
    return mapper.mealEstimate(structured, mapper.finalText(events));
  }

  async suggestSwaps(persona: Persona, items: string[]): Promise<SwapResult> {
    const events = await this.runTurn(persona, [
      {
        text: `Here's my shopping basket: ${items.join(", ")}. Any goal-friendly swaps you'd suggest?`,
      },
    ]);
    const structured = mapper.functionResponse(events, "suggest_swaps");
    if (!structured) throw new Error("no suggest_swaps result in agent response");
    return mapper.swapResult(structured, mapper.finalText(events));
  }

  async buildWorkout(
    persona: Persona,
    goal: string,
    minutes: number,
    equipment: string,
  ): Promise<WorkoutSession> {
    const events = await this.runTurn(persona, [
      {
        text: `Build me a workout session. Goal: ${goal}. I have ${minutes} minutes. Equipment: ${equipment}.`,
      },
    ]);
    const structured = mapper.functionResponse(events, "build_session");
    if (!structured) throw new Error("no build_session result in agent response");
    return mapper.workoutSession(structured, mapper.finalText(events));
  }

  private async runTurn(persona: Persona, parts: WirePart[]) {
    const uid = await this.uidProvider();
    const sessionId = await this.ensureSession(uid, persona);
    const request = {
      appName: this.appName,
      userId: uid,
      sessionId,
      newMessage: { role: "user", parts },
      stateDelta: { persona },
    };
    try {
      return await this.api.run(request);
    } catch (e) {
      // The stored session can vanish (backend redeploy without persistent
      // sessions): recreate once and retry rather than dying.
      if (!(e instanceof ApiError) || e.status !== 404) throw e;
      this.sessionStore.setSessionId(null);
      return await this.api.run({
        ...request,
        sessionId: await this.ensureSession(uid, persona),
      });
    }
  }

  private async ensureSession(uid: string, persona: Persona): Promise<string> {
    const existing = this.sessionStore.getSessionId();
    if (existing) {
      try {
        return (await this.api.getSession(this.appName, uid, existing)).id;
      } catch (e) {
        if (!(e instanceof ApiError) || e.status !== 404) throw e;
        this.sessionStore.setSessionId(null);
      }
    }
    const created = await this.api.createSession(this.appName, uid, { persona });
    this.sessionStore.setSessionId(created.id);
    return created.id;
  }
}

/**
 * Backend-first with graceful degradation: a failed or timed-out backend call
 * falls back to the in-browser engines instead of a dead screen. `backend ===
 * null` means no backend URL configured — pure offline demo mode.
 */
export class CompositeAgentService implements AgentService {
  constructor(
    private readonly backend: AgentService | null,
    private readonly fallback: AgentService,
  ) {}

  estimateMeal(persona: Persona, image: string | null, note: string) {
    return this.withFallback((s) => s.estimateMeal(persona, image, note));
  }

  suggestSwaps(persona: Persona, items: string[]) {
    return this.withFallback((s) => s.suggestSwaps(persona, items));
  }

  buildWorkout(persona: Persona, goal: string, minutes: number, equipment: string) {
    return this.withFallback((s) => s.buildWorkout(persona, goal, minutes, equipment));
  }

  private async withFallback<T>(call: (s: AgentService) => Promise<T>): Promise<T> {
    if (!this.backend) return call(this.fallback);
    try {
      return await call(this.backend);
    } catch (e) {
      console.warn("Backend call failed; using in-browser fallback", e);
      return call(this.fallback);
    }
  }
}
