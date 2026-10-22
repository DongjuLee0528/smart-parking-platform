export type FloorMap = {
  floorId: string;
  floorName: string;
  mapVersion: number;
  drawing: MapDrawing | null;
};

export type MapDrawing = {
  version: number;
  url: string;
  mediaType: string;
  width: number;
  height: number;
};
