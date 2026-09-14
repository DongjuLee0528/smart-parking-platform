import { describe, expect, it } from 'vitest';
import { OccupancyStateTable } from '../../../src/features/occupancy-monitoring/components/OccupancyStateTable';

describe('occupancy monitoring skeleton', () => {
  it('exports the occupancy state table component', () => {
    expect(OccupancyStateTable).toBeDefined();
  });
});
