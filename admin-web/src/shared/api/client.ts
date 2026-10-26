import axios from 'axios';
import { signOut } from 'firebase/auth';
import { getFirebaseAuth } from '../../features/authentication/api/firebaseAuth';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
});

apiClient.interceptors.request.use(async (config) => {
  const auth = getFirebaseAuth();
  if (auth?.currentUser) {
    try {
      config.headers.set('Authorization', `Bearer ${await auth.currentUser.getIdToken()}`);
    } catch (error) {
      await signOut(auth);
      throw error;
    }
  }
  return config;
});

apiClient.interceptors.response.use(undefined, async (error: unknown) => {
  if (axios.isAxiosError(error) && error.response?.status === 401) {
    const auth = getFirebaseAuth();
    if (auth?.currentUser) await signOut(auth);
  }
  return Promise.reject(error);
});
