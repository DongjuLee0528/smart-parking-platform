import { useState, type FormEvent } from 'react';
import { useAuthSession } from '../hooks/useAuthSession';

export function AdminLoginForm() {
  const { login, error, configured } = useAuthSession();
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState('');

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(true);
    setFormError('');
    try {
      await login(String(data.get('email')).trim(), String(data.get('password')));
      form.reset();
    } catch {
      setFormError('Sign-in failed. Check your email and password.');
    } finally {
      setBusy(false);
    }
  }

  return <form onSubmit={submit}>
    <label htmlFor="admin-email">Email</label>
    <input id="admin-email" name="email" type="email" autoComplete="username" required />
    <label htmlFor="admin-password">Password</label>
    <input id="admin-password" name="password" type="password" autoComplete="current-password" required />
    <button type="submit" disabled={!configured || busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
    {!configured && <p role="alert">Firebase web configuration is missing.</p>}
    {(formError || error) && <p role="alert">{formError || error}</p>}
  </form>;
}
