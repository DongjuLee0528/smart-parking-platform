import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FloorMapEditor } from '../../../src/features/floor-map-editor/components/FloorMapEditor';
import { getFloorMap, saveFloorDrawing } from '../../../src/features/floor-map-editor/api/floorMapApi';

vi.mock('../../../src/features/floor-map-editor/api/floorMapApi', () => ({
  getFloorMap: vi.fn(),
  saveFloorDrawing: vi.fn(),
}));

describe('floor map editor', () => {
  beforeEach(() => {
    vi.mocked(getFloorMap).mockResolvedValue({
      floorId: 'floor-id', floorName: 'B1', mapVersion: 2, drawing: null,
    });
    vi.mocked(saveFloorDrawing).mockReset();
  });

  it('sends the selected drawing with the current version and updates the result', async () => {
    vi.mocked(saveFloorDrawing).mockResolvedValue({
      version: 3, url: '/api/v1/floors/floor-id/map/drawing', mediaType: 'image/png', width: 100, height: 80,
    });
    render(<FloorMapEditor floorId="floor-id" token="admin-token" />);
    expect(await screen.findByText(/현재 버전: 2/)).toBeInTheDocument();
    const file = new File(['image'], 'map.png', { type: 'image/png' });
    fireEvent.change(screen.getByLabelText(/SVG, PNG/), { target: { files: [file] } });
    fireEvent.click(screen.getByRole('button', { name: '도면 등록·교체' }));
    await waitFor(() => expect(saveFloorDrawing).toHaveBeenCalledWith('floor-id', 'admin-token', 2, file));
    expect(await screen.findByText(/현재 버전: 3/)).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('도면이 저장되었습니다.');
  });
});
