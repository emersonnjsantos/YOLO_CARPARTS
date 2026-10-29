import React, { useEffect, useRef } from 'react';
import { DetectionResult, VehiclePreset } from '../ml/yoloEngine';

interface DetectionCanvasProps {
  preset: VehiclePreset;
  customImage: HTMLImageElement | null;
  detectionResult: DetectionResult;
  maskOpacity: number; // 40..255
  showMasks: boolean;
  showBoxes: boolean;
  showLabels: boolean;
  highlightedPartId: string | null;
  isMirrored?: boolean;
  scanProgress?: number; // 0..1 for live camera effect
}

function drawProceduralVehicle(
  ctx: CanvasRenderingContext2D,
  viewAngle: VehiclePreset['viewAngle'],
  w: number,
  h: number
) {
  // Studio backdrop
  const bgGrad = ctx.createLinearGradient(0, 0, 0, h);
  bgGrad.addColorStop(0, '#0f172a');
  bgGrad.addColorStop(0.62, '#1e293b');
  bgGrad.addColorStop(0.63, '#0b1120');
  bgGrad.addColorStop(1, '#020617');
  ctx.fillStyle = bgGrad;
  ctx.fillRect(0, 0, w, h);

  // Subtle floor grid
  ctx.strokeStyle = 'rgba(148, 163, 184, 0.12)';
  ctx.lineWidth = 1;
  const horizonY = h * 0.63;
  for (let i = 0; i <= 12; i++) {
    const xTop = (w / 12) * i;
    const xBot = (xTop - w / 2) * 2.1 + w / 2;
    ctx.beginPath();
    ctx.moveTo(xTop, horizonY);
    ctx.lineTo(xBot, h);
    ctx.stroke();
  }

  // Floor shadow
  ctx.fillStyle = 'rgba(0, 0, 0, 0.65)';
  ctx.beginPath();
  ctx.ellipse(w * 0.5, h * 0.80, w * 0.42, h * 0.08, 0, 0, Math.PI * 2);
  ctx.fill();

  if (viewAngle === 'front_quarter') {
    // Main car body (3/4 front)
    const bodyGrad = ctx.createLinearGradient(w * 0.08, h * 0.2, w * 0.92, h * 0.82);
    bodyGrad.addColorStop(0, '#334155');
    bodyGrad.addColorStop(0.5, '#475569');
    bodyGrad.addColorStop(1, '#1e293b');
    ctx.fillStyle = bodyGrad;
    ctx.strokeStyle = '#94a3b8';
    ctx.lineWidth = 2.5;

    ctx.beginPath();
    ctx.moveTo(w * 0.08, h * 0.68);
    ctx.lineTo(w * 0.12, h * 0.48);
    ctx.lineTo(w * 0.31, h * 0.38);
    ctx.lineTo(w * 0.43, h * 0.21);
    ctx.lineTo(w * 0.78, h * 0.22);
    ctx.lineTo(w * 0.92, h * 0.38);
    ctx.lineTo(w * 0.95, h * 0.69);
    ctx.lineTo(w * 0.52, h * 0.82);
    ctx.lineTo(w * 0.10, h * 0.80);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Hood surface
    ctx.fillStyle = '#3b4c63';
    ctx.beginPath();
    ctx.moveTo(w * 0.13, h * 0.56);
    ctx.lineTo(w * 0.26, h * 0.40);
    ctx.lineTo(w * 0.58, h * 0.41);
    ctx.lineTo(w * 0.52, h * 0.57);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Windshield
    ctx.fillStyle = 'rgba(56, 189, 248, 0.28)';
    ctx.beginPath();
    ctx.moveTo(w * 0.32, h * 0.39);
    ctx.lineTo(w * 0.42, h * 0.22);
    ctx.lineTo(w * 0.66, h * 0.23);
    ctx.lineTo(w * 0.58, h * 0.40);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Side windows & doors
    ctx.fillStyle = 'rgba(30, 41, 59, 0.85)';
    ctx.beginPath();
    ctx.moveTo(w * 0.63, h * 0.39);
    ctx.lineTo(w * 0.69, h * 0.24);
    ctx.lineTo(w * 0.87, h * 0.26);
    ctx.lineTo(w * 0.90, h * 0.38);
    ctx.closePath();
    ctx.fill();

    // Grille & Front bumper
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(w * 0.19, h * 0.59, w * 0.24, h * 0.14);

    // Headlights
    ctx.fillStyle = '#e0f2fe';
    ctx.fillRect(w * 0.43, h * 0.51, w * 0.12, h * 0.08);
    ctx.fillRect(w * 0.11, h * 0.50, w * 0.09, h * 0.08);

    // Wheels
    const drawWheel = (cx: number, cy: number, rx: number, ry: number) => {
      ctx.fillStyle = '#090d16';
      ctx.beginPath();
      ctx.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#cbd5e1';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.ellipse(cx, cy, rx * 0.62, ry * 0.62, 0, 0, Math.PI * 2);
      ctx.stroke();
    };
    drawWheel(w * 0.60, h * 0.71, w * 0.065, h * 0.13);
    drawWheel(w * 0.90, h * 0.66, w * 0.048, h * 0.11);
  } else if (viewAngle === 'rear_hatch') {
    // Rear Hatchback view
    ctx.fillStyle = '#1e293b';
    ctx.fillRect(w * 0.16, h * 0.66, w * 0.10, h * 0.22);
    ctx.fillRect(w * 0.74, h * 0.66, w * 0.10, h * 0.22);

    const bodyGrad = ctx.createLinearGradient(w * 0.2, h * 0.18, w * 0.8, h * 0.82);
    bodyGrad.addColorStop(0, '#334155');
    bodyGrad.addColorStop(1, '#1e293b');
    ctx.fillStyle = bodyGrad;
    ctx.strokeStyle = '#94a3b8';
    ctx.lineWidth = 2.5;

    ctx.beginPath();
    ctx.moveTo(w * 0.20, h * 0.78);
    ctx.lineTo(w * 0.22, h * 0.42);
    ctx.lineTo(w * 0.30, h * 0.21);
    ctx.lineTo(w * 0.70, h * 0.21);
    ctx.lineTo(w * 0.78, h * 0.42);
    ctx.lineTo(w * 0.80, h * 0.78);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Rear glass
    ctx.fillStyle = 'rgba(56, 189, 248, 0.25)';
    ctx.beginPath();
    ctx.moveTo(w * 0.28, h * 0.42);
    ctx.lineTo(w * 0.32, h * 0.24);
    ctx.lineTo(w * 0.68, h * 0.24);
    ctx.lineTo(w * 0.72, h * 0.42);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Tail lights
    ctx.fillStyle = '#ef4444';
    ctx.fillRect(w * 0.21, h * 0.44, w * 0.10, h * 0.11);
    ctx.fillRect(w * 0.69, h * 0.44, w * 0.10, h * 0.11);

    // Rear bumper
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(w * 0.20, h * 0.62, w * 0.60, h * 0.19);
  } else {
    // Side profile view
    ctx.fillStyle = '#334155';
    ctx.strokeStyle = '#94a3b8';
    ctx.lineWidth = 2.5;

    ctx.beginPath();
    ctx.moveTo(w * 0.06, h * 0.72);
    ctx.lineTo(w * 0.09, h * 0.48);
    ctx.lineTo(w * 0.29, h * 0.41);
    ctx.lineTo(w * 0.41, h * 0.23);
    ctx.lineTo(w * 0.76, h * 0.23);
    ctx.lineTo(w * 0.92, h * 0.40);
    ctx.lineTo(w * 0.94, h * 0.72);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Wheels
    const drawSideWheel = (cx: number, cy: number, r: number) => {
      ctx.fillStyle = '#090d16';
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#cbd5e1';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.58, 0, Math.PI * 2);
      ctx.stroke();
    };
    drawSideWheel(w * 0.24, h * 0.72, h * 0.14);
    drawSideWheel(w * 0.79, h * 0.72, h * 0.14);
  }
}

export const DetectionCanvas: React.FC<DetectionCanvasProps> = ({
  preset,
  customImage,
  detectionResult,
  maskOpacity,
  showMasks,
  showBoxes,
  showLabels,
  highlightedPartId,
  isMirrored = false,
  scanProgress,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const w = canvas.width;
    const h = canvas.height;

    ctx.clearRect(0, 0, w, h);
    ctx.save();
    if (isMirrored) {
      ctx.translate(w, 0);
      ctx.scale(-1, 1);
    }

    if (customImage) {
      ctx.fillStyle = '#090d16';
      ctx.fillRect(0, 0, w, h);
      ctx.drawImage(customImage, 0, 0, w, h);
    } else {
      drawProceduralVehicle(ctx, preset.viewAngle, w, h);
    }
    ctx.restore();

    const alphaNormalized = Math.min(1, Math.max(0.1, maskOpacity / 255));
    const placedBadges: Array<{ left: number; top: number; right: number; bottom: number }> = [];

    for (const part of detectionResult.parts) {
      const isHighlighted = highlightedPartId === null || highlightedPartId === part.id;
      const effectiveAlpha = isHighlighted ? alphaNormalized : alphaNormalized * 0.25;
      const [r, g, b] = part.partClass.rgb;

      const left = (isMirrored ? 1 - part.box.right : part.box.left) * w;
      const right = (isMirrored ? 1 - part.box.left : part.box.right) * w;
      const top = part.box.top * h;
      const bottom = part.box.bottom * h;
      const boxW = Math.max(1, right - left);
      const boxH = Math.max(1, bottom - top);

      // 1. Segmentation Mask Polygon (DetectionOverlay.kt step 1)
      if (showMasks && part.polygon.length >= 3) {
        ctx.save();
        ctx.fillStyle = `rgba(${r}, ${g}, ${b}, ${effectiveAlpha})`;
        ctx.beginPath();
        part.polygon.forEach((pt, idx) => {
          const px = (isMirrored ? 1 - pt.x : pt.x) * w;
          const py = pt.y * h;
          if (idx === 0) ctx.moveTo(px, py);
          else ctx.lineTo(px, py);
        });
        ctx.closePath();
        ctx.fill();
        ctx.restore();
      }

      // 2. Rounded Bounding Box (DetectionOverlay.kt step 2)
      if (showBoxes) {
        ctx.save();
        ctx.strokeStyle = isHighlighted
          ? part.partClass.color
          : `rgba(${r}, ${g}, ${b}, 0.45)`;
        ctx.lineWidth = highlightedPartId === part.id ? 4.5 : 3;
        ctx.beginPath();
        ctx.roundRect(left, top, boxW, boxH, 10);
        ctx.stroke();
        ctx.restore();
      }

      // 3. Class + Confidence Badge with collision avoidance (DetectionOverlay.kt step 3)
      if (showLabels) {
        ctx.save();
        const labelText = `${part.partClass.labelPt} ${Math.round(part.confidence * 100)}%`;
        ctx.font = 'bold 13px Inter, system-ui, sans-serif';
        const textWidth = ctx.measureText(labelText).width;
        const padH = 10;
        const badgeW = textWidth + padH * 2;
        const badgeH = 25;
        let badgeLeft = Math.max(6, Math.min(w - badgeW - 6, left));
        let badgeTop = Math.max(6, top - badgeH - 3);

        // Nudge badge if it overlaps any previously placed badge
        for (let attempt = 0; attempt < 6; attempt++) {
          const hasCollision = placedBadges.some(
            (b) =>
              badgeLeft < b.right + 4 &&
              badgeLeft + badgeW + 4 > b.left &&
              badgeTop < b.bottom + 3 &&
              badgeTop + badgeH + 3 > b.top
          );
          if (!hasCollision) break;
          badgeTop = Math.min(h - badgeH - 6, badgeTop + badgeH + 4);
        }

        placedBadges.push({
          left: badgeLeft,
          top: badgeTop,
          right: badgeLeft + badgeW,
          bottom: badgeTop + badgeH,
        });

        ctx.fillStyle = isHighlighted
          ? part.partClass.color
          : `rgba(${r}, ${g}, ${b}, 0.55)`;
        ctx.beginPath();
        ctx.roundRect(badgeLeft, badgeTop, badgeW, badgeH, 6);
        ctx.fill();

        ctx.fillStyle = '#ffffff';
        ctx.fillText(labelText, badgeLeft + padH, badgeTop + 17);
        ctx.restore();
      }
    }

    // Optional live scanline indicator
    if (typeof scanProgress === 'number') {
      const scanY = (scanProgress % 1) * h;
      const scanGrad = ctx.createLinearGradient(0, scanY - 20, 0, scanY + 4);
      scanGrad.addColorStop(0, 'rgba(16, 185, 129, 0)');
      scanGrad.addColorStop(1, 'rgba(16, 185, 129, 0.35)');
      ctx.fillStyle = scanGrad;
      ctx.fillRect(0, scanY - 20, w, 24);
    }
  }, [
    preset,
    customImage,
    detectionResult,
    maskOpacity,
    showMasks,
    showBoxes,
    showLabels,
    highlightedPartId,
    isMirrored,
    scanProgress,
  ]);

  return (
    <canvas
      ref={canvasRef}
      width={1280}
      height={720}
      data-testid="detection-canvas"
      className="w-full h-full object-contain bg-slate-950 rounded-xl"
    />
  );
};
