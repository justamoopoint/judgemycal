/**
 * Wire DTOs for the ADK API server (google-adk 2.3.0), camelCase aliases —
 * the same shapes the Android client and backend test suite are pinned to.
 */

export interface WireBlob {
  mimeType?: string;
  /** base64-encoded bytes */
  data?: string;
}

export interface WireFunctionResponse {
  id?: string;
  name?: string;
  response?: Record<string, unknown>;
}

export interface WirePart {
  text?: string;
  inlineData?: WireBlob;
  functionResponse?: WireFunctionResponse;
}

export interface WireContent {
  role?: string;
  parts?: WirePart[];
}

export interface WireSession {
  id: string;
  appName?: string;
  userId?: string;
  state?: Record<string, unknown>;
}

export interface RunAgentRequest {
  appName: string;
  userId: string;
  sessionId: string;
  newMessage: WireContent;
  streaming?: boolean;
  stateDelta?: Record<string, unknown>;
}

export interface WireEvent {
  id?: string;
  author?: string;
  partial?: boolean;
  content?: WireContent;
}
