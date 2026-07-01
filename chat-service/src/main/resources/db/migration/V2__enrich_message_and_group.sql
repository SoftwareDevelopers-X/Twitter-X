-- Add group image url to conversations
ALTER TABLE conversations ADD COLUMN group_image_url VARCHAR(512);

-- Add edit metadata to messages
ALTER TABLE messages ADD COLUMN edited BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN edited_at TIMESTAMP;

-- Table for Delete for Me
CREATE TABLE user_deleted_messages (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    message_id      BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    deleted_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_message_deleted UNIQUE (user_id, message_id)
);
CREATE INDEX idx_user_deleted_user_id ON user_deleted_messages(user_id);

-- Table for Reactions
CREATE TABLE message_reactions (
    id              BIGSERIAL PRIMARY KEY,
    message_id      BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL,
    reaction        VARCHAR(50) NOT NULL, -- emoji or reaction code
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_user_reaction UNIQUE (message_id, user_id, reaction)
);
CREATE INDEX idx_reactions_message_id ON message_reactions(message_id);

-- Table for User status tracking
CREATE TABLE user_statuses (
    user_id         BIGINT PRIMARY KEY,
    online          BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen       TIMESTAMP NOT NULL DEFAULT now()
);
