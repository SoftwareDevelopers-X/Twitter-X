import React from 'react';
import { Loader2 } from 'lucide-react';

interface SpinnerProps {
  size?: number;
}

const Spinner: React.FC<SpinnerProps> = ({ size = 20 }) => {
  return (
    <div className="flex items-center justify-center p-2">
      <Loader2 className="animate-spin text-twitter-blue" size={size} />
    </div>
  );
};

export default Spinner;
