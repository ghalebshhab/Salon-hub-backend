CREATE TABLE community_post (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    salon_id BIGINT NULL,
    content VARCHAR(5000) NOT NULL,
    media_url VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_community_post_author FOREIGN KEY (author_id) REFERENCES user (id),
    CONSTRAINT fk_community_post_salon FOREIGN KEY (salon_id) REFERENCES salon (id),
    INDEX idx_community_post_created (created_at),
    INDEX idx_community_post_author (author_id),
    INDEX idx_community_post_salon (salon_id)
);

CREATE TABLE community_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_community_comment_post FOREIGN KEY (post_id) REFERENCES community_post (id),
    CONSTRAINT fk_community_comment_author FOREIGN KEY (author_id) REFERENCES user (id),
    INDEX idx_community_comment_post (post_id, created_at)
);

CREATE TABLE community_post_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_community_post_like UNIQUE (post_id, user_id),
    CONSTRAINT fk_community_like_post FOREIGN KEY (post_id) REFERENCES community_post (id),
    CONSTRAINT fk_community_like_user FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE friendship (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    addressee_id BIGINT NOT NULL,
    lower_user_id BIGINT NOT NULL,
    higher_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_friendship_pair UNIQUE (lower_user_id, higher_user_id),
    CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES user (id),
    CONSTRAINT fk_friendship_addressee FOREIGN KEY (addressee_id) REFERENCES user (id),
    INDEX idx_friendship_requester (requester_id, status),
    INDEX idx_friendship_addressee (addressee_id, status)
);

CREATE TABLE salon_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    salon_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_salon_follow UNIQUE (user_id, salon_id),
    CONSTRAINT fk_salon_follow_user FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_salon_follow_salon FOREIGN KEY (salon_id) REFERENCES salon (id)
);
