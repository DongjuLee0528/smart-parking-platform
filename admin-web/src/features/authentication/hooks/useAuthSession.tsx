import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { browserSessionPersistence, onIdTokenChanged, setPersistence, signInWithEmailAndPassword,
  signOut } from 'firebase/auth';
import { getCurrentUser } from '../api/authApi';
import { getFirebaseAuth } from '../api/firebaseAuth';
import type { AuthSession } from '../types/AuthSession';

type AuthContextValue = {
  user: AuthSession | null;
  loading: boolean;
  error: string | null;
  configured: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const auth = getFirebaseAuth();
  const [user, setUser] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(Boolean(auth));
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!auth) return;
    const currentAuth = auth;
    let active = true;
    let generation = 0;
    async function rejectSession(message: string) {
      setError(message);
      try {
        await signOut(currentAuth);
      } catch {
        setError('Could not clear the Firebase session. Close this tab and try again.');
      }
    }
    const unsubscribe = onIdTokenChanged(currentAuth, async (firebaseUser) => {
      const current = ++generation;
      if (!firebaseUser) {
        if (active) { setUser(null); setLoading(false); }
        return;
      }
      if (active) { setUser(null); setLoading(true); }
      try {
        const candidate = await getCurrentUser();
        if (!active || current !== generation) return;
        if (candidate.role !== 'ADMIN' || candidate.status !== 'ACTIVE') {
          await rejectSession('This account does not have administrator access.');
          return;
        }
        setUser(candidate);
        setError(null);
      } catch {
        if (!active || current !== generation) return;
        await rejectSession('Your session could not be verified. Please sign in again.');
      } finally {
        if (active && current === generation) setLoading(false);
      }
    });
    return () => { active = false; generation++; unsubscribe(); };
  }, [auth]);

  async function login(email: string, password: string) {
    if (!auth) throw new Error('Firebase configuration is missing');
    setError(null);
    await setPersistence(auth, browserSessionPersistence);
    await signInWithEmailAndPassword(auth, email, password);
  }

  async function logout() {
    if (!auth) throw new Error('Firebase configuration is missing');
    await signOut(auth);
  }

  return <AuthContext.Provider value={{ user, loading, error, configured: Boolean(auth), login, logout }}>
    {children}
  </AuthContext.Provider>;
}

export function useAuthSession() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('AuthProvider is missing');
  return context;
}
