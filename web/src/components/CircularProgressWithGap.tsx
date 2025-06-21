import React from 'react';

interface CircularProgressWithGapProps {
  /** The progress value from 0 to 100 */
  progress: number;
  size?: number;
  /** The thickness of the progress and track lines */
  strokeWidth?: number;
  /** The color of the background track */
  trackColor?: string;
  /** The color of the progress bar */
  progressColor?: string;
  /** The size of the gap at the bottom in degrees (0-360) */
  gapAngle?: number;
  /** Custom React components to render in the center of the circle */
  children?: React.ReactNode;
  /** Custom CSS class for the container */
  className?: string;
}

export const CircularProgressWithGap: React.FC<CircularProgressWithGapProps> = ({
  progress,
  size = 100,
  strokeWidth = 10,
  trackColor = '#e6e6e6',
  progressColor = '#3f51b5',
  gapAngle = 90,
  children,
  className = '',
}) => {
  const clampedProgress = Math.min(Math.max(progress, 0), 100);
  const center = size / 2;
  const radius = center - strokeWidth / 2;
  const circumference = 2 * Math.PI * radius;
  const arcLength = circumference * ((360 - gapAngle) / 360);
  const progressValue = (clampedProgress / 100) * arcLength;
  const rotation = 90 + gapAngle / 2;

  const svgStyle: React.CSSProperties = {
    transform: `rotate(${rotation}deg)`,
  };

  const trackPathStyle: React.CSSProperties = {
    stroke: trackColor,
    strokeDasharray: `${arcLength} ${circumference}`,
  };

  const progressPathStyle: React.CSSProperties = {
    stroke: progressColor,
    strokeDasharray: `${progressValue} ${circumference}`,
    transition: 'stroke-dasharray 0.35s ease-out',
  };

  return (
    <div
      className={`circular-progress-container ${className}`}
      style={{ width: size, height: size, position: 'relative', pointerEvents: 'none' }}
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={svgStyle}>
        {/* Background Track */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          style={trackPathStyle}
        />
        {/* Progress Indicator */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          style={progressPathStyle}
        />
      </svg>
      {children && (
        <div
          className="circular-progress-content"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center'
          }}
        >
          {children}
        </div>
      )}
    </div>
  );
};
