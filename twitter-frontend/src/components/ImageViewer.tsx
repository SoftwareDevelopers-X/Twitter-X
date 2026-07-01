import React, { useState, useEffect } from 'react';
import { X, ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Maximize2 } from 'lucide-react';

<<<<<<< HEAD
=======
const isVideoUrl = (url: string) => {
  if (!url) return false;
  return url.toLowerCase().match(/\.(mp4|webm|ogg|mov|m4v|3gp|avi|mkv)($|\?)/i) || 
         url.includes('/video') || 
         url.includes('video/') ||
         url.startsWith('data:video');
};

>>>>>>> 405d85f (resolved bugs on chat-service)
interface ImageViewerProps {
  urls: string[];
  initialIndex: number;
  onClose: () => void;
}

const ImageViewer: React.FC<ImageViewerProps> = ({ urls, initialIndex, onClose }) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [isZoomed, setIsZoomed] = useState(false);
<<<<<<< HEAD
=======
  const isCurrentVideo = isVideoUrl(urls[currentIndex]);
>>>>>>> 405d85f (resolved bugs on chat-service)

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
    // Disable body scroll when modal is open
    document.body.style.overflow = 'hidden';

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [currentIndex, urls]);

  const handlePrev = () => {
    setIsZoomed(false);
    setCurrentIndex((prev) => (prev === 0 ? urls.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setIsZoomed(false);
    setCurrentIndex((prev) => (prev === urls.length - 1 ? 0 : prev + 1));
  };

  const toggleZoom = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsZoomed(!isZoomed);
  };

  return (
    <div 
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/95 select-none backdrop-blur-sm transition-all duration-300"
      onClick={onClose}
    >
      {/* Top Bar Controls */}
      <div className="absolute top-4 left-4 right-4 flex justify-between items-center z-[110] pointer-events-none">
        {/* Close Button */}
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
<<<<<<< HEAD
          <button
            onClick={toggleZoom}
            className="p-2 bg-black/50 hover:bg-neutral-800 text-white rounded-full transition-colors duration-200"
            title={isZoomed ? "Zoom Out" : "Zoom In"}
          >
            {isZoomed ? <ZoomOut className="w-5 h-5" /> : <ZoomIn className="w-5 h-5" />}
          </button>
=======
          {!isCurrentVideo && (
            <button
              onClick={toggleZoom}
              className="p-2 bg-black/50 hover:bg-neutral-800 text-white rounded-full transition-colors duration-200"
              title={isZoomed ? "Zoom Out" : "Zoom In"}
            >
              {isZoomed ? <ZoomOut className="w-5 h-5" /> : <ZoomIn className="w-5 h-5" />}
            </button>
          )}

>>>>>>> 405d85f (resolved bugs on chat-service)
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
<<<<<<< HEAD
        className={`w-full h-full flex items-center justify-center overflow-auto ${isZoomed ? 'cursor-zoom-out' : 'cursor-zoom-in'}`}
        onClick={onClose}
      >
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
      </div>

=======
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


>>>>>>> 405d85f (resolved bugs on chat-service)
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
