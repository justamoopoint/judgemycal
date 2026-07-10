import type { RunAgentRequest, WireEvent, WireSession } from "./wire";

/** HTTP error carrying the status so callers can react to 404 vs anything else. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

/**
 * Thin fetch client for the ADK API server. The bearer token is attached in
 * one place (the Android app's AuthInterceptor equivalent); the app never
 * talks to a model provider directly, so no key of any kind exists here.
 */
export class AgentApi {
  constructor(
    private readonly baseUrl: string,
    private readonly tokenProvider: () => Promise<string>,
  ) {}

  async createSession(
    appName: string,
    userId: string,
    state: Record<string, unknown>,
  ): Promise<WireSession> {
    return this.request(
      "POST",
      `/apps/${appName}/users/${userId}/sessions`,
      { state },
    );
  }

  async getSession(
    appName: string,
    userId: string,
    sessionId: string,
  ): Promise<WireSession> {
    return this.request(
      "GET",
      `/apps/${appName}/users/${userId}/sessions/${sessionId}`,
    );
  }

  async run(request: RunAgentRequest): Promise<WireEvent[]> {
    return this.request("POST", "/run", request);
  }

  private async request<T>(
    method: string,
    path: string,
    body?: unknown,
  ): Promise<T> {
    const token = await this.tokenProvider();
    const response = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
      throw new ApiError(response.status, `${method} ${path} -> ${response.status}`);
    }
    return (await response.json()) as T;
  }
}
