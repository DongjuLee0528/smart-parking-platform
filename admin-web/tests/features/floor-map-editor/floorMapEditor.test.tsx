import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FloorMapEditor } from '../../../src/features/floor-map-editor/components/FloorMapEditor';
import { FloorMapEditorPage } from '../../../src/features/floor-map-editor/pages/FloorMapEditorPage';
import { getFloorDrawing, getFloorMap, getParkingSpaces, saveFloorDrawing, saveParkingSpaces }
  from '../../../src/features/floor-map-editor/api/floorMapApi';
import type { FloorMap, ParkingSpace } from '../../../src/features/floor-map-editor/types/FloorMap';

vi.mock('../../../src/features/floor-map-editor/api/floorMapApi', () => ({
  getFloorMap: vi.fn(),
  saveFloorDrawing: vi.fn(),
  getFloorDrawing: vi.fn(),
  getParkingSpaces: vi.fn(),
  saveParkingSpaces: vi.fn(),
}));

const map: FloorMap = {
  floorId: 'floor-id', floorName: 'B2', mapVersion: 2,
  drawing: { version: 2, url: '/api/v1/floors/floor-id/map/drawing', mediaType: 'image/png',
    width: 1000, height: 500 },
  zones: [{ id: 'zone-1', name: 'B' }],
};
const existing: ParkingSpace = {
  id: 'space-1', zoneId: 'zone-1', spaceNumber: 'B-12', type: 'GENERAL', active: true,
  mapPolygon: [{ x: 0.2, y: 0.2 }, { x: 0.28, y: 0.2 }, { x: 0.28, y: 0.32 }, { x: 0.2, y: 0.32 }],
  cameraMappings: [{ cameraId: 'camera-1', priority: 1, imagePolygon: [
    { x: 0.1, y: 0.1 }, { x: 0.2, y: 0.1 }, { x: 0.2, y: 0.2 },
  ] }],
};

describe('floor map editor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:floor') });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() });
    vi.mocked(getFloorMap).mockResolvedValue(map);
    vi.mocked(getFloorDrawing).mockResolvedValue(new Blob(['image'], { type: 'image/png' }));
    vi.mocked(getParkingSpaces).mockResolvedValue({ floorId: 'floor-id', version: 2, spaces: [existing] });
  });

  it('uploads a drawing with the current version', async () => {
    vi.mocked(getFloorMap).mockResolvedValueOnce({ ...map, drawing: null }).mockResolvedValue({
      ...map, mapVersion: 3, drawing: { ...map.drawing!, version: 3 },
    });
    vi.mocked(getParkingSpaces).mockResolvedValueOnce({ floorId: 'floor-id', version: 2, spaces: [] })
      .mockResolvedValue({ floorId: 'floor-id', version: 3, spaces: [] });
    vi.mocked(saveFloorDrawing).mockResolvedValue({ ...map.drawing!, version: 3 });
    render(<FloorMapEditor floorId="floor-id" />);
    expect(await screen.findByText('등록된 도면 없음')).toBeInTheDocument();
    const file = new File(['image'], 'map.png', { type: 'image/png' });
    fireEvent.change(screen.getByLabelText('도면 교체'), { target: { files: [file] } });
    fireEvent.click(screen.getByRole('button', { name: '도면 등록·교체' }));
    await waitFor(() => expect(saveFloorDrawing).toHaveBeenCalledWith('floor-id', 2, file));
    expect(await screen.findByText('도면 버전 3')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('도면이 저장되었습니다.');
  });

  it('places a space and preserves existing camera mappings when saving', async () => {
    vi.mocked(saveParkingSpaces).mockResolvedValue({ floorId: 'floor-id', version: 3, spaces: [existing] });
    render(<FloorMapEditor floorId="floor-id" />);
    const canvas = await screen.findByLabelText('주차면 도면');
    Object.defineProperty(canvas, 'getBoundingClientRect', { value: () => ({
      left: 0, top: 0, width: 1000, height: 500,
    }) });
    fireEvent.click(screen.getByRole('button', { name: '주차면 추가' }));
    fireEvent.pointerDown(canvas.querySelector('rect')!, { clientX: 600, clientY: 250, pointerId: 1 });
    fireEvent.change(screen.getByLabelText('주차면 번호'), { target: { value: 'B-13' } });
    fireEvent.click(screen.getByRole('button', { name: '변경 사항 저장' }));
    await waitFor(() => expect(saveParkingSpaces).toHaveBeenCalledWith('floor-id', 2, [
      existing,
      expect.objectContaining({ id: null, zoneId: 'zone-1', spaceNumber: 'B-13',
        cameraMappings: [], mapPolygon: expect.arrayContaining([expect.objectContaining({ x: 0.56 })]) }),
    ]));
  });

  it('moves and rotates a selected space without removing its camera mapping', async () => {
    vi.mocked(saveParkingSpaces).mockResolvedValue({ floorId: 'floor-id', version: 3, spaces: [existing] });
    render(<FloorMapEditor floorId="floor-id" />);
    fireEvent.pointerDown(await screen.findByRole('button', { name: '주차면 B-12' }),
      { clientX: 200, clientY: 100, pointerId: 1 });
    fireEvent.change(screen.getByLabelText('X 좌표'), { target: { value: '300' } });
    fireEvent.click(screen.getByRole('button', { name: '오른쪽으로 90° 회전' }));
    fireEvent.click(screen.getByRole('button', { name: '변경 사항 저장' }));
    await waitFor(() => expect(saveParkingSpaces).toHaveBeenCalled());
    const saved = vi.mocked(saveParkingSpaces).mock.calls[0][2][0];
    expect(saved.cameraMappings).toEqual(existing.cameraMappings);
    expect(saved.mapPolygon).not.toEqual(existing.mapPolygon);
    expect(saved.mapPolygon.every((point) => point.x >= 0 && point.x <= 1 && point.y >= 0 && point.y <= 1))
      .toBe(true);
    const width = Math.max(...saved.mapPolygon.map((point) => point.x))
      - Math.min(...saved.mapPolygon.map((point) => point.x));
    expect(width).toBeCloseTo(0.06);
  });

  it('keeps unsaved edits when the server rejects a stale version', async () => {
    vi.mocked(saveParkingSpaces).mockRejectedValue({
      isAxiosError: true, response: { status: 409, data: { code: 'VERSION_CONFLICT' } },
    });
    render(<FloorMapEditor floorId="floor-id" />);
    fireEvent.pointerDown(await screen.findByRole('button', { name: '주차면 B-12' }),
      { clientX: 200, clientY: 100, pointerId: 1 });
    fireEvent.change(screen.getByLabelText('주차면 번호'), { target: { value: 'B-14' } });
    fireEvent.click(screen.getByRole('button', { name: '변경 사항 저장' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('버전이 달라졌습니다');
    expect(screen.getByLabelText('주차면 번호')).toHaveValue('B-14');
    expect(getFloorMap).toHaveBeenCalledTimes(1);
  });

  it('does not describe an integrity conflict as a stale version', async () => {
    vi.mocked(saveParkingSpaces).mockRejectedValue({
      isAxiosError: true, response: { status: 409, data: { code: 'PARKING_SPACE_CONFLICT' } },
    });
    render(<FloorMapEditor floorId="floor-id" />);
    fireEvent.pointerDown(await screen.findByRole('button', { name: '주차면 B-12' }),
      { clientX: 200, clientY: 100, pointerId: 1 });
    fireEvent.change(screen.getByLabelText('주차면 번호'), { target: { value: 'B-14' } });
    fireEvent.click(screen.getByRole('button', { name: '변경 사항 저장' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('입력값과 연결 상태');
  });

  it('warns before leaving the editor with unsaved changes', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const router = createMemoryRouter([
      { path: '/', element: <div>Dashboard</div> },
      { path: '/floors/:floorId/map', element: <FloorMapEditorPage /> },
    ], { initialEntries: ['/floors/floor-id/map'] });
    render(<RouterProvider router={router} />);
    fireEvent.pointerDown(await screen.findByRole('button', { name: '주차면 B-12' }),
      { clientX: 200, clientY: 100, pointerId: 1 });
    fireEvent.change(screen.getByLabelText('주차면 번호'), { target: { value: 'B-14' } });
    fireEvent.click(screen.getByRole('link', { name: '대시보드' }));
    await waitFor(() => expect(confirm).toHaveBeenCalled());
    expect(screen.getByLabelText('주차면 번호')).toHaveValue('B-14');
    confirm.mockRestore();
  });
});
