import { Navigate } from 'react-router';
import { AdminLoginForm } from '../components/AdminLoginForm';
import { useAuthSession } from '../hooks/useAuthSession';

export function AdminLoginPage() {
  const { user, loading } = useAuthSession();
  if (loading) return <main className="shell"><p role="status">Checking session…</p></main>;
  if (user) return <Navigate to="/" replace />;
  return <main className="shell"><h1>Smart Parking Admin</h1><AdminLoginForm /></main>;
}
