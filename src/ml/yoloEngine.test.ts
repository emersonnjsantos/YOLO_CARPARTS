import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CAR_PARTS_CATALOG,
  getCarPartClass,
  computeIoU,
  applyNMS,
  unmapLetterboxBox,
  runSegmentationPipeline,
  VEHICLE_PRESETS,
} from './yoloEngine';

test('CAR_PARTS_CATALOG contains all 23 classes matching Models.kt', () => {
  assert.equal(CAR_PARTS_CATALOG.length, 23);
  assert.equal(getCarPartClass(0).key, 'back_bumper');
  assert.equal(getCarPartClass(8).labelPt, 'Para-choque Dianteiro');
  assert.equal(getCarPartClass(22).key, 'wheel');
  assert.equal(getCarPartClass(99).labelPt, 'Peça 99');
});

test('computeIoU calculates exact intersection over union', () => {
  const b1 = { left: 0.1, top: 0.1, right: 0.5, bottom: 0.5 };
  const b2 = { left: 0.1, top: 0.1, right: 0.5, bottom: 0.5 };
  assert.equal(computeIoU(b1, b2), 1);

  const nonOverlapping = { left: 0.6, top: 0.6, right: 0.9, bottom: 0.9 };
  assert.equal(computeIoU(b1, nonOverlapping), 0);
});

test('applyNMS suppresses overlapping boxes of the same class above iouThreshold', () => {
  const candidates = [
    { classId: 8, confidence: 0.94, box: { left: 0.1, top: 0.5, right: 0.5, bottom: 0.8 } },
    { classId: 8, confidence: 0.61, box: { left: 0.12, top: 0.52, right: 0.49, bottom: 0.79 } },
    { classId: 16, confidence: 0.89, box: { left: 0.1, top: 0.5, right: 0.5, bottom: 0.8 } },
  ];

  const kept = applyNMS(candidates, 0.45);
  assert.equal(kept.length, 2);
  assert.equal(kept[0].classId, 8);
  assert.equal(kept[0].confidence, 0.94);
  assert.equal(kept[1].classId, 16);
});

test('unmapLetterboxBox restores normalized coordinates from 640x640 letterbox', () => {
  const box = unmapLetterboxBox(320, 320, 320, 180, 1280, 720, 640, 640);
  assert.ok(box.left >= 0.24 && box.left <= 0.26);
  assert.ok(box.right >= 0.74 && box.right <= 0.76);
});

test('runSegmentationPipeline respects confidence and IoU thresholds', () => {
  const preset = VEHICLE_PRESETS[0];
  const lowConfResult = runSegmentationPipeline(preset.rawDetections, 0.20, 0.45);
  const highConfResult = runSegmentationPipeline(preset.rawDetections, 0.85, 0.45);

  assert.ok(lowConfResult.parts.length > highConfResult.parts.length);
  assert.ok(highConfResult.parts.every((p) => p.confidence >= 0.85));
});
