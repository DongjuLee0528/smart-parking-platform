import { apiClient } from '../../../shared/api/client';
import type { FloorMap, MapDrawing } from '../types/FloorMap';

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
