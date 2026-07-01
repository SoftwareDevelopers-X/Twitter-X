import React from 'react';
import './EmptyThreadState.css';

interface EmptyThreadStateProps {
  onNewMessage: () => void;
}

const EmptyThreadState: React.FC<EmptyThreadStateProps> = ({ onNewMessage }) => {
  return (
    <div className="empty-state">
      <div className="empty-state__box">
        <h1>Select a message</h1>
        <p>Choose from your existing conversations, start a new one, or just keep swimming.</p>
        <button className="empty-state__btn" onClick={onNewMessage}>
          New message
        </button>
      </div>
    </div>
  );
};

export default EmptyThreadState;
