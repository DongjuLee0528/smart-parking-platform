import { Link, useParams } from 'react-router';
import { FloorMapEditor } from '../components/FloorMapEditor';

export function FloorMapEditorPage() {
  const { floorId } = useParams();
  return <main className="shell">
    <Link to="/">Dashboard</Link>
    {floorId ? <FloorMapEditor floorId={floorId} /> : <p>Floor ID is missing.</p>}
  </main>;
}
