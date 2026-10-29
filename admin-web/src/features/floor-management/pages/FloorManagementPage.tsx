import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import axios from 'axios';
import { createFloor, createZone, getFloors, updateFloor, updateZone, type Floor, type Zone } from '../api/floorApi';

function message(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 409) return 'This item changed or its name is already in use. Reload and try again.';
    return error.response?.data?.message ?? 'Request failed. Please try again.';
  }
  return 'Request failed. Please try again.';
}

function ZoneRow({ zone, refresh, report }: { zone: Zone; refresh: () => Promise<void>; report: (value: string) => void }) {
  const [name, setName] = useState(zone.name);
  const [busy, setBusy] = useState(false);
  async function save(active: boolean) {
    setBusy(true);
    try {
      await updateZone(zone, name.trim(), active);
      await refresh();
      report('');
    } catch (error) { report(message(error)); }
    finally { setBusy(false); }
  }
  return <li>
    <label>Zone name <input value={name} maxLength={100} onChange={(event) => setName(event.target.value)} required /></label>
    <span>{zone.active ? 'Active' : 'Inactive'}</span>
    <button type="button" disabled={busy || !name.trim()} onClick={() => void save(zone.active)}>Save zone</button>
    <button type="button" disabled={busy} onClick={() => void save(!zone.active)}>
      {zone.active ? 'Deactivate zone' : 'Reactivate zone'}
    </button>
  </li>;
}

function FloorRow({ floor, refresh, report }: { floor: Floor; refresh: () => Promise<void>; report: (value: string) => void }) {
  const [name, setName] = useState(floor.name);
  const [order, setOrder] = useState(floor.floorOrder);
  const [zoneName, setZoneName] = useState('');
  const [busy, setBusy] = useState(false);
  async function save(active: boolean) {
    setBusy(true);
    try {
      await updateFloor(floor, name.trim(), order, active);
      await refresh();
      report('');
    } catch (error) { report(message(error)); }
    finally { setBusy(false); }
  }
  async function addZone(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    try {
      await createZone(floor.id, zoneName.trim());
      setZoneName('');
      await refresh();
      report('');
    } catch (error) { report(message(error)); }
    finally { setBusy(false); }
  }
  return <li>
    <h2>{floor.name}</h2>
    <label>Floor name <input value={name} maxLength={100} onChange={(event) => setName(event.target.value)} required /></label>
    <label>Order <input type="number" value={order} onChange={(event) => setOrder(Number(event.target.value))} required /></label>
    <span>{floor.active ? 'Active' : 'Inactive'}</span>
    <button type="button" disabled={busy || !name.trim()} onClick={() => void save(floor.active)}>Save floor</button>
    <button type="button" disabled={busy} onClick={() => void save(!floor.active)}>
      {floor.active ? 'Deactivate floor' : 'Reactivate floor'}
    </button>
    <Link to={`/floors/${floor.id}/map`}>Open drawing</Link>
    <form onSubmit={(event) => void addZone(event)}>
      <label>New zone <input value={zoneName} maxLength={100} onChange={(event) => setZoneName(event.target.value)} required /></label>
      <button disabled={busy || !zoneName.trim()}>Add zone</button>
    </form>
    <ul>{floor.zones.map((zone) => <ZoneRow key={zone.id} zone={zone} refresh={refresh} report={report} />)}</ul>
  </li>;
}

export function FloorManagementPage() {
  const { lotId } = useParams();
  const [floors, setFloors] = useState<Floor[]>([]);
  const [name, setName] = useState('');
  const [order, setOrder] = useState(0);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const refresh = useCallback(async () => {
    if (!lotId) return;
    setFloors(await getFloors(lotId));
  }, [lotId]);
  useEffect(() => { void refresh().catch((cause: unknown) => setError(message(cause))); }, [refresh]);
  async function addFloor(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!lotId) return;
    setBusy(true);
    try {
      await createFloor(lotId, name.trim(), order);
      setName('');
      await refresh();
      setError('');
    } catch (cause) { setError(message(cause)); }
    finally { setBusy(false); }
  }
  return <main className="shell">
    <Link to="/">Dashboard</Link>
    <h1>Floors and zones</h1>
    {error && <p role="alert">{error}</p>}
    <form onSubmit={(event) => void addFloor(event)}>
      <label>New floor <input value={name} maxLength={100} onChange={(event) => setName(event.target.value)} required /></label>
      <label>Order <input type="number" value={order} onChange={(event) => setOrder(Number(event.target.value))} required /></label>
      <button disabled={busy || !name.trim()}>Add floor</button>
    </form>
    <ul>{floors.map((floor) => <FloorRow key={floor.id} floor={floor} refresh={refresh} report={setError} />)}</ul>
  </main>;
}
