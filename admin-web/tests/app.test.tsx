import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from '../src/app/App';

describe('App', () => {
  it('requires Firebase configuration before sign-in', async () => {
    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Smart Parking Admin' })).toBeInTheDocument();
    expect(screen.getByText('Firebase web configuration is missing.')).toBeInTheDocument();
  });
});
