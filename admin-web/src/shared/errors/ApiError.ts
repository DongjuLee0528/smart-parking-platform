export type ApiError = {
  code: string;
  message: string;
  traceId: string;
  details: Record<string, unknown>;
};
