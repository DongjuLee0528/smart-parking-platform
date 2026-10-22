import { useEffect, useState, type FormEvent } from 'react';
import { getFloorMap, saveFloorDrawing } from '../api/floorMapApi';
import type { FloorMap } from '../types/FloorMap';

type Props = { floorId: string; token: string };

export function FloorMapEditor({ floorId, token }: Props) {
  const [map, setMap] = useState<FloorMap | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let current = true;
    setMap(null);
    setFile(null);
    getFloorMap(floorId, token)
      .then((result) => { if (current) setMap(result); })
      .catch(() => { if (current) setMessage('도면 정보를 불러오지 못했습니다.'); });
    return () => { current = false; };
  }, [floorId, token]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file || !map || busy) return;
    const form = event.currentTarget;
    setBusy(true);
    setMessage('');
    try {
      const drawing = await saveFloorDrawing(floorId, token, map.mapVersion, file);
      setMap({ ...map, mapVersion: drawing.version, drawing });
      setFile(null);
      form.reset();
      setMessage('도면이 저장되었습니다.');
    } catch {
      setMessage('도면 저장에 실패했습니다. 최신 버전을 확인한 뒤 다시 시도하세요.');
      try {
        setMap(await getFloorMap(floorId, token));
      } catch {
        setMap(null);
        setMessage('최신 도면 정보를 불러오지 못했습니다. 페이지를 다시 여세요.');
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-label="층별 도면 등록">
      <h2>{map?.floorName ?? '층별 도면'}</h2>
      {map && <p>현재 버전: {map.mapVersion} · {map.drawing ? '등록된 도면 있음' : '등록된 도면 없음'}</p>}
      <form onSubmit={submit}>
        <label htmlFor="floor-drawing">SVG, PNG 또는 JPEG 도면 (최대 5MB)</label>
        <input id="floor-drawing" type="file" accept=".svg,.png,.jpg,.jpeg,image/svg+xml,image/png,image/jpeg"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        <button type="submit" disabled={!map || !file || busy}>{busy ? '저장 중…' : '도면 등록·교체'}</button>
      </form>
      {message && <p role="status">{message}</p>}
    </section>
  );
}
