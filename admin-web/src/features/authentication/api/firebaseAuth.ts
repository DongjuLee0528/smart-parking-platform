import { getApp, getApps, initializeApp } from 'firebase/app';
import { getAuth, type Auth } from 'firebase/auth';

let auth: Auth | null | undefined;

export function getFirebaseAuth(): Auth | null {
  if (auth !== undefined) return auth;
  const { VITE_FIREBASE_API_KEY: apiKey, VITE_FIREBASE_AUTH_DOMAIN: authDomain,
    VITE_FIREBASE_PROJECT_ID: projectId, VITE_FIREBASE_APP_ID: appId } = import.meta.env;
  if (!apiKey || !authDomain || !projectId || !appId) {
    auth = null;
    return auth;
  }
  auth = getAuth(getApps().length ? getApp() : initializeApp({ apiKey, authDomain, projectId, appId }));
  return auth;
}
