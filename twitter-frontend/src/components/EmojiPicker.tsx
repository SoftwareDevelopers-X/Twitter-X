import React, { useState, useEffect, useRef } from 'react';

interface EmojiPickerProps {
  onSelect: (emoji: string) => void;
  onClose: () => void;
}

const EMOJI_CATEGORIES = [
  {
    name: 'Smileys',
    emojis: [
      '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚',
      '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣',
      '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓', '🤗',
      '🤔', '🤭', '🤫', '🤥', '😶', '😐', '😑', '😬', '🙄', '😯', '😦', '😧', '😮', '😲', '🥱', '😴', '🤤', '😪', '😵', '🤐'
    ]
  },
  {
    name: 'Gestures',
    emojis: [
      '👋', '🤚', '🖐️', '✋', '🖖', '👌', '🤏', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '👍', '👎',
      '✊', '👊', '🤛', '🤜', '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️', '💅', '🤳', '💪', '🦾', '🦵', '🦶', '👂', '👃', '🧠'
    ]
  },
  {
    name: 'Hearts',
    emojis: [
      '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '💌'
    ]
  },
  {
    name: 'Food',
    emojis: [
      '🍏', '🍎', '🍐', '🧡', '🍋', '🍌', '🍉', '🍇', '🍓', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🍆', '🥑', '🥦', '🥬',
      '🥒', '🌶️', '🌽', '🥕', '🥔', '🍠', '🥐', '🥯', '🍞', '🥖', '🥨', '🧀', '🍳', '🥓', '🥩', '🍗', '🍖', '🌭', '🍔', '🍟',
      '🍕', '🌮', '🌯', '🥗', '🍿', '🥫', '🍱', '🍣', '🍤', '🍨', '🍩', '🍪', '🎂', '🍫', '🍬', '🍭', '☕', '🍺', '🍷', '🍹'
    ]
  },
  {
    name: 'Activities',
    emojis: [
      '⚽', '🏀', '🏈', '⚾', '🥎', '🎾', '🏐', '🎱', '🪀', '🏓', '🏸', '🏒', '⛳', '🏹', '🎣', '🤿', '🥊', '🥋', '🛹', '⛷️',
      '🏆', '🥇', '🥈', '🥉', '🏅', '🎟️', '🎬', '🎤', '🎧', '🎼', '🎹', '🥁', '🎸', '🎲', '🧩', '🎯', '🎮', '🏎️', '🏍️', '🚀'
    ]
  }
];

const EmojiPicker: React.FC<EmojiPickerProps> = ({ onSelect, onClose }) => {
  const [activeCategory, setActiveCategory] = useState('Smileys');
  const pickerRef = useRef<HTMLDivElement>(null);

  // Click outside to close handler
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handleClickOutside, true);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside, true);
    };
  }, [onClose]);

  const emojis = EMOJI_CATEGORIES.find(c => c.name === activeCategory)?.emojis || [];

  return (
    <div 
      ref={pickerRef}
      className="absolute bottom-12 left-0 mt-2 w-72 bg-black border border-twitter-dark-4 rounded-2xl shadow-2xl z-50 flex flex-col overflow-hidden text-left"
      onClick={(e) => e.stopPropagation()}
    >
      {/* Category Tabs */}
      <div className="flex border-b border-twitter-dark-4 bg-twitter-dark-2/50 text-xs font-semibold overflow-x-auto scrollbar-none">
        {EMOJI_CATEGORIES.map((cat) => (
          <button
            key={cat.name}
            type="button"
            onClick={() => setActiveCategory(cat.name)}
            className={`px-3 py-2.5 transition-colors whitespace-nowrap ${
              activeCategory === cat.name
                ? 'text-twitter-blue border-b-2 border-twitter-blue'
                : 'text-twitter-gray-1 hover:text-white'
            }`}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {/* Emoji Grid */}
      <div className="p-3 max-h-56 overflow-y-auto grid grid-cols-8 gap-1.5 justify-items-center bg-black">
        {emojis.map((emoji, index) => (
          <button
            key={index}
            type="button"
            onClick={() => onSelect(emoji)}
            className="text-xl p-1.5 hover:bg-neutral-800 rounded-lg active:scale-90 transition-transform duration-75 flex items-center justify-center w-8 h-8"
          >
            {emoji}
          </button>
        ))}
      </div>
    </div>
  );
};

export default EmojiPicker;
