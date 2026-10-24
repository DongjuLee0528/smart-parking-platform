import { Navigate, Outlet } from 'react-router';
import { useAuthSession } from '../hooks/useAuthSession';

export function RequireAdmin() {
  const { user, loading } = useAuthSession();
  if (loading) return <main className="shell"><p role="status">Checking session…</p></main>;
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}
