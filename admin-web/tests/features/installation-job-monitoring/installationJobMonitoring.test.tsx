import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { vi } from 'vitest';
import { InstallationJobStatus } from '../../../src/features/installation-job-monitoring/components/InstallationJobStatus';
import { InstallationReadinessPage } from '../../../src/features/installation-job-monitoring/pages/InstallationReadinessPage';
import { getInstallationReadiness } from '../../../src/features/installation-job-monitoring/api/installationJobApi';

vi.mock('../../../src/features/installation-job-monitoring/api/installationJobApi', () => ({
  getInstallationReadiness: vi.fn(),
}));

function page() {
  const router = createMemoryRouter([
    { path: '/parking-lots/:lotId/installation-check', element: <InstallationReadinessPage /> },
    { path: '/parking-lots/:lotId/floors', element: <div>Floors</div> },
  ], { initialEntries: ['/parking-lots/lot-1/installation-check'] });
  render(<RouterProvider router={router} />);
}

describe('installation job monitoring skeleton', () => {
  it('exports the installation job status component', () => {
    expect(InstallationJobStatus).toBeDefined();
  });
});

describe('installation readiness page', () => {
  it('shows missing items and refreshes to a ready result', async () => {
    vi.mocked(getInstallationReadiness).mockResolvedValueOnce({
      parkingLotId: 'lot-1', ready: false,
      issues: [{ code: 'DRAWING_MISSING', floorId: 'floor-1', spaceId: null }],
    }).mockResolvedValueOnce({ parkingLotId: 'lot-1', ready: true, issues: [] });
    page();
    expect(await screen.findByText('확인할 항목 1개')).toBeInTheDocument();
    expect(screen.getByText('층 도면을 등록하세요.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '층 도면 열기' })).toHaveAttribute('href', '/floors/floor-1/map');
    fireEvent.click(screen.getByRole('button', { name: '다시 검사' }));
    expect(await screen.findByText('설치 설정 검증 통과')).toBeInTheDocument();
    expect(getInstallationReadiness).toHaveBeenCalledTimes(2);
    expect(getInstallationReadiness).toHaveBeenCalledWith('lot-1');
  });

  it('reports a failed request and allows retry', async () => {
    vi.mocked(getInstallationReadiness).mockRejectedValueOnce(new Error('Network'))
      .mockResolvedValueOnce({ parkingLotId: 'lot-1', ready: true, issues: [] });
    page();
    expect(await screen.findByRole('alert')).toHaveTextContent('설치 상태를 확인하지 못했습니다');
    fireEvent.click(screen.getByRole('button', { name: '다시 검사' }));
    await waitFor(() => expect(screen.getByText('설치 설정 검증 통과')).toBeInTheDocument());
  });
});
