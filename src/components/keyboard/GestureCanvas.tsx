import React, { useRef, useEffect } from 'react';

interface Point {
  x: number;
  y: number;
  time: number;
}

interface GestureCanvasProps {
  points: Point[];
  trailColor: string;
  trailWidth: number;
  fadeDuration: number;
}

export const GestureCanvas: React.FC<GestureCanvasProps> = ({
  points,
  trailColor,
  trailWidth,
  fadeDuration
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Handle canvas resolution
    const rect = canvas.getBoundingClientRect();
    if (canvas.width !== rect.width || canvas.height !== rect.height) {
      canvas.width = rect.width;
      canvas.height = rect.height;
    }

    let animationFrameId: number;

    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      if (points.length < 2) return;

      const now = Date.now();
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';

      for (let i = 1; i < points.length; i++) {
        const p1 = points[i - 1];
        const p2 = points[i];
        const age = now - p2.time;

        if (age < fadeDuration) {
          const progress = 1 - age / fadeDuration;
          const alpha = Math.max(0, Math.min(1, progress));
          const currentWidth = Math.max(1, trailWidth * progress);

          ctx.beginPath();
          ctx.moveTo(p1.x, p1.y);
          ctx.lineTo(p2.x, p2.y);
          ctx.strokeStyle = trailColor;
          ctx.globalAlpha = alpha * 0.85;
          ctx.lineWidth = currentWidth;
          ctx.stroke();
        }
      }

      ctx.globalAlpha = 1.0;

      // Draw active glowing head particle
      const head = points[points.length - 1];
      if (head && (now - head.time) < fadeDuration) {
        ctx.beginPath();
        ctx.arc(head.x, head.y, trailWidth * 1.5, 0, Math.PI * 2);
        ctx.fillStyle = trailColor;
        ctx.shadowColor = trailColor;
        ctx.shadowBlur = 10;
        ctx.fill();
        ctx.shadowBlur = 0;
      }

      // Continue animating if recent points exist
      if (points.length > 0 && (now - points[points.length - 1].time) < fadeDuration) {
        animationFrameId = requestAnimationFrame(render);
      }
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [points, trailColor, trailWidth, fadeDuration]);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 pointer-events-none z-30 w-full h-full"
    />
  );
};
