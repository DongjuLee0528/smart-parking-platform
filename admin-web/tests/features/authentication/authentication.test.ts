import { describe, expect, it } from 'vitest';
import { AdminLoginPage } from '../../../src/features/authentication/pages/AdminLoginPage';

describe('authentication skeleton', () => {
  it('exports the admin login page type', () => {
    expect(AdminLoginPage).toBeDefined();
  });
});
