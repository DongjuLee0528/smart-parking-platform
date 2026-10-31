import { createBrowserRouter } from 'react-router';
import { DashboardPage } from '../../pages/DashboardPage';
import { AdminLoginPage } from '../../features/authentication/pages/AdminLoginPage';
import { RequireAdmin } from '../../features/authentication/components/RequireAdmin';
import { FloorMapEditorPage } from '../../features/floor-map-editor/pages/FloorMapEditorPage';
import { FloorManagementPage } from '../../features/floor-management/pages/FloorManagementPage';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <AdminLoginPage />,
  },
  {
    element: <RequireAdmin />,
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/floors/:floorId/map', element: <FloorMapEditorPage /> },
      { path: '/parking-lots/:lotId/floors', element: <FloorManagementPage /> },
    ],
  },
]);
