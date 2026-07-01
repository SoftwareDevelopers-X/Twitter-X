import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ConversationList from '../components/chat/ConversationList';
import ThreadHeader from '../components/chat/ThreadHeader';
import MessageThread from '../components/chat/MessageThread';
import MessageComposer from '../components/chat/MessageComposer';
import EmptyThreadState from '../components/chat/EmptyThreadState';
import NewMessageModal from '../components/chat/NewMessageModal';
import { useChat } from '../context/ChatContext';
import './Messages.css';

const Messages: React.FC = () => {
  const { conversationId } = useParams<{ conversationId?: string }>();
  const navigate = useNavigate();
  const {
    conversations,
    activeConversationId,
    messages,
    messagesLoading,
    hasMoreMessages,
    typingUserIds,
    openConversation,
    loadOlderMessages,
    sendMessage,
    sendTyping,
    sendMarkRead,
    connectionState,
  } = useChat();

  const [showNewMessage, setShowNewMessage] = useState(false);

  const activeIdNum = conversationId ? Number(conversationId) : null;
  const activeConversation = conversations.find((c) => c.id === activeIdNum);

  useEffect(() => {
    if (activeIdNum && activeIdNum !== activeConversationId) {
      openConversation(activeIdNum);
    }
  }, [activeIdNum, activeConversationId, openConversation]);

  // Mark the last message read whenever the thread updates
  useEffect(() => {
    if (!activeIdNum || messages.length === 0) return;
    const last = messages[messages.length - 1];
    sendMarkRead(activeIdNum, last.id);
  }, [activeIdNum, messages.length, sendMarkRead]);

  const handleSelect = (id: number) => {
    navigate(`/messages/${id}`);
  };

  const handleSend = (content: string, type: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM' = 'TEXT') => {
    if (!activeIdNum) return;
    sendMessage(activeIdNum, content, type);
  };

  const handleTyping = () => {
    if (activeIdNum) sendTyping(activeIdNum);
  };

  const handleNewMessageClose = (newConversationId?: number) => {
    setShowNewMessage(false);
    if (newConversationId) {
      navigate(`/messages/${newConversationId}`);
    }
  };

  return (
    <div className="messages-page">
      <div className={`messages-page__list${activeIdNum ? ' messages-page__list--hidden-mobile' : ''}`}>
        <ConversationList
          activeId={activeIdNum}
          onSelect={handleSelect}
          onNewMessage={() => setShowNewMessage(true)}
        />
      </div>

      <div className={`messages-page__thread${!activeIdNum ? ' messages-page__thread--hidden-mobile' : ''}`}>
        {activeConversation ? (
          <>
            <ThreadHeader conversation={activeConversation} onBack={() => navigate('/messages')} />
            <MessageThread
              messages={messages}
              typingUserIds={typingUserIds}
              hasMore={hasMoreMessages}
              onLoadMore={() => activeIdNum && loadOlderMessages(activeIdNum)}
              loading={messagesLoading}
              isGroup={activeConversation?.type === 'GROUP'}
            />
            <MessageComposer
              onSend={handleSend}
              onTyping={handleTyping}
              disabled={connectionState !== 'connected'}
            />
          </>
        ) : (
          <EmptyThreadState onNewMessage={() => setShowNewMessage(true)} />
        )}
      </div>

      {showNewMessage && <NewMessageModal onClose={handleNewMessageClose} />}
    </div>
  );
};

export default Messages;
