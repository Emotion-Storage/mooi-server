-- users.email 을 NULL 허용으로 보정 (Apple 가입 지원)
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

-- 소셜 계정 중복 방지 (soft delete 고려)
ALTER TABLE users
    ADD CONSTRAINT uk_users_social_deleted_at UNIQUE (social_type, social_id, deleted_at);
