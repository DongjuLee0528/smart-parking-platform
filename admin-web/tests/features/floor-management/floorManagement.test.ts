import { describe, expect, it } from 'vitest';
import { ParkingFloorForm } from '../../../src/features/floor-management/components/ParkingFloorForm';

describe('floor management skeleton', () => {
  it('exports the parking floor form component', () => {
    expect(ParkingFloorForm).toBeDefined();
  });
});
