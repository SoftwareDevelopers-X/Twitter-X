import React from 'react';
import './Spinner.css';

interface SpinnerProps {
  size?: number;
}

export default function Spinner({ size = 28 }: SpinnerProps) {
  return (
    <span className="twx-spinner-wrap" role="status" aria-label="Loading">
      <svg
        className="twx-spinner"
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="var(--twitter-blue, #1d9bf0)"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray="50 100"
        />
      </svg>
    </span>
  );
}
