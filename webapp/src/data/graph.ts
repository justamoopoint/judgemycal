import { AgentApi } from "./agentApi";
import {
  BackendAgentService,
  CompositeAgentService,
  FallbackAgentService,
  type AgentService,
} from "./agentService";
import { ensureSignedIn, getIdToken } from "./firebase";
import { Storage } from "./storage";

/**
 * Production wiring, mirroring the Android AppGraph: backend when
 * VITE_BACKEND_URL is set, in-browser fallback behind it either way.
 */
export const storage = new Storage();

function buildAgent(): AgentService {
  const rawUrl = (import.meta.env.VITE_BACKEND_URL as string | undefined)?.trim() ?? "";
  const baseUrl = rawUrl.replace(/\/+$/, "");
  const backend = baseUrl
    ? new BackendAgentService(
        new AgentApi(baseUrl, getIdToken),
        storage,
        async () => (await ensureSignedIn()).uid,
      )
    : null;
  return new CompositeAgentService(backend, new FallbackAgentService());
}

export const agent: AgentService = buildAgent();
