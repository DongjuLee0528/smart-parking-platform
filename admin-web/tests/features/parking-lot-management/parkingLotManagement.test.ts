import { describe, expect, it } from 'vitest';
import { ParkingLotForm } from '../../../src/features/parking-lot-management/components/ParkingLotForm';

describe('parking lot management skeleton', () => {
  it('exports the parking lot form component', () => {
    expect(ParkingLotForm).toBeDefined();
  });
});
