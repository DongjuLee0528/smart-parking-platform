import { describe, expect, it } from 'vitest';
import { ParkingSpaceCandidateReview } from '../../../src/features/candidate-review/components/ParkingSpaceCandidateReview';

describe('candidate review skeleton', () => {
  it('exports the review component type', () => {
    expect(ParkingSpaceCandidateReview).toBeDefined();
  });
});
