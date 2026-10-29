export interface BoundingBox {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export interface CarPartClass {
  id: number;
  key: string;
  labelPt: string;
  color: string;
  rgb: [number, number, number];
  category: 'front' | 'rear' | 'side' | 'glass_wheel';
  valMap50: number;
  instances: number;
}

export interface PolygonPoint {
  x: number;
  y: number;
}

export interface RawDetection {
  classId: number;
  confidence: number;
  box: BoundingBox;
  polygon?: PolygonPoint[];
}

export interface SegmentedPart {
  id: string;
  partClass: CarPartClass;
  confidence: number;
  box: BoundingBox;
  polygon: PolygonPoint[];
  areaPercentage: number;
}

export interface DetectionResult {
  parts: SegmentedPart[];
  inferenceTimeMs: number;
  preprocessTimeMs: number;
  postprocessTimeMs: number;
  imageWidth: number;
  imageHeight: number;
}

export interface VehiclePreset {
  id: string;
  name: string;
  subtitle: string;
  viewAngle: 'front_quarter' | 'rear_hatch' | 'side_sedan' | 'front_closeup';
  width: number;
  height: number;
  rawDetections: RawDetection[];
}

export const CAR_PARTS_CATALOG: CarPartClass[] = [
  { id: 0, key: 'back_bumper', labelPt: 'Para-choque Traseiro', color: '#FF6347', rgb: [255, 99, 71], category: 'rear', valMap50: 0.892, instances: 184 },
  { id: 1, key: 'back_door', labelPt: 'Porta Traseira', color: '#36A2EB', rgb: [54, 162, 235], category: 'side', valMap50: 0.845, instances: 142 },
  { id: 2, key: 'back_glass', labelPt: 'Vidro Traseiro', color: '#4BC0C0', rgb: [75, 192, 192], category: 'glass_wheel', valMap50: 0.914, instances: 176 },
  { id: 3, key: 'back_left_door', labelPt: 'Porta Traseira Esquerda', color: '#9966FF', rgb: [153, 102, 255], category: 'side', valMap50: 0.818, instances: 98 },
  { id: 4, key: 'back_left_light', labelPt: 'Lanterna Traseira Esq.', color: '#FF9F40', rgb: [255, 159, 64], category: 'rear', valMap50: 0.784, instances: 112 },
  { id: 5, key: 'back_light', labelPt: 'Lanterna Traseira', color: '#FFCD56', rgb: [255, 205, 86], category: 'rear', valMap50: 0.851, instances: 164 },
  { id: 6, key: 'back_right_door', labelPt: 'Porta Traseira Direita', color: '#C9CBCF', rgb: [201, 203, 207], category: 'side', valMap50: 0.812, instances: 94 },
  { id: 7, key: 'back_right_light', labelPt: 'Lanterna Traseira Dir.', color: '#FF6384', rgb: [255, 99, 132], category: 'rear', valMap50: 0.791, instances: 108 },
  { id: 8, key: 'front_bumper', labelPt: 'Para-choque Dianteiro', color: '#00CC96', rgb: [0, 204, 150], category: 'front', valMap50: 0.923, instances: 215 },
  { id: 9, key: 'front_door', labelPt: 'Porta Dianteira', color: '#AB63FA', rgb: [171, 99, 250], category: 'side', valMap50: 0.867, instances: 156 },
  { id: 10, key: 'front_glass', labelPt: 'Para-brisa Dianteiro', color: '#19D3F2', rgb: [25, 211, 242], category: 'glass_wheel', valMap50: 0.941, instances: 210 },
  { id: 11, key: 'front_left_door', labelPt: 'Porta Dianteira Esq.', color: '#FECB52', rgb: [254, 203, 82], category: 'side', valMap50: 0.839, instances: 104 },
  { id: 12, key: 'front_left_light', labelPt: 'Farol Dianteiro Esq.', color: '#E763FA', rgb: [231, 99, 250], category: 'front', valMap50: 0.848, instances: 119 },
  { id: 13, key: 'front_light', labelPt: 'Farol Dianteiro', color: '#13B5EA', rgb: [19, 181, 234], category: 'front', valMap50: 0.876, instances: 178 },
  { id: 14, key: 'front_right_door', labelPt: 'Porta Dianteira Dir.', color: '#835AF1', rgb: [131, 90, 241], category: 'side', valMap50: 0.832, instances: 101 },
  { id: 15, key: 'front_right_light', labelPt: 'Farol Dianteiro Dir.', color: '#F67E00', rgb: [246, 126, 0], category: 'front', valMap50: 0.854, instances: 121 },
  { id: 16, key: 'hood', labelPt: 'Capô', color: '#2CA02C', rgb: [44, 160, 44], category: 'front', valMap50: 0.935, instances: 208 },
  { id: 17, key: 'left_mirror', labelPt: 'Retrovisor Esquerdo', color: '#D62728', rgb: [214, 39, 40], category: 'side', valMap50: 0.692, instances: 74 },
  { id: 18, key: 'object', labelPt: 'Objeto Genérico', color: '#9467BD', rgb: [148, 103, 189], category: 'glass_wheel', valMap50: 0.645, instances: 42 },
  { id: 19, key: 'right_mirror', labelPt: 'Retrovisor Direito', color: '#8C564B', rgb: [140, 86, 75], category: 'side', valMap50: 0.688, instances: 71 },
  { id: 20, key: 'tailgate', labelPt: 'Tampa Traseira (Hatch)', color: '#E377C2', rgb: [227, 119, 194], category: 'rear', valMap50: 0.862, instances: 96 },
  { id: 21, key: 'trunk', labelPt: 'Porta-Malas (Sedan)', color: '#7F7F7F', rgb: [127, 127, 127], category: 'rear', valMap50: 0.849, instances: 88 },
  { id: 22, key: 'wheel', labelPt: 'Roda / Pneu', color: '#BCBD22', rgb: [188, 189, 34], category: 'glass_wheel', valMap50: 0.948, instances: 379 },
];

export function getCarPartClass(classId: number): CarPartClass {
  return (
    CAR_PARTS_CATALOG[classId] ?? {
      id: classId,
      key: `part_${classId}`,
      labelPt: `Peça ${classId}`,
      color: '#64C8FF',
      rgb: [100, 200, 255],
      category: 'glass_wheel',
      valMap50: 0.75,
      instances: 50,
    }
  );
}

export function getBoxWidth(box: BoundingBox): number {
  return Math.max(0, box.right - box.left);
}

export function getBoxHeight(box: BoundingBox): number {
  return Math.max(0, box.bottom - box.top);
}

export function getBoxArea(box: BoundingBox): number {
  return getBoxWidth(box) * getBoxHeight(box);
}

/**
 * Exact port of YoloSegmenter.computeIoU from Models.kt / YoloSegmenter.kt
 */
export function computeIoU(b1: BoundingBox, b2: BoundingBox): number {
  const interLeft = Math.max(b1.left, b2.left);
  const interTop = Math.max(b1.top, b2.top);
  const interRight = Math.min(b1.right, b2.right);
  const interBottom = Math.min(b1.bottom, b2.bottom);

  const interW = Math.max(0, interRight - interLeft);
  const interH = Math.max(0, interBottom - interTop);
  const interArea = interW * interH;

  const area1 = getBoxArea(b1);
  const area2 = getBoxArea(b2);
  const unionArea = area1 + area2 - interArea;

  return unionArea > 0 ? interArea / unionArea : 0;
}

/**
 * Exact port of YoloSegmenter.applyNMS from YoloSegmenter.kt
 */
export function applyNMS(boxes: RawDetection[], iouThreshold: number): RawDetection[] {
  const sorted = [...boxes].sort((a, b) => b.confidence - a.confidence);
  const selected: RawDetection[] = [];

  while (sorted.length > 0) {
    const current = sorted.shift()!;
    selected.push(current);

    for (let i = sorted.length - 1; i >= 0; i--) {
      const next = sorted[i];
      if (current.classId === next.classId && computeIoU(current.box, next.box) > iouThreshold) {
        sorted.splice(i, 1);
      }
    }
  }

  return selected;
}

/**
 * Unmaps letterbox coordinates back to normalized original image coordinates [0..1],
 * matching YoloSegmenter.kt lines 191-294.
 */
export function unmapLetterboxBox(
  cx: number,
  cy: number,
  w: number,
  h: number,
  originalWidth: number,
  originalHeight: number,
  inputWidth = 640,
  inputHeight = 640
): BoundingBox {
  let normCx = cx;
  let normCy = cy;
  let normW = w;
  let normH = h;

  if (normCx > 1.5 || normCy > 1.5 || normW > 1.5 || normH > 1.5) {
    normCx /= inputWidth;
    normCy /= inputHeight;
    normW /= inputWidth;
    normH /= inputHeight;
  }

  const scale = Math.min(inputWidth / originalWidth, inputHeight / originalHeight);
  const scaledW = Math.floor(originalWidth * scale);
  const scaledH = Math.floor(originalHeight * scale);
  const padX = (inputWidth - scaledW) / 2;
  const padY = (inputHeight - scaledH) / 2;

  const clamp01 = (val: number) => Math.min(1, Math.max(0, val));

  const origLeft = clamp01(((normCx - normW / 2) * inputWidth - padX) / scaledW);
  const origTop = clamp01(((normCy - normH / 2) * inputHeight - padY) / scaledH);
  const origRight = clamp01(((normCx + normW / 2) * inputWidth - padX) / scaledW);
  const origBottom = clamp01(((normCy + normH / 2) * inputHeight - padY) / scaledH);

  return {
    left: origLeft,
    top: origTop,
    right: origRight,
    bottom: origBottom,
  };
}

export function buildDefaultPolygonForBox(box: BoundingBox): PolygonPoint[] {
  const w = getBoxWidth(box);
  const h = getBoxHeight(box);
  const insetX = w * 0.08;
  const insetY = h * 0.08;
  return [
    { x: box.left + insetX, y: box.top + insetY * 0.5 },
    { x: box.right - insetX, y: box.top + insetY * 0.5 },
    { x: box.right - insetX * 0.3, y: box.top + h * 0.5 },
    { x: box.right - insetX, y: box.bottom - insetY * 0.5 },
    { x: box.left + insetX, y: box.bottom - insetY * 0.5 },
    { x: box.left + insetX * 0.3, y: box.top + h * 0.5 },
  ];
}

/**
 * Runs the YOLO26-seg filtering + NMS + mask polygon reconstruction pipeline.
 */
export function runSegmentationPipeline(
  rawCandidates: RawDetection[],
  confThreshold: number,
  iouThreshold: number,
  imageWidth = 1280,
  imageHeight = 720
): DetectionResult {
  const filtered = rawCandidates.filter(
    (d) => d.confidence >= confThreshold && d.classId >= 0 && d.classId < CAR_PARTS_CATALOG.length
  );

  const nmsDetections = applyNMS(filtered, iouThreshold);

  const parts: SegmentedPart[] = nmsDetections.map((det, idx) => {
    const partClass = getCarPartClass(det.classId);
    const polygon =
      det.polygon && det.polygon.length >= 3 ? det.polygon : buildDefaultPolygonForBox(det.box);
    const areaPercentage = Number((getBoxArea(det.box) * 100 * 0.78).toFixed(1));

    return {
      id: `${partClass.key}_${idx}`,
      partClass,
      confidence: det.confidence,
      box: det.box,
      polygon,
      areaPercentage,
    };
  });

  return {
    parts,
    preprocessTimeMs: 2,
    inferenceTimeMs: 15,
    postprocessTimeMs: 8,
    imageWidth,
    imageHeight,
  };
}

export const VEHICLE_PRESETS: VehiclePreset[] = [
  {
    id: 'front_quarter',
    name: 'Sedan Esportivo — Dianteira 3/4',
    subtitle: 'Inspeção frontal completa (Capô, Para-choque, Faróis, Para-brisa, Portas e Rodas)',
    viewAngle: 'front_quarter',
    width: 1280,
    height: 720,
    rawDetections: [
      {
        classId: 8, // front_bumper
        confidence: 0.94,
        box: { left: 0.08, top: 0.56, right: 0.56, bottom: 0.83 },
        polygon: [
          { x: 0.09, y: 0.60 },
          { x: 0.53, y: 0.57 },
          { x: 0.56, y: 0.74 },
          { x: 0.49, y: 0.82 },
          { x: 0.12, y: 0.81 },
          { x: 0.08, y: 0.71 },
        ],
      },
      {
        classId: 8, // duplicate overlapping front_bumper candidate to test NMS
        confidence: 0.51,
        box: { left: 0.10, top: 0.58, right: 0.54, bottom: 0.82 },
      },
      {
        classId: 16, // hood
        confidence: 0.92,
        box: { left: 0.13, top: 0.39, right: 0.59, bottom: 0.59 },
        polygon: [
          { x: 0.25, y: 0.40 },
          { x: 0.58, y: 0.41 },
          { x: 0.52, y: 0.57 },
          { x: 0.14, y: 0.57 },
        ],
      },
      {
        classId: 10, // front_glass
        confidence: 0.91,
        box: { left: 0.32, top: 0.21, right: 0.67, bottom: 0.41 },
        polygon: [
          { x: 0.41, y: 0.22 },
          { x: 0.66, y: 0.23 },
          { x: 0.58, y: 0.40 },
          { x: 0.32, y: 0.39 },
        ],
      },
      {
        classId: 12, // front_left_light
        confidence: 0.88,
        box: { left: 0.42, top: 0.50, right: 0.56, bottom: 0.61 },
        polygon: [
          { x: 0.43, y: 0.52 },
          { x: 0.55, y: 0.50 },
          { x: 0.56, y: 0.59 },
          { x: 0.44, y: 0.60 },
        ],
      },
      {
        classId: 15, // front_right_light
        confidence: 0.84,
        box: { left: 0.10, top: 0.49, right: 0.21, bottom: 0.59 },
        polygon: [
          { x: 0.11, y: 0.50 },
          { x: 0.20, y: 0.51 },
          { x: 0.19, y: 0.58 },
          { x: 0.10, y: 0.58 },
        ],
      },
      {
        classId: 11, // front_left_door
        confidence: 0.86,
        box: { left: 0.64, top: 0.36, right: 0.81, bottom: 0.72 },
        polygon: [
          { x: 0.66, y: 0.37 },
          { x: 0.80, y: 0.37 },
          { x: 0.81, y: 0.70 },
          { x: 0.65, y: 0.71 },
        ],
      },
      {
        classId: 3, // back_left_door
        confidence: 0.79,
        box: { left: 0.79, top: 0.35, right: 0.91, bottom: 0.69 },
        polygon: [
          { x: 0.80, y: 0.36 },
          { x: 0.90, y: 0.37 },
          { x: 0.91, y: 0.67 },
          { x: 0.80, y: 0.69 },
        ],
      },
      {
        classId: 22, // wheel (front)
        confidence: 0.95,
        box: { left: 0.53, top: 0.57, right: 0.67, bottom: 0.85 },
      },
      {
        classId: 22, // wheel (rear)
        confidence: 0.89,
        box: { left: 0.85, top: 0.54, right: 0.95, bottom: 0.79 },
      },
      {
        classId: 17, // left_mirror
        confidence: 0.64,
        box: { left: 0.63, top: 0.34, right: 0.70, bottom: 0.41 },
      },
      {
        classId: 19, // right_mirror (low confidence candidate)
        confidence: 0.22,
        box: { left: 0.25, top: 0.33, right: 0.31, bottom: 0.39 },
      },
    ],
  },
  {
    id: 'rear_hatch',
    name: 'Hatchback Compacto — Traseira (car88_jpg)',
    subtitle: 'Cena de validação do Colab: Para-choque Traseiro, Vidro Traseiro, Lanternas e Tampa',
    viewAngle: 'rear_hatch',
    width: 1280,
    height: 720,
    rawDetections: [
      {
        classId: 0, // back_bumper
        confidence: 0.93,
        box: { left: 0.19, top: 0.61, right: 0.81, bottom: 0.83 },
        polygon: [
          { x: 0.21, y: 0.62 },
          { x: 0.79, y: 0.62 },
          { x: 0.81, y: 0.78 },
          { x: 0.75, y: 0.83 },
          { x: 0.25, y: 0.83 },
          { x: 0.19, y: 0.78 },
        ],
      },
      {
        classId: 2, // back_glass
        confidence: 0.91,
        box: { left: 0.27, top: 0.22, right: 0.73, bottom: 0.43 },
        polygon: [
          { x: 0.31, y: 0.23 },
          { x: 0.69, y: 0.23 },
          { x: 0.73, y: 0.42 },
          { x: 0.27, y: 0.42 },
        ],
      },
      {
        classId: 20, // tailgate
        confidence: 0.87,
        box: { left: 0.25, top: 0.42, right: 0.75, bottom: 0.63 },
        polygon: [
          { x: 0.27, y: 0.43 },
          { x: 0.73, y: 0.43 },
          { x: 0.75, y: 0.62 },
          { x: 0.25, y: 0.62 },
        ],
      },
      {
        classId: 4, // back_left_light
        confidence: 0.89,
        box: { left: 0.20, top: 0.43, right: 0.31, bottom: 0.56 },
      },
      {
        classId: 7, // back_right_light
        confidence: 0.88,
        box: { left: 0.69, top: 0.43, right: 0.80, bottom: 0.56 },
      },
      {
        classId: 22, // wheel left
        confidence: 0.82,
        box: { left: 0.15, top: 0.66, right: 0.26, bottom: 0.88 },
      },
      {
        classId: 22, // wheel right
        confidence: 0.81,
        box: { left: 0.74, top: 0.66, right: 0.85, bottom: 0.88 },
      },
      {
        classId: 17, // left_mirror
        confidence: 0.48,
        box: { left: 0.16, top: 0.36, right: 0.23, bottom: 0.42 },
      },
    ],
  },
  {
    id: 'side_sedan',
    name: 'SUV Executivo — Perfil Lateral',
    subtitle: 'Inspeção de Portas Dianteira/Traseira, Retrovisor, Rodas e Para-choques',
    viewAngle: 'side_sedan',
    width: 1280,
    height: 720,
    rawDetections: [
      {
        classId: 9, // front_door
        confidence: 0.92,
        box: { left: 0.34, top: 0.28, right: 0.55, bottom: 0.73 },
      },
      {
        classId: 1, // back_door
        confidence: 0.90,
        box: { left: 0.55, top: 0.28, right: 0.75, bottom: 0.73 },
      },
      {
        classId: 22, // front wheel
        confidence: 0.96,
        box: { left: 0.16, top: 0.56, right: 0.32, bottom: 0.86 },
      },
      {
        classId: 22, // rear wheel
        confidence: 0.95,
        box: { left: 0.71, top: 0.56, right: 0.87, bottom: 0.86 },
      },
      {
        classId: 16, // hood
        confidence: 0.83,
        box: { left: 0.09, top: 0.41, right: 0.34, bottom: 0.56 },
      },
      {
        classId: 8, // front_bumper
        confidence: 0.81,
        box: { left: 0.05, top: 0.55, right: 0.18, bottom: 0.78 },
      },
      {
        classId: 0, // back_bumper
        confidence: 0.80,
        box: { left: 0.84, top: 0.54, right: 0.95, bottom: 0.77 },
      },
      {
        classId: 17, // left_mirror
        confidence: 0.76,
        box: { left: 0.33, top: 0.39, right: 0.39, bottom: 0.46 },
      },
      {
        classId: 21, // trunk
        confidence: 0.72,
        box: { left: 0.75, top: 0.37, right: 0.93, bottom: 0.56 },
      },
    ],
  },
];
