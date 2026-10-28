import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { browserSessionPersistence, setPersistence, signInWithEmailAndPassword, signOut } from 'firebase/auth';
import { AxiosError } from 'axios';
import { AdminLoginPage } from '../../../src/features/authentication/pages/AdminLoginPage';
import { RequireAdmin } from '../../../src/features/authentication/components/RequireAdmin';
import { AuthProvider } from '../../../src/features/authentication/hooks/useAuthSession';
import { getCurrentUser } from '../../../src/features/authentication/api/authApi';
import { apiClient } from '../../../src/shared/api/client';
import { DashboardPage } from '../../../src/pages/DashboardPage';

const state = vi.hoisted(() => ({
  listener: null as null | ((user: unknown) => Promise<void>),
  auth: { currentUser: null as null | { uid: string; getIdToken: () => Promise<string> } },
}));

vi.mock('../../../src/features/authentication/api/firebaseAuth', () => ({
  getFirebaseAuth: () => state.auth,
}));
vi.mock('../../../src/features/authentication/api/authApi', () => ({ getCurrentUser: vi.fn() }));
vi.mock('firebase/auth', () => ({
  browserSessionPersistence: {},
  onIdTokenChanged: vi.fn((_auth, listener) => { state.listener = listener; return () => { state.listener = null; }; }),
  setPersistence: vi.fn(async () => {}),
  signInWithEmailAndPassword: vi.fn(async () => {}),
  signOut: vi.fn(async () => { state.auth.currentUser = null; await state.listener?.(null); }),
}));

function renderAdmin(initial = '/') {
  render(<AuthProvider><MemoryRouter initialEntries={[initial]}><Routes>
    <Route path="/login" element={<AdminLoginPage />} />
    <Route element={<RequireAdmin />}><Route path="/" element={<DashboardPage />} /></Route>
  </Routes></MemoryRouter></AuthProvider>);
}

async function authChanged(user: typeof state.auth.currentUser) {
  await act(async () => { await state.listener?.(user); });
}

describe('administrator authentication', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    state.auth.currentUser = null;
    state.listener = null;
  });

  it('redirects signed-out users to login and submits Firebase credentials', async () => {
    renderAdmin();
    await authChanged(null);
    expect(screen.queryByText(/Signed in as/)).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'admin@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(setPersistence).toHaveBeenCalledWith(state.auth, browserSessionPersistence));
    expect(signInWithEmailAndPassword).toHaveBeenCalledWith(state.auth, 'admin@example.com', 'password');
  });

  it('opens protected content only after /me confirms an active ADMIN', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({ id: '1', email: 'admin@example.com',
      displayName: 'Admin', role: 'ADMIN', status: 'ACTIVE' });
    renderAdmin();
    state.auth.currentUser = { uid: 'admin-1', getIdToken: async () => 'admin-token' };
    await authChanged(state.auth.currentUser);
    expect(await screen.findByText('Signed in as Admin')).toBeInTheDocument();
    expect(signOut).not.toHaveBeenCalled();
  });

  it('keeps protected page state while the same user token is reverified', async () => {
    const admin = { id: '1', email: 'admin@example.com', displayName: 'Admin',
      role: 'ADMIN' as const, status: 'ACTIVE' as const };
    let finishRefresh: ((user: typeof admin) => void) | undefined;
    vi.mocked(getCurrentUser).mockResolvedValueOnce(admin).mockImplementationOnce(() =>
      new Promise<typeof admin>((resolve) => { finishRefresh = resolve; }));
    renderAdmin();
    const firebaseUser = { uid: 'admin-1', getIdToken: async () => 'admin-token' };
    state.auth.currentUser = firebaseUser;
    await authChanged(firebaseUser);
    const input = screen.getByLabelText('Floor ID');
    fireEvent.change(input, { target: { value: '42' } });

    let refresh: Promise<void> | undefined;
    act(() => { refresh = state.listener?.(firebaseUser); });
    expect(screen.getByLabelText('Floor ID')).toBe(input);
    expect(input).toHaveValue('42');
    expect(screen.queryByText('Checking session…')).not.toBeInTheDocument();

    await act(async () => { finishRefresh?.(admin); await refresh; });
    expect(getCurrentUser).toHaveBeenCalledTimes(2);
  });

  it('revokes protected access if the same user loses administrator status', async () => {
    const admin = { id: '1', email: 'admin@example.com', displayName: 'Admin',
      role: 'ADMIN' as const, status: 'ACTIVE' as const };
    vi.mocked(getCurrentUser).mockResolvedValueOnce(admin)
      .mockResolvedValueOnce({ ...admin, role: 'USER' });
    renderAdmin();
    const firebaseUser = { uid: 'admin-1', getIdToken: async () => 'admin-token' };
    state.auth.currentUser = firebaseUser;
    await authChanged(firebaseUser);
    await authChanged(firebaseUser);

    expect(signOut).toHaveBeenCalledOnce();
    expect(screen.queryByText('Signed in as Admin')).not.toBeInTheDocument();
    expect(await screen.findByText('This account does not have administrator access.')).toBeInTheDocument();
  });

  it('signs out USER accounts and denied sessions', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({ id: '2', email: 'user@example.com',
      displayName: 'User', role: 'USER', status: 'ACTIVE' });
    renderAdmin();
    state.auth.currentUser = { uid: 'user-1', getIdToken: async () => 'user-token' };
    await authChanged(state.auth.currentUser);
    expect(signOut).toHaveBeenCalledOnce();
    expect(await screen.findByText('This account does not have administrator access.')).toBeInTheDocument();
    expect(screen.queryByText(/Signed in as/)).not.toBeInTheDocument();
  });

  it('clears a session rejected by the server', async () => {
    vi.mocked(getCurrentUser).mockRejectedValue(new Error('Unauthorized'));
    renderAdmin();
    state.auth.currentUser = { uid: 'admin-1', getIdToken: async () => 'expired-token' };
    await authChanged(state.auth.currentUser);
    expect(signOut).toHaveBeenCalledOnce();
    expect(await screen.findByText('Your session could not be verified. Please sign in again.'))
      .toBeInTheDocument();
  });

  it('signs out and returns to login', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue({ id: '1', email: 'admin@example.com',
      displayName: 'Admin', role: 'ADMIN', status: 'ACTIVE' });
    renderAdmin();
    state.auth.currentUser = { uid: 'admin-1', getIdToken: async () => 'admin-token' };
    await authChanged(state.auth.currentUser);
    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    await waitFor(() => expect(signOut).toHaveBeenCalledOnce());
    expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('gets a fresh Firebase ID token for each API request', async () => {
    const getIdToken = vi.fn().mockResolvedValueOnce('first-token').mockResolvedValueOnce('second-token');
    state.auth.currentUser = { uid: 'admin-1', getIdToken };
    const headers: string[] = [];
    for (let index = 0; index < 2; index++) {
      await apiClient.get('/me', { adapter: async (config) => {
        headers.push(String(config.headers.get('Authorization')));
        return { data: {}, status: 200, statusText: 'OK', headers: {}, config };
      } });
    }
    expect(headers).toEqual(['Bearer first-token', 'Bearer second-token']);
  });

  it('signs out when the server rejects an expired token', async () => {
    state.auth.currentUser = { uid: 'admin-1', getIdToken: async () => 'expired-token' };
    await expect(apiClient.get('/me', { adapter: async (config) => {
      const response = { data: {}, status: 401, statusText: 'Unauthorized', headers: {}, config };
      throw new AxiosError('Unauthorized', 'ERR_BAD_RESPONSE', config, undefined, response);
    } })).rejects.toBeInstanceOf(AxiosError);
    expect(signOut).toHaveBeenCalledOnce();
  });
});
