import React, { useState } from 'react';
import './Avatar.css';

interface AvatarProps {
  user?: {
    avatarUrl?: string | null;
    name?: string;
    username?: string;
    color?: string;
    initial?: string;
  } | null;
  size?: number;
  online?: boolean;
}

const Avatar: React.FC<AvatarProps> = ({ user, size = 40, online = false }) => {
  const px = `${size}px`;
  
  const [avatarError, setAvatarError] = useState(false);
  const [dicebearError, setDicebearError] = useState(false);

  const username = user?.username || 'user';
  const name = user?.name || 'User';
  const avatarUrl = user?.avatarUrl;
  const color = user?.color || '#1d9bf0';
  const initial = user?.initial || (name ? name.charAt(0).toUpperCase() : '?');

  const dicebearUrl = `https://api.dicebear.com/7.x/adventurer/svg?seed=${username}`;

  return (
    <span className="twx-avatar" style={{ width: px, height: px }}>
      {avatarUrl && !avatarError ? (
        <img 
          src={avatarUrl} 
          alt={name} 
          onError={() => setAvatarError(true)} 
        />
      ) : !dicebearError ? (
        <img 
          src={dicebearUrl} 
          alt={name} 
          onError={() => setDicebearError(true)} 
        />
      ) : (
        <span
          className="twx-avatar__fallback font-bold"
          style={{ 
            backgroundColor: color || '#1d9bf0', 
            fontSize: size * 0.42 
          }}
        >
          {initial}
        </span>
      )}
      {online && <span className="twx-avatar__online-dot" />}
    </span>
  );
};

export default Avatar;
