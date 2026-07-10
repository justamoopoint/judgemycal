/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of the deployed backend; empty = offline demo mode. */
  readonly VITE_BACKEND_URL?: string;
  /** Firebase web-app config JSON (public client identifiers, not secrets). */
  readonly VITE_FIREBASE_CONFIG?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
