-- Drop the old constraint if it exists
ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_message_type_check;

-- Add the new constraint with VIDEO message type included
ALTER TABLE messages ADD CONSTRAINT messages_message_type_check CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'SYSTEM'));
