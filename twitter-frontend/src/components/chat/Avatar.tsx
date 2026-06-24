import React, { useState } from 'react';
import './Avatar.css';

interface AvatarProps {
  user: {
    id: number;
    name: string;
    username: string;
    avatarUrl: string | null;
    color: string;
    initial: string;
  };
  size?: number;
  online?: boolean;
}

export default function Avatar({ user, size = 40, online = false }: AvatarProps) {
  const px = `${size}px`;
  const [hasError, setHasError] = useState(false);

  return (
    <span className="twx-avatar" style={{ width: px, height: px }}>
      {user.avatarUrl && !hasError ? (
        <img 
          src={user.avatarUrl} 
          alt={user.name} 
          onError={() => setHasError(true)} 
        />
      ) : (
        <span
          className="twx-avatar__fallback"
          style={{ background: user.color, fontSize: size * 0.42 }}
        >
          {user.initial}
        </span>
      )}
      {online && <span className="twx-avatar__online-dot" />}
    </span>
  );
}
