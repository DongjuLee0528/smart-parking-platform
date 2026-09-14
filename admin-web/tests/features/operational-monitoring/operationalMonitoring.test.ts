import { describe, expect, it } from 'vitest';
import { OperationalStatusPanel } from '../../../src/features/operational-monitoring/components/OperationalStatusPanel';

describe('operational monitoring skeleton', () => {
  it('exports the operational status panel component', () => {
    expect(OperationalStatusPanel).toBeDefined();
  });
});
