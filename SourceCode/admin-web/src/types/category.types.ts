export type CategoryCode =
  | 'TRAFFIC_JAM'
  | 'ROAD_BLOCK'
  | 'ROAD_DAMAGE'
  | 'BROKEN_TRAFFIC_LIGHT'
  | 'FALLEN_TREE'
  | 'FLOOD';

export interface AdminCategory {
  id: number;
  code: CategoryCode;
  name: string;
  color: string;
}
