import { describe, expect, it } from 'vitest';
import { useFloorEvents } from '../../../src/features/realtime/hooks/useFloorEvents';

describe('realtime skeleton', () => {
  it('exports the floor event hook type', () => {
    expect(useFloorEvents).toBeDefined();
  });
});
