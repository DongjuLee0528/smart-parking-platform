import { apiClient } from '../../../shared/api/client';
import type { AuthSession } from '../types/AuthSession';

export async function getCurrentUser(): Promise<AuthSession> {
  const response = await apiClient.get<{ data: AuthSession }>('/me');
  return response.data.data;
}
