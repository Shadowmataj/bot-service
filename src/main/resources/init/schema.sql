CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

CREATE TABLE IF NOT EXISTS vector_store (
                                            id TEXT PRIMARY KEY, -- id should be TEXT (not UUID type)
                                            content TEXT,
                                            metadata JSONB,
                                            embedding VECTOR(1536)
    );

-- Create HNSW index for fast search
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);

-- Chat Memory Tables for persistent conversation storage
CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    current_state VARCHAR(50) DEFAULT 'INITIAL',
    context_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM, TOOL
    content TEXT NOT NULL,
    metadata JSONB,
    message_order INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation FOREIGN KEY (conversation_id) 
        REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_chat_conversations_phone ON chat_conversations(phone_number);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_state ON chat_conversations(current_state);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation ON chat_messages(conversation_id, message_order);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created ON chat_messages(created_at); 