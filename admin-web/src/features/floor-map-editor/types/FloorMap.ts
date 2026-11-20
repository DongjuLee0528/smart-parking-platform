export type FloorMap = {
  floorId: string;
  floorName: string;
  mapVersion: number;
  drawing: MapDrawing | null;
  zones: { id: string; name: string }[];
};

export type MapDrawing = {
  version: number;
  url: string;
  mediaType: string;
  width: number;
  height: number;
};

export type Point = { x: number; y: number };

export type ParkingSpace = {
  id: string | null;
  zoneId: string;
  spaceNumber: string;
  type: 'GENERAL' | 'DISABLED' | 'ELECTRIC' | 'COMPACT';
  active: boolean;
  mapPolygon: Point[];
  cameraMappings: { cameraId: string; priority: number; imagePolygon: Point[] }[];
};

export type ParkingSpaces = { floorId: string; version: number; spaces: ParkingSpace[] };
