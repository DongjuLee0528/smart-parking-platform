import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { getInstallationReadiness, type InstallationReadiness } from '../api/installationJobApi';
import './installation-readiness.css';

const labels: Record<string, string> = {
  FLOOR_MISSING: '사용할 층을 하나 이상 등록하세요.',
  DRAWING_MISSING: '층 도면을 등록하세요.',
  ZONE_MISSING: '사용할 구역을 하나 이상 등록하세요.',
  SPACE_MISSING: '사용할 주차면을 하나 이상 배치하세요.',
  SPACE_ZONE_INACTIVE: '주차면이 비활성 구역에 있습니다.',
  SPACE_NUMBER_MISSING: '주차면 번호를 입력하세요.',
  SPACE_TYPE_MISSING: '주차면 유형을 선택하세요.',
  MAP_POLYGON_MISSING: '앱 도면의 주차면 영역을 지정하세요.',
  CAMERA_MAPPING_MISSING: '주차면에 카메라 영상 영역을 연결하세요.',
  CAMERA_INVALID: '연결된 카메라의 층 또는 구역을 확인하세요.',
  CAMERA_NOT_REPORTING: '카메라 연결과 정상 프레임 수신을 확인하세요.',
  IMAGE_POLYGON_MISSING: '카메라 영상의 주차면 영역을 지정하세요.',
};

export function InstallationReadinessPage() {
  const { lotId } = useParams();
  const [result, setResult] = useState<InstallationReadiness | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const refresh = useCallback(async () => {
    if (!lotId) return;
    setLoading(true);
    setError(false);
    setResult(null);
    try { setResult(await getInstallationReadiness(lotId)); }
    catch { setError(true); setResult(null); }
    finally { setLoading(false); }
  }, [lotId]);

  useEffect(() => { void refresh(); }, [refresh]);

  return <main className="shell readiness-page">
    <Link to={lotId ? `/parking-lots/${encodeURIComponent(lotId)}/floors` : '/'}>층·구역 관리로 돌아가기</Link>
    <h1>설치 완료 검증</h1>
    <p>현재 등록된 도면, 주차면, 카메라 연결을 확인합니다. 이 검사는 주차장을 공개하지 않습니다.</p>
    <button type="button" disabled={loading || !lotId} onClick={() => void refresh()}>다시 검사</button>
    {!lotId && <p role="alert">주차장 ID가 없습니다.</p>}
    {loading && <p role="status">설치 상태를 확인하는 중…</p>}
    {error && <p role="alert">설치 상태를 확인하지 못했습니다. 다시 검사해 주세요.</p>}
    {result && <section aria-label="설치 검증 결과">
      <h2>{result.ready ? '설치 설정 검증 통과' : `확인할 항목 ${result.issues.length}개`}</h2>
      {result.ready ? <p>현재 설정이 검증을 통과했습니다. 운영 활성화는 별도 단계입니다.</p>
        : <ul>{result.issues.map((issue, index) => <li key={`${issue.code}-${issue.floorId}-${issue.spaceId}-${index}`}>
          <span>{labels[issue.code] ?? issue.code}</span>
          {issue.spaceId && <small>주차면 {issue.spaceId.slice(0, 8)}</small>}
          {issue.floorId && <Link to={`/floors/${issue.floorId}/map`}>층 도면 열기</Link>}
        </li>)}</ul>}
    </section>}
  </main>;
}
