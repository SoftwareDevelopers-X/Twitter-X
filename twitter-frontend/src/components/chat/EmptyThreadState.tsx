import React from 'react';
import { Mail } from 'lucide-react';
import './EmptyThreadState.css';

interface EmptyThreadStateProps {
  onNewMessage: () => void;
}

export default function EmptyThreadState({ onNewMessage }: EmptyThreadStateProps) {
  return (
    <div className="empty-thread">
      <div className="empty-thread__icon">
        <Mail size={32} />
      </div>
      <h2>Select a message</h2>
      <p>Choose from your existing conversations, start a new one, or just keep swimming.</p>
      <button className="empty-thread__cta" onClick={onNewMessage}>
        New message
      </button>
    </div>
  );
}
