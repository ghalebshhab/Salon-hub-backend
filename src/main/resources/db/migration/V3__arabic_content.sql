-- Arabic copies of owner-written text. Claude fills them when the owner saves
-- (and once for older rows); the owner can correct them in the forms.

ALTER TABLE `salon_service`
  ADD COLUMN `name_ar` varchar(200) DEFAULT NULL,
  ADD COLUMN `description_ar` varchar(2000) DEFAULT NULL;

ALTER TABLE `salon`
  ADD COLUMN `description_ar` varchar(1000) DEFAULT NULL;
