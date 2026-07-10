import { useState } from "react";
import {
  DEFAULT_PERSONA,
  type Persona,
} from "../domain/models";
import { PERSONA_DESCRIPTIONS, PERSONA_LABELS } from "../domain/personaEngine";
import { SupportDialog } from "./components";
import { MealTab, ShoppingTab, TodayTab, WorkoutTab } from "./tabs";
import { useAppState } from "./useAppState";

const TABS = ["home", "capture", "shopping", "workout"] as const;
type Tab = (typeof TABS)[number];

const TAB_LABELS: Record<Tab, string> = {
  home: "Today",
  capture: "Meal",
  shopping: "Shopping",
  workout: "Workout",
};

// Buddy first: it's the default; Auntie/Coach are opt-in.
const PERSONA_ORDER: Persona[] = [DEFAULT_PERSONA, "auntie", "coach"];

export default function App() {
  const state = useAppState();
  const [tab, setTab] = useState<Tab>("home");
  const [personaSheet, setPersonaSheet] = useState(false);

  return (
    <div className="app">
      <header className="topbar">
        <h1>JudgeMyCal</h1>
        <button
          className="secondary"
          data-testid="persona_switcher"
          onClick={() => setPersonaSheet(true)}
        >
          {PERSONA_LABELS[state.persona]}
        </button>
      </header>

      <main className="content">
        {tab === "home" && <TodayTab meals={state.meals} />}
        {tab === "capture" && <MealTab state={state} />}
        {tab === "shopping" && <ShoppingTab state={state} />}
        {tab === "workout" && <WorkoutTab state={state} />}
      </main>

      <nav className="tabs">
        {TABS.map((t) => (
          <button
            key={t}
            className={tab === t ? "active" : ""}
            data-testid={`nav_${t}`}
            onClick={() => setTab(t)}
          >
            {TAB_LABELS[t]}
          </button>
        ))}
      </nav>

      {/* The safety floor owns the screen when triggered — everything else waits. */}
      {state.supportMessage && (
        <SupportDialog
          message={state.supportMessage}
          onDismiss={state.dismissSupportMessage}
        />
      )}

      {personaSheet && (
        <div className="dialog-backdrop" onClick={() => setPersonaSheet(false)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <strong>Who's judging today?</strong>
            {PERSONA_ORDER.map((p) => (
              <button
                key={p}
                className={`persona-option ${p === state.persona ? "selected" : ""}`}
                data-testid={`persona_${p}`}
                onClick={() => {
                  state.setPersona(p);
                  setPersonaSheet(false);
                }}
              >
                <strong>{PERSONA_LABELS[p]}</strong>
                <div className="muted">{PERSONA_DESCRIPTIONS[p]}</div>
              </button>
            ))}
            <p className="muted" style={{ margin: 0 }}>
              JudgeMyCal is a wellness companion, not a medical or diagnostic tool.
              Calorie estimates are honest ranges, never exact numbers.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
