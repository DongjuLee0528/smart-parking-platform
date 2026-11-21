import { apiClient } from '../../../shared/api/client';
import type { FloorMap, MapDrawing, ParkingSpace, ParkingSpaces } from '../types/FloorMap';

export async function getFloorMap(floorId: string): Promise<FloorMap> {
  const response = await apiClient.get<{ data: FloorMap }>(`/floors/${floorId}/map`);
  return response.data.data;
}

export async function saveFloorDrawing(
  floorId: string,
  expectedVersion: number,
  file: File,
): Promise<MapDrawing> {
  const form = new FormData();
  form.append('expectedVersion', String(expectedVersion));
  form.append('file', file);
  const response = await apiClient.put<{ data: MapDrawing }>(`/admin/floors/${floorId}/map`, form);
  return response.data.data;
}

export async function getFloorDrawing(floorId: string): Promise<Blob> {
  const response = await apiClient.get<Blob>(`/floors/${floorId}/map/drawing`, { responseType: 'blob' });
  return response.data;
}

export async function getParkingSpaces(floorId: string): Promise<ParkingSpaces> {
  const response = await apiClient.get<{ data: ParkingSpaces }>(`/admin/floors/${floorId}/parking-spaces`);
  return response.data.data;
}

export async function saveParkingSpaces(floorId: string, version: number, spaces: ParkingSpace[]): Promise<ParkingSpaces> {
  const response = await apiClient.put<{ data: ParkingSpaces }>(`/admin/floors/${floorId}/parking-spaces`, { version, spaces });
  return response.data.data;
}

export type FloorCamera = { id: string; floorId: string; name: string; status: string };

export async function getFloorCameras(floorId: string): Promise<FloorCamera[]> {
  const cameras: FloorCamera[] = [];
  for (let page = 0; ; page++) {
    const response = await apiClient.get<{ data: { items: FloorCamera[]; totalElements: number } }>(
      '/admin/cameras', { params: { page, size: 100, sort: 'name,asc' } },
    );
    cameras.push(...response.data.data.items.filter((camera) => camera.floorId === floorId));
    if ((page + 1) * 100 >= response.data.data.totalElements || response.data.data.items.length === 0) return cameras;
  }
}
