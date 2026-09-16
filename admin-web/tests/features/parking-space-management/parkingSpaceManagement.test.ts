import { describe, expect, it } from 'vitest';
import { ParkingSpaceForm } from '../../../src/features/parking-space-management/components/ParkingSpaceForm';

describe('parking space management skeleton', () => {
  it('exports the parking space form component', () => {
    expect(ParkingSpaceForm).toBeDefined();
  });
});
