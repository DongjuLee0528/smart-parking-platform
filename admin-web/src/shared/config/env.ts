export type Env = {
  apiBaseUrl: string;
};

export const env: Env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
};
