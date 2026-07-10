import { useRef, useState } from "react";
import type { LoggedMeal } from "../domain/models";
import { ConfidenceBand, ConfidencePill, OfflineBadge } from "./components";
import type { AppState } from "./useAppState";

export function TodayTab({ meals }: { meals: LoggedMeal[] }) {
  const dayStart = new Date();
  dayStart.setHours(0, 0, 0, 0);
  const today = meals.filter((m) => m.timestampMillis >= dayStart.getTime());

  if (today.length === 0) {
    return (
      <>
        <h2>Today</h2>
        <p className="muted" data-testid="home_empty">
          Nothing logged yet — and that's okay. Snap a meal when you're ready.
        </p>
      </>
    );
  }
  return (
    <>
      <h2>Today</h2>
      {/* The daily total is a range too — sums of ranges, honestly. */}
      <p data-testid="home_total" style={{ color: "var(--primary)", fontWeight: 600 }}>
        Logged so far: about {today.reduce((s, m) => s + m.kcal, 0)} kcal (
        {today.reduce((s, m) => s + m.kcalLow, 0)}–
        {today.reduce((s, m) => s + m.kcalHigh, 0)})
      </p>
      {[...today].reverse().map((meal, i) => (
        <div className="card" key={i}>
          <div className="row">
            <div>
              <strong>{meal.name}</strong>
              <div className="muted">
                {new Date(meal.timestampMillis).toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </div>
            </div>
            <span>
              ~{meal.kcal} ({meal.kcalLow}–{meal.kcalHigh})
            </span>
          </div>
        </div>
      ))}
    </>
  );
}

export function MealTab({ state }: { state: AppState }) {
  const [note, setNote] = useState("");
  const fileInput = useRef<HTMLInputElement>(null);
  const { estimate } = state;

  const onFile = async (file: File | undefined) => {
    if (!file) return;
    const buffer = await file.arrayBuffer();
    let binary = "";
    for (const byte of new Uint8Array(buffer)) binary += String.fromCharCode(byte);
    await state.estimateMeal(btoa(binary), note);
  };

  if (estimate === null) {
    return (
      <>
        <h2>Judge my meal</h2>
        <input
          type="text"
          placeholder='Anything I should know? (e.g. "no oil")'
          value={note}
          onChange={(e) => setNote(e.target.value)}
          data-testid="note_input"
        />
        {state.busy ? (
          <div className="spinner" />
        ) : (
          <>
            <button className="primary" onClick={() => fileInput.current?.click()}>
              Photo of my plate
            </button>
            <input
              ref={fileInput}
              type="file"
              accept="image/*"
              capture="environment"
              hidden
              onChange={(e) => onFile(e.target.files?.[0])}
            />
            <button
              className="text"
              data-testid="demo_meal"
              onClick={() => state.estimateMeal(null, note)}
            >
              Try a demo meal
            </button>
          </>
        )}
      </>
    );
  }

  return (
    <div className="card">
      {estimate.fromFallback && <OfflineBadge />}
      <ConfidenceBand estimate={estimate} />
      {estimate.items.map((item) => (
        <div key={item.name}>
          <div className="row">
            <div>
              <strong>{item.name}</strong>
              <div className="muted">
                {item.grams} g · ~{item.kcal} kcal ({item.kcalLow}–{item.kcalHigh})
              </div>
            </div>
            <ConfidencePill confidence={item.confidence} />
          </div>
          {/* One-tap portion correction: honest ranges invite correction. */}
          <div>
            {[0.75, 1.25].map((factor) => {
              const grams = Math.trunc(item.grams * factor);
              return (
                <button
                  key={factor}
                  className="text"
                  data-testid={`portion_${item.name}_${grams}`}
                  onClick={() => state.correctPortion(item.name, grams)}
                >
                  {factor < 1 ? `Smaller (${grams} g)` : `Bigger (${grams} g)`}
                </button>
              );
            })}
          </div>
        </div>
      ))}
      {estimate.personaReaction && (
        <div className="persona-card" data-testid="persona_reaction">
          {estimate.personaReaction}
        </div>
      )}
      <button
        className="primary"
        data-testid="log_meal"
        onClick={() => {
          state.logMeal();
          setNote("");
        }}
      >
        Log this meal
      </button>
      <button className="text" onClick={() => state.setEstimate(null)}>
        Start over
      </button>
    </div>
  );
}

export function ShoppingTab({ state }: { state: AppState }) {
  const [input, setInput] = useState("");
  const { swaps } = state;
  return (
    <>
      <h2>Shopping swaps</h2>
      <div className="row">
        <input
          type="text"
          placeholder="Add a basket item (e.g. white rice)"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          data-testid="basket_input"
        />
        <button
          className="secondary"
          data-testid="basket_add"
          onClick={() => {
            state.addBasketItem(input);
            setInput("");
          }}
        >
          Add
        </button>
      </div>
      {state.basket.length === 0 ? (
        <p className="muted">Add a few items and I'll suggest goal-friendly swaps.</p>
      ) : (
        <>
          <div className="chips">
            {state.basket.map((item, i) => (
              <button
                key={`${item}-${i}`}
                className="chip"
                onClick={() => state.removeBasketItem(i)}
              >
                {item} ✕
              </button>
            ))}
          </div>
          {state.busy ? (
            <div className="spinner" />
          ) : (
            <button className="primary" data-testid="suggest_swaps" onClick={state.suggestSwaps}>
              Suggest swaps
            </button>
          )}
        </>
      )}
      {swaps && (
        <>
          {swaps.fromFallback && <OfflineBadge />}
          {swaps.swaps.map((s, i) => (
            <div className="card" key={i}>
              <strong>{s.item}</strong>
              <span className="muted" data-testid="swap_suggestion">
                {s.swap}
              </span>
            </div>
          ))}
          {swaps.swaps.length === 0 && (
            <p>Your basket already looks aligned with your goals — nothing to suggest.</p>
          )}
          {swaps.personaReaction && (
            <div className="persona-card">{swaps.personaReaction}</div>
          )}
          <p className="muted">Suggestions only — your call, no pressure.</p>
        </>
      )}
    </>
  );
}

export function WorkoutTab({ state }: { state: AppState }) {
  const [goal, setGoal] = useState("general fitness");
  const [minutes, setMinutes] = useState(30);
  const [equipment, setEquipment] = useState("none");
  const { workout } = state;
  return (
    <>
      <h2>Workout</h2>
      <input
        type="text"
        value={goal}
        onChange={(e) => setGoal(e.target.value)}
        data-testid="workout_goal"
        aria-label="Goal"
      />
      <label className="muted">
        Time: {minutes} min
        <input
          type="range"
          min={10}
          max={90}
          value={minutes}
          onChange={(e) => setMinutes(Number(e.target.value))}
          style={{ width: "100%" }}
        />
      </label>
      <input
        type="text"
        value={equipment}
        onChange={(e) => setEquipment(e.target.value)}
        aria-label="Equipment on hand"
      />
      {state.busy ? (
        <div className="spinner" />
      ) : (
        <button
          className="primary"
          data-testid="workout_build"
          onClick={() => state.buildWorkout(goal, minutes, equipment)}
        >
          Build my session
        </button>
      )}
      {workout && (
        <div className="card" data-testid="workout_session">
          {workout.fromFallback && <OfflineBadge />}
          <strong>
            {workout.goal} · {workout.totalMinutes} min
          </strong>
          <span>Warm-up — {workout.warmupMin} min</span>
          <span>Main — {workout.mainMin} min</span>
          {workout.mainBlock.map((exercise) => (
            <span key={exercise} className="muted">
              &nbsp;&nbsp;• {exercise}
            </span>
          ))}
          <span>Cool-down — {workout.cooldownMin} min</span>
          <span className="muted">{workout.note}</span>
          {workout.personaReaction && (
            <div className="persona-card">{workout.personaReaction}</div>
          )}
        </div>
      )}
      {state.workouts.length > 0 && (
        <>
          <h2>Recent sessions</h2>
          {[...state.workouts]
            .reverse()
            .slice(0, 5)
            .map((past, i) => (
              <div className="card" key={i}>
                {past.goal} · {past.totalMinutes} min · {past.equipment}
              </div>
            ))}
        </>
      )}
    </>
  );
}
