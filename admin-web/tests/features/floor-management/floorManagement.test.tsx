import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FloorManagementPage } from '../../../src/features/floor-management/pages/FloorManagementPage';
import { createFloor, createZone, getFloors, updateFloor, updateZone, type Floor } from '../../../src/features/floor-management/api/floorApi';

vi.mock('../../../src/features/floor-management/api/floorApi', () => ({
  createFloor: vi.fn(), createZone: vi.fn(), getFloors: vi.fn(), updateFloor: vi.fn(), updateZone: vi.fn(),
}));

function show() {
  render(<MemoryRouter initialEntries={['/parking-lots/lot-1/floors']}><Routes>
    <Route path="/parking-lots/:lotId/floors" element={<FloorManagementPage />} />
  </Routes></MemoryRouter>);
}

const floor: Floor = { id: 'floor-1', name: 'B1', floorOrder: -1, active: true, version: 2,
  zones: [{ id: 'zone-1', floorId: 'floor-1', name: 'A', active: true, version: 3 }] };

describe('floor and zone management', () => {
  beforeEach(() => { vi.clearAllMocks(); vi.mocked(getFloors).mockResolvedValue([floor]); });

  it('creates a floor and zone under the selected parent', async () => {
    show();
    expect(await screen.findByText('B1')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('New floor'), { target: { value: 'B2' } });
    fireEvent.change(screen.getAllByLabelText('Order')[0], { target: { value: '-2' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add floor' }));
    await waitFor(() => expect(createFloor).toHaveBeenCalledWith('lot-1', 'B2', -2));
    fireEvent.change(screen.getByLabelText('New zone'), { target: { value: 'C' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add zone' }));
    await waitFor(() => expect(createZone).toHaveBeenCalledWith('floor-1', 'C'));
  });

  it('sends current versions for deactivation and shows conflicts', async () => {
    vi.mocked(updateFloor).mockRejectedValue({ isAxiosError: true, response: { status: 409 } });
    show();
    await screen.findByText('B1');
    fireEvent.click(screen.getByRole('button', { name: 'Deactivate floor' }));
    await waitFor(() => expect(updateFloor).toHaveBeenCalledWith(floor, 'B1', -1, false));
    expect(await screen.findByRole('alert')).toHaveTextContent('Reload and try again');
    fireEvent.click(screen.getByRole('button', { name: 'Deactivate zone' }));
    await waitFor(() => expect(updateZone).toHaveBeenCalledWith(floor.zones[0], 'A', false));
  });
});
