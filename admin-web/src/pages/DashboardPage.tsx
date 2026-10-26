import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { useAuthSession } from '../features/authentication/hooks/useAuthSession';

export function DashboardPage() {
  const { user, logout } = useAuthSession();
  const [floorId, setFloorId] = useState('');
  const [logoutError, setLogoutError] = useState('');
  const navigate = useNavigate();

  async function leave() {
    try {
      await logout();
    } catch {
      setLogoutError('Sign-out failed. Please try again.');
    }
  }

  function openMap(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    navigate(`/floors/${encodeURIComponent(floorId.trim())}/map`);
  }

  return (
    <main className="shell">
      <h1>Smart Parking Admin</h1>
      <p>Signed in as {user?.displayName}</p>
      <button type="button" onClick={() => void leave()}>Sign out</button>
      {logoutError && <p role="alert">{logoutError}</p>}
      <form onSubmit={openMap}>
        <label htmlFor="floor-id">Floor ID</label>
        <input id="floor-id" value={floorId} onChange={(event) => setFloorId(event.target.value)} required />
        <button type="submit">Open floor drawing</button>
      </form>
    </main>
  );
}
