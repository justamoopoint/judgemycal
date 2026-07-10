import { initializeApp, type FirebaseApp } from "firebase/app";
import {
  getAuth,
  signInAnonymously,
  type Auth,
  type User,
} from "firebase/auth";

/**
 * Guarded Firebase init, mirroring the Android app's posture: no config means
 * pure offline demo mode, never a crash. `VITE_FIREBASE_CONFIG` holds the
 * public web-app config JSON from the Firebase console — client identifiers,
 * not secrets (same class of values as google-services.json).
 */

let app: FirebaseApp | null = null;
let auth: Auth | null = null;

function tryInit(): Auth | null {
  if (auth) return auth;
  const raw = import.meta.env.VITE_FIREBASE_CONFIG as string | undefined;
  if (!raw) return null;
  try {
    app = initializeApp(JSON.parse(raw));
    auth = getAuth(app);
    return auth;
  } catch (e) {
    console.warn("Firebase unavailable — offline demo mode", e);
    return null;
  }
}

/** Anonymous sign-in is enough for v1 — no real accounts. */
export async function ensureSignedIn(): Promise<User> {
  const a = tryInit();
  if (!a) throw new Error("Firebase not configured");
  if (a.currentUser) return a.currentUser;
  const cred = await signInAnonymously(a);
  return cred.user;
}

export async function getIdToken(): Promise<string> {
  const user = await ensureSignedIn();
  return user.getIdToken();
}
