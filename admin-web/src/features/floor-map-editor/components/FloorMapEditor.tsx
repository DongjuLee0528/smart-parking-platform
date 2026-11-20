import axios from 'axios';
import { useEffect, useRef, useState, type PointerEvent } from 'react';
import { getFloorDrawing, getFloorMap, getParkingSpaces, saveFloorDrawing, saveParkingSpaces } from '../api/floorMapApi';
import { bounds, movePolygon, newPolygon, rotatePolygon } from '../geometry';
import type { FloorMap, ParkingSpace, Point } from '../types/FloorMap';
import '../floor-map-editor.css';

type Props = { floorId: string; onDirtyChange?: (dirty: boolean) => void };
type Drag = { pointerId: number; index: number; x: number; y: number; points: Point[] };

export function FloorMapEditor({ floorId, onDirtyChange }: Props) {
  const [map, setMap] = useState<FloorMap | null>(null);
  const [spaces, setSpaces] = useState<ParkingSpace[]>([]);
  const [version, setVersion] = useState<number | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [placing, setPlacing] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [message, setMessage] = useState('');
  const [error, setError] = useState(false);
  const drag = useRef<Drag | null>(null);

  useEffect(() => {
    let active = true;
    let url: string | null = null;
    async function load() {
      setLoading(true);
      setMap(null);
      setImageUrl(null);
      setSelected(null);
      setPlacing(false);
      setDirty(false);
      setFile(null);
      try {
        const snapshot = await getFloorMap(floorId);
        const [details, image] = await Promise.all([
          getParkingSpaces(floorId),
          snapshot.drawing ? getFloorDrawing(floorId) : Promise.resolve(null),
        ]);
        if (snapshot.mapVersion !== details.version) throw new Error('도면 버전이 변경되었습니다. 새로고침하세요.');
        if (!active) return;
        if (image) url = URL.createObjectURL(image);
        setMap(snapshot);
        setSpaces(details.spaces);
        setVersion(details.version);
        setImageUrl(url);
      } catch (cause) {
        if (active) {
          setMessage(cause instanceof Error && cause.message.startsWith('도면 버전')
            ? cause.message : '도면 정보를 불러오지 못했습니다. 새로고침하세요.');
          setError(true);
        }
      } finally { if (active) setLoading(false); }
    }
    void load();
    return () => { active = false; if (url) URL.revokeObjectURL(url); };
  }, [floorId, refresh]);

  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty]);

  useEffect(() => { onDirtyChange?.(dirty); }, [dirty, onDirtyChange]);

  const drawing = map?.drawing;
  const current = selected === null ? null : spaces[selected] ?? null;
  const invalid = spaces.some((space) => !space.spaceNumber.trim());

  function report(text: string, failed = false) { setMessage(text); setError(failed); }
  function update(index: number, change: Partial<ParkingSpace>) {
    if (busy || loading) return;
    setSpaces((old) => old.map((space, position) => position === index ? { ...space, ...change } : space));
    setDirty(true);
    report('');
  }
  function addSpace(x: number, y: number) {
    if (!map?.zones.length || !drawing || busy || loading || spaces.length >= 500) return;
    setSpaces((old) => [...old, { id: null, zoneId: map.zones[0].id, spaceNumber: '',
      type: 'GENERAL', active: true, mapPolygon: newPolygon(x, y), cameraMappings: [] }]);
    setSelected(spaces.length);
    setPlacing(false);
    setDirty(true);
    report('주차면 번호를 입력한 뒤 변경 사항을 저장하세요.');
  }
  function place(event: PointerEvent<SVGRectElement>) {
    if (!placing) return;
    const rect = event.currentTarget.ownerSVGElement!.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    addSpace((event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height);
  }
  function startDrag(event: PointerEvent<SVGPolygonElement>, index: number) {
    if (busy || loading) return;
    event.stopPropagation();
    setSelected(index);
    setPlacing(false);
    drag.current = { pointerId: event.pointerId, index, x: event.clientX, y: event.clientY,
      points: spaces[index].mapPolygon };
    event.currentTarget.setPointerCapture?.(event.pointerId);
  }
  function moveDrag(event: PointerEvent<SVGSVGElement>) {
    const active = drag.current;
    if (!active || active.pointerId !== event.pointerId) return;
    const rect = event.currentTarget.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    const points = movePolygon(active.points, (event.clientX - active.x) / rect.width,
      (event.clientY - active.y) / rect.height);
    if (points.some((point, index) => point.x !== active.points[index].x || point.y !== active.points[index].y)) {
      update(active.index, { mapPolygon: points });
    }
  }
  function moveSelected(dx: number, dy: number) {
    if (selected !== null && current) update(selected, { mapPolygon: movePolygon(current.mapPolygon, dx, dy) });
  }
  function setCoordinate(axis: 'x' | 'y', value: number) {
    if (selected === null || !current || !drawing || !Number.isFinite(value)) return;
    const box = bounds(current.mapPolygon);
    const delta = axis === 'x' ? value / drawing.width - box.left : value / drawing.height - box.top;
    moveSelected(axis === 'x' ? delta : 0, axis === 'y' ? delta : 0);
  }
  function rotateSelected() {
    if (selected === null || !current || !drawing) return;
    const points = rotatePolygon(current.mapPolygon, drawing.width, drawing.height);
    if (!points) { report('도면 밖으로 벗어납니다. 주차면을 안쪽으로 옮긴 뒤 회전하세요.', true); return; }
    update(selected, { mapPolygon: points });
  }
  async function upload() {
    if (!file || !map || busy || dirty) return;
    setBusy(true);
    report('');
    try {
      await saveFloorDrawing(floorId, map.mapVersion, file);
      setRefresh((value) => value + 1);
      report('도면이 저장되었습니다.');
    } catch (cause) {
      report(axios.isAxiosError(cause) && cause.response?.status === 409
        ? '도면 버전이 변경되었습니다. 새로고침 후 다시 시도하세요.'
        : '도면 저장에 실패했습니다. 파일과 연결 상태를 확인하세요.', true);
    } finally { setBusy(false); }
  }
  async function save() {
    if (!dirty || version === null || busy || invalid) return;
    setBusy(true);
    report('');
    try {
      await saveParkingSpaces(floorId, version, spaces.map((space) => ({
        ...space, spaceNumber: space.spaceNumber.trim(),
      })));
      setRefresh((value) => value + 1);
      report('주차면 변경 사항을 저장했습니다.');
    } catch (cause) {
      report(axios.isAxiosError(cause) && cause.response?.data?.code === 'VERSION_CONFLICT'
        ? '다른 변경으로 도면 버전이 달라졌습니다. 새로고침 후 다시 편집하세요.'
        : '주차면 저장에 실패했습니다. 입력값과 연결 상태를 확인하세요.', true);
    } finally { setBusy(false); }
  }

  return <section className="map-editor" aria-label="도면 편집">
    <div className="map-editor-heading">
      <div><h1>도면 편집</h1><p>도면을 등록하고 주차면 위치와 방향을 수정하세요.</p></div>
      <button className="map-primary" type="button" disabled={!dirty || busy || invalid}
        onClick={() => void save()}>{busy ? '처리 중…' : '변경 사항 저장'}</button>
    </div>
    {message && <p className={error ? 'map-message map-error' : 'map-message'}
      role={error ? 'alert' : 'status'}>{message}</p>}
    <div className="map-toolbar">
      <strong>{map?.floorName ?? '층 도면'}</strong>
      <span>{drawing ? '도면 버전 ' + drawing.version : '등록된 도면 없음'}</span>
      <label htmlFor="floor-drawing">도면 교체</label>
      <input id="floor-drawing" type="file" accept=".svg,.png,.jpg,.jpeg,image/svg+xml,image/png,image/jpeg"
        onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
      <button type="button" disabled={!file || !map || dirty || busy} onClick={() => void upload()}>도면 등록·교체</button>
      <button type="button" disabled={busy} onClick={() => {
        if (dirty && !window.confirm('저장하지 않은 주차면 변경 사항을 버리고 새로고침할까요?')) return;
        report('');
        setRefresh((value) => value + 1);
      }}>{dirty ? '변경 취소 후 새로고침' : '새로고침'}</button>
    </div>
    {dirty && <p className="map-hint">주차면 변경 사항을 저장한 뒤 도면을 교체할 수 있습니다.</p>}
    {loading ? <p role="status">도면을 불러오는 중…</p> : map && <div className="map-editor-body">
      <div className="map-canvas-panel">
        <div className="map-canvas-heading"><strong>{map.floorName} 도면</strong>
          <div className="map-canvas-actions">
            {placing && <button type="button" onClick={() => addSpace(0.5, 0.5)}>가운데 배치</button>}
            <button type="button" disabled={!drawing || !map.zones.length || busy || spaces.length >= 500}
              aria-pressed={placing} onClick={() => { setPlacing((value) => !value); setSelected(null); }}>
              {placing ? '배치 취소' : '주차면 추가'}</button>
          </div></div>
        {drawing && imageUrl ? <div className="map-canvas" style={{ aspectRatio: drawing.width + ' / ' + drawing.height }}>
          <img src={imageUrl} alt={map.floorName + ' 도면'} draggable={false} />
          <svg viewBox="0 0 1000 1000" preserveAspectRatio="none" aria-label="주차면 도면"
            onPointerMove={moveDrag} onPointerUp={() => { drag.current = null; }}
            onPointerCancel={() => { drag.current = null; }}>
            <rect width="1000" height="1000" fill="transparent" onPointerDown={place} />
            {spaces.map((space, index) => {
              if (!space.active) return null;
              const box = bounds(space.mapPolygon);
              return <g key={space.id ?? 'new-' + index}>
                <polygon points={space.mapPolygon.map((point) => (point.x * 1000) + ',' + (point.y * 1000)).join(' ')}
                  className={selected === index ? 'map-space selected' : 'map-space'}
                  role="button" tabIndex={0} aria-label={'주차면 ' + (space.spaceNumber || '새 주차면')}
                  onPointerDown={(event) => startDrag(event, index)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); setSelected(index); }
                    if (selected === index && event.key.startsWith('Arrow')) {
                      event.preventDefault();
                      moveSelected(event.key === 'ArrowLeft' ? -0.01 : event.key === 'ArrowRight' ? 0.01 : 0,
                        event.key === 'ArrowUp' ? -0.01 : event.key === 'ArrowDown' ? 0.01 : 0);
                    }
                  }} />
                <text x={(box.left + box.right) * 500} y={(box.top + box.bottom) * 500}
                  textAnchor="middle" dominantBaseline="middle" pointerEvents="none">{space.spaceNumber || '+'}</text>
              </g>;
            })}
          </svg>
        </div> : <div className="map-empty">도면을 등록하면 주차면을 배치할 수 있습니다.</div>}
        <p className="map-hint">{placing ? '도면에서 주차면을 놓을 위치를 누르세요.'
          : '주차면을 선택하고 드래그하거나 방향키와 오른쪽 좌표로 이동하세요.'}</p>
      </div>
      <aside className="map-properties" aria-label="주차면 속성">
        <h2>주차면 속성</h2>
        {current && selected !== null && drawing ? <>
          <p className="map-property-caption">선택한 주차면</p>
          <strong className="map-selected-name">{current.spaceNumber || '새 주차면'}</strong>
          <label htmlFor="map-space-number">주차면 번호</label>
          <input id="map-space-number" value={current.spaceNumber} maxLength={40} disabled={busy}
            onChange={(event) => update(selected, { spaceNumber: event.target.value })} />
          <label htmlFor="map-zone">구역</label>
          <select id="map-zone" value={current.zoneId} disabled={busy}
            onChange={(event) => update(selected, { zoneId: event.target.value })}>
            {!map.zones.some((zone) => zone.id === current.zoneId) && <option value={current.zoneId}>현재 구역</option>}
            {map.zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
          </select>
          <span className="map-field-label">주차면 유형</span>
          <div className="map-type-options">
            {([['GENERAL', '일반'], ['DISABLED', '장애인'], ['ELECTRIC', '전기차'], ['COMPACT', '경차']] as const)
              .map(([type, label]) => <button key={type} type="button" disabled={busy} aria-pressed={current.type === type}
                onClick={() => update(selected, { type })}>{label}</button>)}
          </div>
          <span className="map-field-label">도면 좌표 (px)</span>
          <div className="map-coordinate-fields">
            <label>X <input aria-label="X 좌표" type="number" min="0" max={drawing.width} disabled={busy}
              value={Math.round(bounds(current.mapPolygon).left * drawing.width)}
              onChange={(event) => setCoordinate('x', Number(event.target.value))} /></label>
            <label>Y <input aria-label="Y 좌표" type="number" min="0" max={drawing.height} disabled={busy}
              value={Math.round(bounds(current.mapPolygon).top * drawing.height)}
              onChange={(event) => setCoordinate('y', Number(event.target.value))} /></label>
          </div>
          <button type="button" className="map-rotate" disabled={busy} onClick={rotateSelected}>오른쪽으로 90° 회전</button>
          <p className="map-help">드래그하거나 좌표를 직접 수정하세요. 방향은 주차면 모양을 회전해 저장합니다.</p>
        </> : <p className="map-help">도면의 주차면을 선택하거나 새 주차면을 추가하세요.</p>}
      </aside>
    </div>}
  </section>;
}
