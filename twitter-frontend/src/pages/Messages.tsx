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

export default function Messages() {
  const { conversationId } = useParams();
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

  function handleSelect(id: number) {
    navigate(`/messages/${id}`);
  }

  function handleSend(content: string, messageType?: string) {
    if (!activeIdNum) return;
    sendMessage(activeIdNum, content, messageType);
  }

  function handleTyping() {
    if (activeIdNum) sendTyping(activeIdNum);
  }

  function handleNewMessageClose(newConversationId?: number) {
    setShowNewMessage(false);
    if (newConversationId) navigate(`/messages/${newConversationId}`);
  }

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
              onLoadMore={() => loadOlderMessages(activeIdNum!)}
              loading={messagesLoading}
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
}
