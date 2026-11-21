import { useEffect, useState, type PointerEvent } from 'react';
import type { FloorCamera } from '../api/floorMapApi';
import type { ParkingSpace, Point } from '../types/FloorMap';

type Props = {
  space: ParkingSpace;
  cameras: FloorCamera[];
  disabled: boolean;
  onChange: (mappings: ParkingSpace['cameraMappings']) => void;
};

function position(event: PointerEvent<SVGSVGElement>): Point | null {
  const rect = event.currentTarget.getBoundingClientRect();
  if (!rect.width || !rect.height) return null;
  return {
    x: Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width)),
    y: Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height)),
  };
}

function rectangle(start: Point, end: Point): Point[] | null {
  const left = Math.min(start.x, end.x);
  const right = Math.max(start.x, end.x);
  const top = Math.min(start.y, end.y);
  const bottom = Math.max(start.y, end.y);
  if (right - left < 0.01 || bottom - top < 0.01) return null;
  return [{ x: left, y: top }, { x: right, y: top }, { x: right, y: bottom }, { x: left, y: bottom }];
}

export function CameraMappingEditor({ space, cameras, disabled, onChange }: Props) {
  const [cameraId, setCameraId] = useState(space.cameraMappings[0]?.cameraId ?? '');
  const [frame, setFrame] = useState<File | null>(null);
  const [frameUrl, setFrameUrl] = useState<string | null>(null);
  const [frameSize, setFrameSize] = useState<{ width: number; height: number } | null>(null);
  const [start, setStart] = useState<Point | null>(null);
  const [draft, setDraft] = useState<Point[] | null>(null);
  const mapping = space.cameraMappings.find((item) => item.cameraId === cameraId);

  useEffect(() => {
    if (!frame) { setFrameUrl(null); return; }
    const url = URL.createObjectURL(frame);
    setFrameUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [frame]);

  function chooseCamera(id: string) {
    setCameraId(id);
    setFrame(null);
    setFrameSize(null);
    setStart(null);
    setDraft(null);
  }

  function savePolygon(points: Point[]) {
    if (!cameraId || disabled) return;
    if (mapping) {
      onChange(space.cameraMappings.map((item) => item.cameraId === cameraId
        ? { ...item, imagePolygon: points } : item));
      return;
    }
    if (space.cameraMappings.length >= 8) return;
    const used = new Set(space.cameraMappings.map((item) => item.priority));
    let priority = 1;
    while (used.has(priority)) priority++;
    onChange([...space.cameraMappings, { cameraId, priority, imagePolygon: points }]);
  }

  function begin(event: PointerEvent<SVGSVGElement>) {
    if (disabled || !frameSize || !cameraId) return;
    const point = position(event);
    if (!point) return;
    setStart(point);
    setDraft(null);
    event.currentTarget.setPointerCapture?.(event.pointerId);
  }

  function move(event: PointerEvent<SVGSVGElement>) {
    if (!start) return;
    const point = position(event);
    if (point) setDraft(rectangle(start, point));
  }

  function finish(event: PointerEvent<SVGSVGElement>) {
    if (!start) return;
    const point = position(event);
    const points = point && rectangle(start, point);
    setStart(null);
    setDraft(null);
    if (points) savePolygon(points);
  }

  const polygon = draft ?? mapping?.imagePolygon;
  const selectedCamera = cameras.find((camera) => camera.id === cameraId);
  return <section className="map-camera-section" aria-label="카메라 영역 연결">
    <h3>카메라 영역 연결</h3>
    <p className="map-help">이 주차면 ID에 도면 위치와 카메라 화면 영역을 함께 저장합니다.</p>
    <label htmlFor="map-camera">카메라</label>
    <select id="map-camera" value={cameraId} disabled={disabled || !cameras.length}
      onChange={(event) => chooseCamera(event.target.value)}>
      <option value="">카메라 선택</option>
      {cameras.map((camera) => <option key={camera.id} value={camera.id}
        disabled={space.cameraMappings.length >= 8 && !space.cameraMappings.some((item) => item.cameraId === camera.id)}>
        {camera.name}</option>)}
    </select>
    {!cameras.length && <p className="map-hint">이 층에 등록된 카메라가 없습니다.</p>}
    {selectedCamera && <>
      <p className="map-hint">{selectedCamera.name} · {selectedCamera.status} · {mapping ? '영역 연결됨' : '영역 미설정'}</p>
      <label htmlFor="map-camera-frame">카메라 정지 화면 (PNG 또는 JPEG)</label>
      <input id="map-camera-frame" type="file" accept="image/png,image/jpeg" disabled={disabled}
        onChange={(event) => {
          const next = event.target.files?.[0] ?? null;
          setFrame(next && ['image/png', 'image/jpeg'].includes(next.type) ? next : null);
          setFrameSize(null);
        }} />
      <p className="map-hint">선택한 화면은 브라우저에서만 표시됩니다. 같은 카메라의 실제 화면을 사용하세요.</p>
      {frameUrl && <div className="map-camera-canvas" style={frameSize ? { aspectRatio: `${frameSize.width} / ${frameSize.height}` } : undefined}>
        <img src={frameUrl} alt={selectedCamera.name + ' 정지 화면'} draggable={false}
          onLoad={(event) => setFrameSize({ width: event.currentTarget.naturalWidth,
            height: event.currentTarget.naturalHeight })} />
        {frameSize && <svg viewBox="0 0 1000 1000" preserveAspectRatio="none" aria-label="카메라 영역 지정"
          onPointerDown={begin} onPointerMove={move} onPointerUp={finish}
          onPointerCancel={() => { setStart(null); setDraft(null); }}>
          {polygon && <polygon className="map-camera-polygon"
            points={polygon.map((point) => `${point.x * 1000},${point.y * 1000}`).join(' ')} />}
        </svg>}
      </div>}
      {frameSize && <p className="map-hint">화면에서 주차면 영역의 한쪽 끝에서 반대쪽 끝까지 드래그하세요.</p>}
      {mapping && <button type="button" disabled={disabled} onClick={() =>
        onChange(space.cameraMappings.filter((item) => item.cameraId !== cameraId))}>이 카메라 연결 해제</button>}
    </>}
    {space.cameraMappings.length > 0 && <p className="map-hint">연결된 카메라 {space.cameraMappings.length}개 · 저장 버튼으로 확정</p>}
  </section>;
}
