-- SalonHub baseline schema (version 1).
-- Generated from the schema Hibernate managed before Flyway was adopted.
-- An existing database is marked as version 1 (spring.flyway.baseline-on-migrate)
-- and skips this file; an empty database is created from it.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `auth_verification_codes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code_hash` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `failed_attempts` int NOT NULL,
  `purpose` enum('EMAIL_VERIFICATION','PASSWORD_RESET') NOT NULL,
  `resend_available_at` datetime(6) NOT NULL,
  `reset_token_expires_at` datetime(6) DEFAULT NULL,
  `reset_token_hash` varchar(64) DEFAULT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_code_user_purpose` (`user_id`,`purpose`),
  UNIQUE KEY `UKanr7fg2aytphxcixlar04xws3` (`reset_token_hash`),
  CONSTRAINT `FKgjmtipy82rb9df8t0plau6tvc` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_fils` bigint NOT NULL,
  `commission_fils` bigint NOT NULL,
  `commission_rate_bps` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `method` varchar(20) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `receipt_number` varchar(40) DEFAULT NULL,
  `refunded_fils` bigint NOT NULL,
  `status` varchar(20) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `confirmed_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkdytmty85vlkj7607nygkxxs1` (`receipt_number`),
  KEY `idx_booking_payment_status_paid` (`status`,`paid_at`),
  KEY `FKq8w6xm10fbcvbjyy6mut40kj7` (`confirmed_by_id`),
  CONSTRAINT `FKq8w6xm10fbcvbjyy6mut40kj7` FOREIGN KEY (`confirmed_by_id`) REFERENCES `user` (`id`),
  CONSTRAINT `booking_payment_chk_1` CHECK ((`method` in (_utf8mb4'CASH',_utf8mb4'CARD_PREPAID',_utf8mb4'CARD_POSTPAID'))),
  CONSTRAINT `booking_payment_chk_2` CHECK ((`status` in (_utf8mb4'UNPAID',_utf8mb4'AUTHORIZED',_utf8mb4'PAID',_utf8mb4'PARTIALLY_REFUNDED',_utf8mb4'REFUNDED',_utf8mb4'VOIDED',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_reschedule_proposal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `proposed_date` date NOT NULL,
  `proposed_time` time NOT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','CANCELLED','PENDING','REJECTED') NOT NULL,
  `version` bigint NOT NULL,
  `booking_id` bigint NOT NULL,
  `proposed_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reschedule_booking_created` (`booking_id`,`created_at`),
  KEY `idx_reschedule_booking_status` (`booking_id`,`status`),
  KEY `FKta00qsteejp3yu2bf1s3jr64f` (`proposed_by_id`),
  CONSTRAINT `FKcp6n9xyunt0xdf3lkq8m9m2hu` FOREIGN KEY (`booking_id`) REFERENCES `salon_booking` (`id`),
  CONSTRAINT `FKta00qsteejp3yu2bf1s3jr64f` FOREIGN KEY (`proposed_by_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_service_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `duration_minutes` int NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `service_name` varchar(100) NOT NULL,
  `source_service_id` bigint NOT NULL,
  `booking_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8yyj2aq3rcc0h10umki3gr5u1` (`booking_id`),
  CONSTRAINT `FK8yyj2aq3rcc0h10umki3gr5u1` FOREIGN KEY (`booking_id`) REFERENCES `salon_booking` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(2000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `author_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_community_comment_post` (`post_id`,`created_at`),
  KEY `FKc9uwylbfjnaqp2kjrdl5bu3na` (`author_id`),
  CONSTRAINT `FKc9uwylbfjnaqp2kjrdl5bu3na` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKrve078nctpgytx54f5jp98po3` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(5000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `media_url` varchar(1000) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `author_id` bigint NOT NULL,
  `salon_id` bigint DEFAULT NULL,
  `media_public_id` varchar(255) DEFAULT NULL,
  `media_resource_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_community_post_created` (`created_at`),
  KEY `idx_community_post_author` (`author_id`),
  KEY `idx_community_post_salon` (`salon_id`),
  CONSTRAINT `FK3ian8urh5ex8dc0p5dn4sjh2f` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKk8q5ajm5s9l6832sfkmugk6g5` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_community_post_like` (`post_id`,`user_id`),
  KEY `FK1k3gkv5pyhk8o09624rw2jhxv` (`user_id`),
  CONSTRAINT `FK1k3gkv5pyhk8o09624rw2jhxv` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK8siu16g2986e10u789c4rp89g` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `employee_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `employee_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_schedule_employee` (`employee_id`),
  CONSTRAINT `FKsk8qrhwh708ywaj9y6gr8mamk` FOREIGN KEY (`employee_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `employee_service_capability` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) DEFAULT NULL,
  `employee_id` bigint NOT NULL,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_service_capability` (`employee_id`,`service_id`),
  KEY `idx_capability_employee` (`employee_id`),
  KEY `idx_capability_service` (`service_id`),
  CONSTRAINT `FK4bviv9nnn5ge52x7djqf107s5` FOREIGN KEY (`employee_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKdnfippadj5mc6sj08t5t6idqm` FOREIGN KEY (`service_id`) REFERENCES `salon_service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `employee_time_off` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `start_date` date NOT NULL,
  `employee_id` bigint NOT NULL,
  `salon_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_time_off_employee_dates` (`employee_id`,`start_date`,`end_date`),
  KEY `FKnsvpee8e3dhhvo05j8qjy60w6` (`salon_id`),
  CONSTRAINT `FKfnqe1t85823v40p2h8m5bwhew` FOREIGN KEY (`employee_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKnsvpee8e3dhhvo05j8qjy60w6` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `employee_work_media` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bytes` bigint DEFAULT NULL,
  `caption` varchar(300) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `height` int DEFAULT NULL,
  `public_id` varchar(255) NOT NULL,
  `resource_type` enum('IMAGE','VIDEO') NOT NULL,
  `secure_url` varchar(1000) NOT NULL,
  `width` int DEFAULT NULL,
  `employee_id` bigint NOT NULL,
  `salon_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKf294uf3towam0y1u66em5nk4i` (`employee_id`),
  KEY `FK7ri9mueylrfay5yvp5dw965g7` (`salon_id`),
  CONSTRAINT `FK7ri9mueylrfay5yvp5dw965g7` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`),
  CONSTRAINT `FKf294uf3towam0y1u66em5nk4i` FOREIGN KEY (`employee_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `employee_working_period` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,
  `end_time` time NOT NULL,
  `start_time` time NOT NULL,
  `schedule_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_working_period_schedule_day` (`schedule_id`,`day_of_week`),
  CONSTRAINT `FK1mromlu6gnpgicv51y43hgirv` FOREIGN KEY (`schedule_id`) REFERENCES `employee_schedule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `employment_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `request_type` enum('Owner_Invite','User_Request') DEFAULT NULL,
  `status` enum('Accepted','Rejected','Requested') DEFAULT NULL,
  `receiver_id` bigint DEFAULT NULL,
  `sender_id` bigint DEFAULT NULL,
  `salon_id` bigint DEFAULT NULL,
  `rejection_reason` varchar(255) DEFAULT NULL,
  `request_source` enum('HIRING_POST','SALON_PROFILE') DEFAULT NULL,
  `hiring_post_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKeiy6mw7ahtuv03m8v6b33y23e` (`receiver_id`),
  KEY `FKafyjm1xx02kyxaaj7a7tasqb7` (`sender_id`),
  KEY `FK2xl7a0gui10w2i7gq3ijfkye5` (`salon_id`),
  KEY `FKb77wrki9cdnfg0b26hnhdajwc` (`hiring_post_id`),
  CONSTRAINT `FK2xl7a0gui10w2i7gq3ijfkye5` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`),
  CONSTRAINT `FKafyjm1xx02kyxaaj7a7tasqb7` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKb77wrki9cdnfg0b26hnhdajwc` FOREIGN KEY (`hiring_post_id`) REFERENCES `hiring_post` (`id`),
  CONSTRAINT `FKeiy6mw7ahtuv03m8v6b33y23e` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `friendship` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `higher_user_id` bigint NOT NULL,
  `lower_user_id` bigint NOT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','PENDING') NOT NULL,
  `addressee_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friendship_pair` (`lower_user_id`,`higher_user_id`),
  KEY `idx_friendship_requester` (`requester_id`,`status`),
  KEY `idx_friendship_addressee` (`addressee_id`,`status`),
  CONSTRAINT `FK1lq9fd9brkgunpea7e8jferpm` FOREIGN KEY (`addressee_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKs6abgeydylxt8br7f3ttlk2q5` FOREIGN KEY (`requester_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hiring_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `application_note` varchar(2000) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `current_location` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `portfolio_url` varchar(255) DEFAULT NULL,
  `resume_url` varchar(255) DEFAULT NULL,
  `years_of_experience` int DEFAULT NULL,
  `applicant_id` bigint NOT NULL,
  `employment_request_id` bigint NOT NULL,
  `hiring_post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hiring_post_applicant` (`hiring_post_id`,`applicant_id`),
  UNIQUE KEY `UK7o98cpcuub6auu91dxyi0f864` (`employment_request_id`),
  KEY `FKd2fys94bjxxq3653avagh02k1` (`applicant_id`),
  CONSTRAINT `FK84o8lwcxp5ubumg7obyrj299t` FOREIGN KEY (`hiring_post_id`) REFERENCES `hiring_post` (`id`),
  CONSTRAINT `FKd2fys94bjxxq3653avagh02k1` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKgbokp43uw5exylirve9ltsy78` FOREIGN KEY (`employment_request_id`) REFERENCES `employment_request` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hiring_application_preferred_locations` (
  `hiring_application_id` bigint NOT NULL,
  `location` varchar(255) NOT NULL,
  KEY `FK5lb6x8j3k8sh6o1je0sckufjv` (`hiring_application_id`),
  CONSTRAINT `FK5lb6x8j3k8sh6o1je0sckufjv` FOREIGN KEY (`hiring_application_id`) REFERENCES `hiring_application` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hiring_application_skills` (
  `hiring_application_id` bigint NOT NULL,
  `skill` varchar(255) NOT NULL,
  KEY `FKr9rrmpyh21kmqsf7wpe26poap` (`hiring_application_id`),
  CONSTRAINT `FKr9rrmpyh21kmqsf7wpe26poap` FOREIGN KEY (`hiring_application_id`) REFERENCES `hiring_application` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hiring_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `num_of_positions` int DEFAULT NULL,
  `status` enum('Closed','Expired','Open') DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `salon_id` bigint NOT NULL,
  `employee_location` varchar(255) DEFAULT NULL,
  `maximum_age` int DEFAULT NULL,
  `minimum_age` int DEFAULT NULL,
  `minimum_years_of_experience` int DEFAULT NULL,
  `closure_reason` enum('MANUALLY_CLOSED','POSITIONS_FILLED') DEFAULT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKo6606ernijvgkjj662ychixgs` (`salon_id`),
  CONSTRAINT `FKo6606ernijvgkjj662ychixgs` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hiring_post_required_skills` (
  `hiring_post_id` bigint NOT NULL,
  `skill` varchar(255) NOT NULL,
  KEY `FK6lixb5bbfsf15a7ukuau8n91d` (`hiring_post_id`),
  CONSTRAINT `FK6lixb5bbfsf15a7ukuau8n91d` FOREIGN KEY (`hiring_post_id`) REFERENCES `hiring_post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `leave_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` enum('Accepted','Cancelled','Requested') DEFAULT NULL,
  `current_salon_id` bigint DEFAULT NULL,
  `employee_id` bigint DEFAULT NULL,
  `employment_request_id` bigint DEFAULT NULL,
  `target_salon_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKki9xgy5y8qc8338sxc9jktcyh` (`employment_request_id`),
  KEY `FKoflqhq1ojop4t9uhelj9nij6x` (`current_salon_id`),
  KEY `FKlyxpiaxo856pl1ehq1x13kic6` (`employee_id`),
  KEY `FKtmbreujhgtsgufgegofyp56mn` (`target_salon_id`),
  CONSTRAINT `FK36n2i9xp4ipiyx6mjfwp1275w` FOREIGN KEY (`employment_request_id`) REFERENCES `employment_request` (`id`),
  CONSTRAINT `FKlyxpiaxo856pl1ehq1x13kic6` FOREIGN KEY (`employee_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKoflqhq1ojop4t9uhelj9nij6x` FOREIGN KEY (`current_salon_id`) REFERENCES `salon` (`id`),
  CONSTRAINT `FKtmbreujhgtsgufgegofyp56mn` FOREIGN KEY (`target_salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `moderation_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_note` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `details` varchar(1000) DEFAULT NULL,
  `reason` enum('HARASSMENT','IMPERSONATION','INAPPROPRIATE_CONTENT','OTHER','SCAM','SPAM') NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` enum('DISMISSED','OPEN','RESOLVED') NOT NULL,
  `target_id` bigint NOT NULL,
  `target_label` varchar(255) NOT NULL,
  `target_type` enum('COMMENT','POST','SALON','USER') NOT NULL,
  `reporter_id` bigint NOT NULL,
  `reviewed_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_moderation_report_status_created` (`status`,`created_at`),
  KEY `idx_moderation_report_target` (`target_type`,`target_id`),
  KEY `FKkj5pxdp5qs78220drxe7jhmeb` (`reporter_id`),
  KEY `FKk6s5gdxtg8m5iitulett0m6kf` (`reviewed_by_id`),
  CONSTRAINT `FKk6s5gdxtg8m5iitulett0m6kf` FOREIGN KEY (`reviewed_by_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKkj5pxdp5qs78220drxe7jhmeb` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_fils` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `idempotency_key` varchar(80) NOT NULL,
  `note` varchar(255) DEFAULT NULL,
  `provider_reference` varchar(120) DEFAULT NULL,
  `type` varchar(20) NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `payment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK48aiw2plynjrh8d5wu2laxjij` (`idempotency_key`),
  KEY `FK3ns70r4ginjy9dgs49ps920p9` (`actor_id`),
  KEY `FK9f3y8bh74ojr5gatt63y65d75` (`payment_id`),
  CONSTRAINT `FK3ns70r4ginjy9dgs49ps920p9` FOREIGN KEY (`actor_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK9f3y8bh74ojr5gatt63y65d75` FOREIGN KEY (`payment_id`) REFERENCES `booking_payment` (`id`),
  CONSTRAINT `payment_transaction_chk_1` CHECK ((`type` in (_utf8mb4'CASH_RECEIVED',_utf8mb4'AUTHORIZE',_utf8mb4'CAPTURE',_utf8mb4'VOID',_utf8mb4'REFUND',_utf8mb4'METHOD_CHANGED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `closing_time` time DEFAULT NULL,
  `created_at` date DEFAULT NULL,
  `current_employees_number` bigint DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `max_employees_number` bigint DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `opening_time` time DEFAULT NULL,
  `owner_phone_number` varchar(255) DEFAULT NULL,
  `profile_picture_url` varchar(255) DEFAULT NULL,
  `salon_phone_number` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `openning_status` tinyint DEFAULT NULL,
  `version` bigint NOT NULL,
  `blocked_by_admin` bit(1) NOT NULL,
  `blocked_by_owner` bit(1) NOT NULL,
  `owner_role_before_admin_block` enum('ADMIN','Employee','OWNER','USER') DEFAULT NULL,
  `rejection_reason` varchar(1000) DEFAULT NULL,
  `resubmission_count` int NOT NULL,
  `status_before_block` enum('Accepted','DELETED','Inactive','Rejected','Requested') DEFAULT NULL,
  `google_maps_url` varchar(1000) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `profile_picture_public_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr3sppqb8v1i9rnufxxfmxktne` (`owner_id`),
  CONSTRAINT `FKe7wrhm5jt4s11aws5aauxqu98` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`),
  CONSTRAINT `salon_chk_1` CHECK ((`status` between 0 and 6)),
  CONSTRAINT `salon_chk_2` CHECK ((`openning_status` between 0 and 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_booking` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_date` date NOT NULL,
  `booking_time` time NOT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `customer_location` varchar(255) NOT NULL,
  `customer_phone_number` varchar(30) NOT NULL,
  `decided_at` datetime(6) DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `owner_note` varchar(1000) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `total_duration_minutes` int NOT NULL,
  `total_price` decimal(12,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `assigned_employee_id` bigint DEFAULT NULL,
  `customer_id` bigint NOT NULL,
  `salon_id` bigint NOT NULL,
  `arrived_at` datetime(6) DEFAULT NULL,
  `customer_latitude` double DEFAULT NULL,
  `customer_location_updated_at` datetime(6) DEFAULT NULL,
  `customer_longitude` double DEFAULT NULL,
  `distance_meters` int DEFAULT NULL,
  `eta_minutes` int DEFAULT NULL,
  `expected_end_at` datetime(6) DEFAULT NULL,
  `journey_started_at` datetime(6) DEFAULT NULL,
  `journey_status` varchar(20) DEFAULT NULL,
  `service_started_at` datetime(6) DEFAULT NULL,
  `no_show_at` datetime(6) DEFAULT NULL,
  `payment_id` bigint DEFAULT NULL,
  `customer_confirmed_at` datetime(6) DEFAULT NULL,
  `plan_latitude` double DEFAULT NULL,
  `plan_longitude` double DEFAULT NULL,
  `plan_updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKi7ekbj9vtmq8c8q1et95eh3l4` (`payment_id`),
  KEY `idx_booking_employee_date_status` (`assigned_employee_id`,`booking_date`,`status`),
  KEY `FKa7feu9cftb0mwi0sq01gkknm5` (`customer_id`),
  KEY `idx_booking_salon_date_status` (`salon_id`,`booking_date`,`status`),
  CONSTRAINT `FK82klcrr9duwynm00ybh3fcagh` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`),
  CONSTRAINT `FK892pit7tak3ptx3b4rxvd6rvd` FOREIGN KEY (`assigned_employee_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK9ypbsbk93ldlsy953b3xipy06` FOREIGN KEY (`payment_id`) REFERENCES `booking_payment` (`id`),
  CONSTRAINT `FKa7feu9cftb0mwi0sq01gkknm5` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `salon_booking_chk_1` CHECK ((`journey_status` in (_utf8mb4'NOT_STARTED',_utf8mb4'ON_THE_WAY',_utf8mb4'ARRIVED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_closure` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `start_date` date NOT NULL,
  `salon_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_salon_closure_salon_dates` (`salon_id`,`start_date`,`end_date`),
  CONSTRAINT `FK8w9rsk047cq0k80lkcqjru0ja` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `salon_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_salon_follow` (`user_id`,`salon_id`),
  KEY `FKrd7ns9hv0jr1b5r8q9k4yakiy` (`salon_id`),
  CONSTRAINT `FKoyu2vtfibkl10hffxa3yffrn` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKrd7ns9hv0jr1b5r8q9k4yakiy` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_payment_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_postpaid_enabled` bit(1) NOT NULL,
  `card_prepaid_enabled` bit(1) NOT NULL,
  `cash_enabled` bit(1) NOT NULL,
  `free_cancel_hours` int NOT NULL,
  `late_cancel_fee_percent` int NOT NULL,
  `no_show_fee_percent` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `salon_id` bigint NOT NULL,
  `no_show_limit` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKrap5tfjxts37w5vud1aar0k8v` (`salon_id`),
  CONSTRAINT `FK4jv43s7ri4aprdqr5iac37wek` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` varchar(2000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `rating` int NOT NULL,
  `booking_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `salon_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_salon_review_booking` (`booking_id`),
  KEY `FK15g1cwcsf2wcnwius81ub6q1q` (`customer_id`),
  KEY `FKf5qj8q34rju2x62jb9761li9d` (`salon_id`),
  CONSTRAINT `FK15g1cwcsf2wcnwius81ub6q1q` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKccv1eh3ql1vhk6agk6e12yye6` FOREIGN KEY (`booking_id`) REFERENCES `salon_booking` (`id`),
  CONSTRAINT `FKf5qj8q34rju2x62jb9761li9d` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_service` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `duration_minutes` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `salon_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK909bg90y6veilfoo49c07ow1h` (`salon_id`),
  CONSTRAINT `FK909bg90y6veilfoo49c07ow1h` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `salon_subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `billing_cycle` varchar(20) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_period_end` datetime(6) DEFAULT NULL,
  `current_period_start` datetime(6) DEFAULT NULL,
  `grace_ends_at` datetime(6) DEFAULT NULL,
  `last_notice_key` varchar(60) DEFAULT NULL,
  `plan` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `trial_ends_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `salon_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1h6bul8o6i3hrrc25db9smd04` (`salon_id`),
  KEY `idx_subscription_status` (`status`),
  CONSTRAINT `FKs38ww5myefsb2she379v609lw` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`),
  CONSTRAINT `salon_subscription_chk_1` CHECK ((`billing_cycle` in (_utf8mb4'MONTHLY',_utf8mb4'YEARLY'))),
  CONSTRAINT `salon_subscription_chk_2` CHECK ((`plan` in (_utf8mb4'FREE',_utf8mb4'PRO',_utf8mb4'BUSINESS'))),
  CONSTRAINT `salon_subscription_chk_3` CHECK ((`status` in (_utf8mb4'TRIAL',_utf8mb4'ACTIVE',_utf8mb4'GRACE',_utf8mb4'ENDED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `subscription_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_fils` bigint NOT NULL,
  `billing_cycle` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `method` varchar(20) NOT NULL,
  `period_end` datetime(6) DEFAULT NULL,
  `period_start` datetime(6) DEFAULT NULL,
  `plan` varchar(20) NOT NULL,
  `reference` varchar(120) DEFAULT NULL,
  `rejection_reason` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `requested_by_id` bigint DEFAULT NULL,
  `reviewed_by_id` bigint DEFAULT NULL,
  `subscription_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_subscription_payment_status` (`status`,`created_at`),
  KEY `idx_subscription_payment_subscription` (`subscription_id`),
  KEY `FKsu68d05t0491s4xc1bexh68di` (`requested_by_id`),
  KEY `FK5m0e8jt8f9xdnm390xl8msmvf` (`reviewed_by_id`),
  CONSTRAINT `FK5m0e8jt8f9xdnm390xl8msmvf` FOREIGN KEY (`reviewed_by_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKieqnu2sr4k7m38wfvtr4dfhj3` FOREIGN KEY (`subscription_id`) REFERENCES `salon_subscription` (`id`),
  CONSTRAINT `FKsu68d05t0491s4xc1bexh68di` FOREIGN KEY (`requested_by_id`) REFERENCES `user` (`id`),
  CONSTRAINT `subscription_payment_chk_1` CHECK ((`billing_cycle` in (_utf8mb4'MONTHLY',_utf8mb4'YEARLY'))),
  CONSTRAINT `subscription_payment_chk_2` CHECK ((`method` in (_utf8mb4'CLIQ',_utf8mb4'CASH',_utf8mb4'COMPLIMENTARY'))),
  CONSTRAINT `subscription_payment_chk_3` CHECK ((`plan` in (_utf8mb4'FREE',_utf8mb4'PRO',_utf8mb4'BUSINESS'))),
  CONSTRAINT `subscription_payment_chk_4` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'CONFIRMED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `role` tinyint DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `salon_id` bigint DEFAULT NULL,
  `career_profile_enabled` bit(1) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `portfolio_url` varchar(255) DEFAULT NULL,
  `professional_bio` varchar(2000) DEFAULT NULL,
  `resume_url` varchar(255) DEFAULT NULL,
  `years_of_experience` int DEFAULT NULL,
  `credentials_updated_at` datetime(6) DEFAULT NULL,
  `email_verified` bit(1) DEFAULT NULL,
  `last_activity_at` datetime(6) DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `total_login_count` bigint NOT NULL,
  `profile_picture_public_id` varchar(255) DEFAULT NULL,
  `profile_picture_url` varchar(1000) DEFAULT NULL,
  `salon_manager` bit(1) NOT NULL,
  `salon_joined_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7i9qqhafnn5yu5p901fjjjya9` (`salon_id`),
  CONSTRAINT `FK7i9qqhafnn5yu5p901fjjjya9` FOREIGN KEY (`salon_id`) REFERENCES `salon` (`id`),
  CONSTRAINT `user_chk_1` CHECK ((`role` between 0 and 3)),
  CONSTRAINT `user_chk_2` CHECK ((`status` between 0 and 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_activity_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_count` bigint NOT NULL,
  `activity_date` date NOT NULL,
  `first_seen_at` datetime(6) NOT NULL,
  `last_seen_at` datetime(6) NOT NULL,
  `login_count` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity_day` (`user_id`,`activity_date`),
  CONSTRAINT `FKrsxajqophgqdu06whcnk5676` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_career_skills` (
  `user_id` bigint NOT NULL,
  `skill` varchar(255) NOT NULL,
  KEY `FKr16pskd0n4i50n3bp43g1vd43` (`user_id`),
  CONSTRAINT `FKr16pskd0n4i50n3bp43g1vd43` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_date` date DEFAULT NULL,
  `booking_status` varchar(20) DEFAULT NULL,
  `booking_time` time DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `event_key` varchar(180) NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `message` varchar(500) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `salon_id` bigint NOT NULL,
  `salon_name` varchar(255) NOT NULL,
  `suggested_date` date DEFAULT NULL,
  `suggested_time` time DEFAULT NULL,
  `target_path` varchar(255) NOT NULL,
  `version` bigint NOT NULL,
  `booking_id` bigint DEFAULT NULL,
  `recipient_id` bigint NOT NULL,
  `domain` enum('BOOKING','COMMUNITY','SALON') NOT NULL,
  `reason` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_event_key` (`event_key`),
  KEY `idx_notification_recipient_created` (`recipient_id`,`created_at`),
  KEY `idx_notification_recipient_read` (`recipient_id`,`read_at`),
  KEY `FK50ccg90i8mp937c5eeoqtnhaw` (`booking_id`),
  CONSTRAINT `FK50ccg90i8mp937c5eeoqtnhaw` FOREIGN KEY (`booking_id`) REFERENCES `salon_booking` (`id`),
  CONSTRAINT `FKl6o8w154uun78yfuelfqis59b` FOREIGN KEY (`recipient_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_preferred_work_locations` (
  `user_id` bigint NOT NULL,
  `location` varchar(255) NOT NULL,
  KEY `FK1gdiojs57pn0spis0b4bx545x` (`user_id`),
  CONSTRAINT `FK1gdiojs57pn0spis0b4bx545x` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
