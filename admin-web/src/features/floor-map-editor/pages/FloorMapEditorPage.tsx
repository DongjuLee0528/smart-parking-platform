import { useEffect, useState } from 'react';
import { Link, useBlocker, useParams } from 'react-router';
import { FloorMapEditor } from '../components/FloorMapEditor';

export function FloorMapEditorPage() {
  const { floorId } = useParams();
  const [dirty, setDirty] = useState(false);
  const blocker = useBlocker(dirty);
  useEffect(() => {
    if (blocker.state !== 'blocked') return;
    if (window.confirm('저장하지 않은 주차면 변경 사항을 버리고 이동할까요?')) blocker.proceed();
    else blocker.reset();
  }, [blocker]);
  return <main className="map-page">
    <aside className="map-sidebar">
      <Link className="map-brand" to="/"><span>P</span><strong>Smart Parking<small>운영자</small></strong></Link>
      <nav aria-label="운영 메뉴">
        <Link to="/">대시보드</Link>
        <span className="active">도면 편집</span>
      </nav>
    </aside>
    <div className="map-page-main">
      <header className="map-topbar">Smart Parking / 운영 콘솔</header>
      {floorId ? <FloorMapEditor floorId={floorId} onDirtyChange={setDirty} />
        : <p role="alert">Floor ID is missing.</p>}
    </div>
  </main>;
}
