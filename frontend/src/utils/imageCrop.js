const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

export function fitCrop(width, height, ratio = null) {
  const cropWidth = ratio ? Math.min(width, height * ratio) : width;
  const cropHeight = ratio ? cropWidth / ratio : height;
  return { x: (width - cropWidth) / 2, y: (height - cropHeight) / 2, width: cropWidth, height: cropHeight };
}

export function moveCrop(crop, dx, dy, bounds) {
  return { ...crop, x: clamp(crop.x + dx, 0, bounds.width - crop.width), y: clamp(crop.y + dy, 0, bounds.height - crop.height) };
}

export function resizeCrop(crop, corner, dx, dy, bounds, ratio = null) {
  const left = corner.includes('w');
  const top = corner.includes('n');
  const anchorX = left ? crop.x + crop.width : crop.x;
  const anchorY = top ? crop.y + crop.height : crop.y;
  const maxWidth = left ? anchorX : bounds.width - anchorX;
  const maxHeight = top ? anchorY : bounds.height - anchorY;
  let width = crop.width + (left ? -dx : dx);
  let height = crop.height + (top ? -dy : dy);
  if (ratio) {
    const max = Math.min(maxWidth / ratio, maxHeight);
    height = clamp((width * ratio + height) / (ratio * ratio + 1), Math.min(24, 24 / ratio, max), max);
    width = height * ratio;
  } else {
    width = clamp(width, Math.min(24, maxWidth), maxWidth);
    height = clamp(height, Math.min(24, maxHeight), maxHeight);
  }
  return { x: left ? anchorX - width : anchorX, y: top ? anchorY - height : anchorY, width, height };
}

export function isFullCrop(crop, bounds) {
  return Math.abs(crop.x) < 0.01 && Math.abs(crop.y) < 0.01
    && Math.abs(crop.width - bounds.width) < 0.01 && Math.abs(crop.height - bounds.height) < 0.01;
}

export function cropOutputSize(crop) {
  const scale = Math.min(1, 4096 / Math.max(crop.width, crop.height));
  return { width: Math.max(1, Math.round(crop.width * scale)), height: Math.max(1, Math.round(crop.height * scale)) };
}

export async function exportCroppedImage(image, file, crop) {
  if (isFullCrop(crop, { width: image.naturalWidth, height: image.naturalHeight })) return file;
  const canvas = document.createElement('canvas');
  const size = cropOutputSize(crop);
  canvas.width = size.width;
  canvas.height = size.height;
  const context = canvas.getContext('2d');
  if (!context) throw new Error('当前浏览器无法裁剪图片');
  context.drawImage(image, crop.x, crop.y, crop.width, crop.height, 0, 0, size.width, size.height);
  const mime = ['image/jpeg', 'image/webp'].includes(file.type) ? file.type : 'image/png';
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, mime, 0.92));
  if (!blob) throw new Error('图片裁剪失败，请重新选择图片');
  if (blob.size > 5 * 1024 * 1024) throw new Error('裁剪后的图片超过 5MB，请缩小裁剪范围或更换图片');
  const extension = { 'image/jpeg': 'jpg', 'image/webp': 'webp', 'image/png': 'png' }[blob.type] || 'png';
  return new File([blob], `${file.name.replace(/\.[^.]+$/, '')}-cropped.${extension}`, { type: blob.type });
}
