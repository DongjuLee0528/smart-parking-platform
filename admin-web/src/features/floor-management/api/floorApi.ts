import { apiClient } from '../../../shared/api/client';

export type Zone = { id: string; floorId: string; name: string; active: boolean; version: number };
export type Floor = { id: string; name: string; floorOrder: number; active: boolean; version: number; zones: Zone[] };

export async function getFloors(lotId: string): Promise<Floor[]> {
  const response = await apiClient.get<{ data: Floor[] }>(`/admin/parking-lots/${lotId}/floors`);
  return response.data.data;
}

export async function createFloor(lotId: string, name: string, floorOrder: number): Promise<Floor> {
  const response = await apiClient.post<{ data: Floor }>(`/admin/parking-lots/${lotId}/floors`, { name, floorOrder });
  return response.data.data;
}

export async function updateFloor(floor: Floor, name: string, floorOrder: number, active: boolean): Promise<Floor> {
  const response = await apiClient.patch<{ data: Floor }>(`/admin/floors/${floor.id}`, {
    name, floorOrder, active, version: floor.version,
  });
  return response.data.data;
}

export async function createZone(floorId: string, name: string): Promise<Zone> {
  const response = await apiClient.post<{ data: Zone }>(`/admin/floors/${floorId}/zones`, { name });
  return response.data.data;
}

export async function updateZone(zone: Zone, name: string, active: boolean): Promise<Zone> {
  const response = await apiClient.patch<{ data: Zone }>(`/admin/zones/${zone.id}`, {
    name, active, version: zone.version,
  });
  return response.data.data;
}
