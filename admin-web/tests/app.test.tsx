import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from '../src/app/App';

describe('App', () => {
  it('renders the admin shell', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Smart Parking Admin' })).toBeInTheDocument();
  });
});
