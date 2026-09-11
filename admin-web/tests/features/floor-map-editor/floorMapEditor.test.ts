import { describe, expect, it } from 'vitest';
import { FloorMapEditor } from '../../../src/features/floor-map-editor/components/FloorMapEditor';

describe('floor map editor skeleton', () => {
  it('exports the floor map editor component', () => {
    expect(FloorMapEditor).toBeDefined();
  });
});
