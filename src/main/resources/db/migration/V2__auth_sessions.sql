-- Server-side sign-in sessions: durable logout and rotating refresh tokens.

CREATE TABLE IF NOT EXISTS `auth_sessions` (
  `id` varchar(36) NOT NULL,
  `client` enum('ADMIN','WEB') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `last_used_at` datetime(6) NOT NULL,
  `previous_refresh_token_hash` varchar(64) DEFAULT NULL,
  `refresh_token_hash` varchar(64) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `version` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqwxgu6607bn597i2g2ivyp3gu` (`refresh_token_hash`),
  KEY `idx_auth_session_user` (`user_id`),
  KEY `idx_auth_session_expires` (`expires_at`),
  CONSTRAINT `FK8pqghqmogk8eg0omovmh4ci9o` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
