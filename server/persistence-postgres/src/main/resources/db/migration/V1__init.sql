CREATE TABLE users (
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO users (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'default');

CREATE TABLE chats (
    id               UUID PRIMARY KEY,
    title            TEXT NOT NULL,
    creator_id       UUID NOT NULL REFERENCES users(id),
    system_prompt    TEXT NOT NULL DEFAULT '',
    active_leaf_id   UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
CREATE INDEX idx_chats_creator ON chats(creator_id, updated_at DESC);

CREATE TABLE message_nodes (
    id                 UUID PRIMARY KEY,
    conversation_id    UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    parent_id          UUID REFERENCES message_nodes(id) ON DELETE CASCADE,
    role               TEXT NOT NULL CHECK (role IN ('system', 'user', 'assistant')),
    content            TEXT NOT NULL,
    status             TEXT NOT NULL DEFAULT 'done'
       CHECK (status IN ('generating', 'done', 'failed', 'cancelled')),
    model              TEXT,
    params             JSONB,
    prompt_tokens      INT,
    completion_tokens  INT,
    latency_ms         INT,
    finish_reason      TEXT,
    error              TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_nodes_parent ON message_nodes(parent_id);
CREATE INDEX idx_nodes_chat   ON message_nodes(conversation_id, created_at);
CREATE INDEX idx_nodes_status ON message_nodes(status) WHERE status = 'generating';

ALTER TABLE chats
    ADD CONSTRAINT fk_chats_active_leaf
    FOREIGN KEY (active_leaf_id) REFERENCES message_nodes(id) ON DELETE SET NULL;

CREATE TABLE attachments (
    id            UUID PRIMARY KEY,
    node_id       UUID NOT NULL REFERENCES message_nodes(id) ON DELETE CASCADE,
    content_type  TEXT NOT NULL,
    location      TEXT NOT NULL,
    size_bytes    BIGINT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_node ON attachments(node_id);

CREATE TABLE chat_collaborators (
    chat_id  UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access   TEXT NOT NULL,
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE model_presets (
    id             UUID PRIMARY KEY,
    user_id        UUID REFERENCES users(id),
    name           TEXT NOT NULL,
    model          TEXT NOT NULL,
    system_prompt  TEXT NOT NULL DEFAULT '',
    params         JSONB NOT NULL DEFAULT '{}'
);