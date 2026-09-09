import { describe, expect, it } from 'vitest';
import { CameraConnectionStatus } from '../../../src/features/camera-management/components/CameraConnectionStatus';

describe('camera management skeleton', () => {
  it('exports the camera connection status component', () => {
    expect(CameraConnectionStatus).toBeDefined();
  });
});
