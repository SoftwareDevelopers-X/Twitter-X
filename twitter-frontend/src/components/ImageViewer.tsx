import React, { useState, useEffect } from 'react';
import { X, ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Maximize2 } from 'lucide-react';

const isVideoUrl = (url: string) => {
  if (!url) return false;
  return url.toLowerCase().match(/\.(mp4|webm|ogg|mov|m4v|3gp|avi|mkv)($|\?)/i) || 
         url.includes('/video') || 
         url.includes('video/') ||
         url.startsWith('data:video');
};

interface ImageViewerProps {
  urls: string[];
  initialIndex: number;
  onClose: () => void;
}

const ImageViewer: React.FC<ImageViewerProps> = ({ urls, initialIndex, onClose }) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [isZoomed, setIsZoomed] = useState(false);
  const isCurrentVideo = isVideoUrl(urls[currentIndex]);

  // Handle keyboard events
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft' && urls.length > 1) {
        handlePrev();
      } else if (e.key === 'ArrowRight' && urls.length > 1) {
        handleNext();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentIndex, urls]);

  const handlePrev = () => {
    setIsZoomed(false);
    setCurrentIndex((prev) => (prev === 0 ? urls.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setIsZoomed(false);
    setCurrentIndex((prev) => (prev === urls.length - 1 ? 0 : prev + 1));
  };

  const toggleZoom = () => {
    setIsZoomed((prev) => !prev);
  };

  return (
    <div 
      className="fixed inset-0 bg-black/95 z-[9999] flex items-center justify-center select-none"
      onClick={onClose}
    >
      {/* Top Toolbar */}
      <div className="absolute top-0 left-0 right-0 p-4 flex items-center justify-between z-[110] bg-gradient-to-b from-black/60 to-transparent pointer-events-none">
        <button
          onClick={(e) => {
            e.stopPropagation();
            onClose();
          }}
          className="pointer-events-auto p-2 bg-black/50 hover:bg-neutral-800 text-white rounded-full transition-colors duration-200"
          title="Close"
        >
          <X className="w-6 h-6" />
        </button>

        {/* Zoom & External Link controls */}
        <div className="flex gap-2 pointer-events-auto">
          {!isCurrentVideo && (
            <button
              onClick={toggleZoom}
              className="p-2 bg-black/50 hover:bg-neutral-800 text-white rounded-full transition-colors duration-200"
              title={isZoomed ? "Zoom Out" : "Zoom In"}
            >
              {isZoomed ? <ZoomOut className="w-5 h-5" /> : <ZoomIn className="w-5 h-5" />}
            </button>
          )}

          <a
            href={urls[currentIndex]}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="p-2 bg-black/50 hover:bg-neutral-800 text-white rounded-full transition-colors duration-200"
            title="Open original"
          >
            <Maximize2 className="w-5 h-5" />
          </a>
        </div>
      </div>

      {/* Prev Arrow */}
      {urls.length > 1 && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            handlePrev();
          }}
          className="absolute left-4 p-3 bg-black/50 hover:bg-neutral-800 text-white rounded-full z-[110] transition-colors duration-200"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>
      )}

      {/* Image Container */}
      <div 
        className={`w-full h-full flex items-center justify-center overflow-auto ${
          isCurrentVideo ? '' : (isZoomed ? 'cursor-zoom-out' : 'cursor-zoom-in')
        }`}
        onClick={onClose}
      >
        {isCurrentVideo ? (
          <video
            src={urls[currentIndex]}
            controls
            autoPlay
            onClick={(e) => e.stopPropagation()}
            className="max-w-[90vw] max-h-[90vh] object-contain"
          />
        ) : (
          <img
            src={urls[currentIndex]}
            alt={`Viewer Media ${currentIndex + 1}`}
            onClick={toggleZoom}
            className={`transition-transform duration-200 object-contain max-w-full max-h-full ${
              isZoomed 
                ? 'scale-150 md:scale-200 max-w-none max-h-none my-auto' 
                : 'max-w-[90vw] max-h-[90vh]'
            }`}
          />
        )}
      </div>

      {/* Next Arrow */}
      {urls.length > 1 && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleNext();
          }}
          className="absolute right-4 p-3 bg-black/50 hover:bg-neutral-800 text-white rounded-full z-[110] transition-colors duration-200"
        >
          <ChevronRight className="w-6 h-6" />
        </button>
      )}

      {/* Slide counter footer */}
      {urls.length > 1 && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 px-3 py-1 bg-black/60 text-white text-xs font-semibold rounded-full select-none pointer-events-none">
          {currentIndex + 1} / {urls.length}
        </div>
      )}
    </div>
  );
};

export default ImageViewer;
