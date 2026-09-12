import { describe, expect, it } from 'vitest';
import { InstallationJobStatus } from '../../../src/features/installation-job-monitoring/components/InstallationJobStatus';

describe('installation job monitoring skeleton', () => {
  it('exports the installation job status component', () => {
    expect(InstallationJobStatus).toBeDefined();
  });
});
