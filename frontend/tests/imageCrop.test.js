import test from 'node:test';
import assert from 'node:assert/strict';
import { cropOutputSize, exportCroppedImage, fitCrop, isFullCrop, moveCrop, resizeCrop } from '../src/utils/imageCrop.js';

test('portrait images retain their entire height when keeping the original', async () => {
  const bounds = { width: 600, height: 1800 };
  const crop = fitCrop(bounds.width, bounds.height, bounds.width / bounds.height);
  assert.deepEqual(crop, { x: 0, y: 0, width: 600, height: 1800 });
  const file = new File(['original animation'], 'image.gif', { type: 'image/gif' });
  assert.equal(await exportCroppedImage({ naturalWidth: 600, naturalHeight: 1800 }, file, crop), file);
});

test('avatar and cover presets fit inside portrait and landscape images', () => {
  assert.deepEqual(fitCrop(600, 1800, 1), { x: 0, y: 600, width: 600, height: 600 });
  assert.deepEqual(fitCrop(1600, 900, 1), { x: 350, y: 0, width: 900, height: 900 });
  const cover = fitCrop(600, 1800, 16 / 9);
  assert.equal(cover.width / cover.height, 16 / 9);
  assert.equal(cover.width, 600);
});

test('moving a selection reaches the bottom without changing its size', () => {
  const bounds = { width: 600, height: 1800 };
  const moved = moveCrop(fitCrop(600, 1800, 1), 9999, 9999, bounds);
  assert.deepEqual(moved, { x: 0, y: 1200, width: 600, height: 600 });
  assert.deepEqual(moveCrop(moved, -9999, -9999, bounds), { ...moved, x: 0, y: 0 });
});

test('all corners remain in bounds under extreme drags, including very small images', () => {
  for (const bounds of [{ width: 1600, height: 900 }, { width: 400, height: 1600 }, { width: 1, height: 2 }]) {
    for (const ratio of [null, 1, 16 / 9, 9 / 16]) {
      for (const corner of ['nw', 'ne', 'sw', 'se']) {
        for (const dx of [-10000, -50, 0, 50, 10000]) {
          for (const dy of [-10000, -50, 0, 50, 10000]) {
            const initial = fitCrop(bounds.width, bounds.height, ratio);
            const crop = resizeCrop(initial, corner, dx, dy, bounds, ratio);
            assert.ok(crop.width > 0 && crop.height > 0);
            assert.ok(crop.x >= -1e-8 && crop.y >= -1e-8);
            assert.ok(crop.x + crop.width <= bounds.width + 1e-8);
            assert.ok(crop.y + crop.height <= bounds.height + 1e-8);
            if (ratio) assert.ok(Math.abs(crop.width / crop.height - ratio) < 1e-8);
            const anchorX = corner.includes('w') ? crop.x + crop.width : crop.x;
            const anchorY = corner.includes('n') ? crop.y + crop.height : crop.y;
            assert.ok(Math.abs(anchorX - (corner.includes('w') ? initial.x + initial.width : initial.x)) < 1e-8);
            assert.ok(Math.abs(anchorY - (corner.includes('n') ? initial.y + initial.height : initial.y)) < 1e-8);
          }
        }
      }
    }
  }
});

test('free crop resizes width and height independently', () => {
  assert.deepEqual(resizeCrop({ x: 10, y: 20, width: 200, height: 200 }, 'se', -100, -50, { width: 500, height: 500 }),
    { x: 10, y: 20, width: 100, height: 150 });
});

test('export size is bounded without stretching or upscaling the crop', () => {
  assert.deepEqual(cropOutputSize({ width: 8000, height: 4000 }), { width: 4096, height: 2048 });
  assert.deepEqual(cropOutputSize({ width: 100, height: 200 }), { width: 100, height: 200 });
  assert.equal(isFullCrop({ x: 0, y: 1, width: 600, height: 1799 }, { width: 600, height: 1800 }), false);
});
