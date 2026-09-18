import { createBrowserRouter } from 'react-router';
import { DashboardPage } from '../../pages/DashboardPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <DashboardPage />,
  },
]);
