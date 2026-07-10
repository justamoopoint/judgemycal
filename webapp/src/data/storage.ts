import type { LoggedMeal, Persona, WorkoutSession } from "../domain/models";
import { personaFromKey } from "../domain/models";
import type { SessionStore } from "./agentService";

/**
 * Local persistence in localStorage: meal log, workout history, persona, and
 * the backend session id (so agent memory continues across page loads).
 * Persona-agnostic — switching companion never changes the data.
 */
export class Storage implements SessionStore {
  private read<T>(key: string, fallback: T): T {
    try {
      const raw = localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as T) : fallback;
    } catch {
      return fallback;
    }
  }

  private write(key: string, value: unknown): void {
    localStorage.setItem(key, JSON.stringify(value));
  }

  getMeals(): LoggedMeal[] {
    return this.read<LoggedMeal[]>("jmc.meals", []);
  }

  addMeal(meal: LoggedMeal): LoggedMeal[] {
    const meals = [...this.getMeals(), meal];
    this.write("jmc.meals", meals);
    return meals;
  }

  getWorkouts(): WorkoutSession[] {
    return this.read<WorkoutSession[]>("jmc.workouts", []);
  }

  addWorkout(session: WorkoutSession): WorkoutSession[] {
    // Keep local history bounded; the backend session holds long-term memory.
    const workouts = [...this.getWorkouts(), session].slice(-20);
    this.write("jmc.workouts", workouts);
    return workouts;
  }

  getPersona(): Persona {
    return personaFromKey(this.read<string | null>("jmc.persona", null));
  }

  setPersona(persona: Persona): void {
    this.write("jmc.persona", persona);
  }

  getSessionId(): string | null {
    return this.read<string | null>("jmc.sessionId", null);
  }

  setSessionId(id: string | null): void {
    if (id === null) localStorage.removeItem("jmc.sessionId");
    else this.write("jmc.sessionId", id);
  }
}
