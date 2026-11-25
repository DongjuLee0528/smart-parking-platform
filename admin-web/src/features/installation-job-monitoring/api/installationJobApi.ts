import { apiClient } from '../../../shared/api/client';

export type InstallationIssue = { code: string; floorId: string | null; spaceId: string | null };
export type InstallationReadiness = { parkingLotId: string; ready: boolean; issues: InstallationIssue[] };

export async function getInstallationReadiness(lotId: string): Promise<InstallationReadiness> {
  const response = await apiClient.get<{ data: InstallationReadiness }>(
    `/admin/parking-lots/${encodeURIComponent(lotId)}/installation-check`,
  );
  return response.data.data;
}
