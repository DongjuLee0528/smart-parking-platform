export type AuthSession = {
  id: string;
  email: string;
  displayName: string;
  role: 'USER' | 'ADMIN';
  status: 'ACTIVE' | 'DISABLED';
};
