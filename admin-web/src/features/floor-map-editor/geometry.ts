import type { Point } from './types/FloorMap';

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));
const round = (value: number) => Math.round(value * 1_000_000) / 1_000_000;

export function bounds(points: Point[]) {
  return {
    left: Math.min(...points.map((point) => point.x)),
    top: Math.min(...points.map((point) => point.y)),
    right: Math.max(...points.map((point) => point.x)),
    bottom: Math.max(...points.map((point) => point.y)),
  };
}

export function movePolygon(points: Point[], dx: number, dy: number): Point[] {
  const box = bounds(points);
  const x = clamp(dx, -box.left, 1 - box.right);
  const y = clamp(dy, -box.top, 1 - box.bottom);
  return points.map((point) => ({ x: round(point.x + x), y: round(point.y + y) }));
}

export function rotatePolygon(points: Point[], width: number, height: number): Point[] | null {
  const box = bounds(points);
  const cx = (box.left + box.right) / 2;
  const cy = (box.top + box.bottom) / 2;
  const rotated = points.map((point) => ({
    x: round(cx - ((point.y - cy) * height) / width),
    y: round(cy + ((point.x - cx) * width) / height),
  }));
  return rotated.every((point) => point.x >= 0 && point.x <= 1 && point.y >= 0 && point.y <= 1)
    ? rotated : null;
}

export function newPolygon(x: number, y: number): Point[] {
  const left = clamp(x - 0.04, 0, 0.92);
  const top = clamp(y - 0.06, 0, 0.88);
  return [
    { x: round(left), y: round(top) },
    { x: round(left + 0.08), y: round(top) },
    { x: round(left + 0.08), y: round(top + 0.12) },
    { x: round(left), y: round(top + 0.12) },
  ];
}
