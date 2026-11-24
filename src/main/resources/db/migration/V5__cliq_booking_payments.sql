-- CliQ (Jordan instant transfer) as a booking payment method.
-- The salon owner saves a CliQ alias; the customer transfers to it; the owner
-- confirms that the money arrived. The booking keeps the alias the customer saw.
-- The CHECK constraints that list the allowed methods and transaction types are
-- removed at start-up by PaymentCheckConstraintUpgrade: MySQL and TiDB name them
-- differently, so plain SQL cannot drop them safely.

ALTER TABLE `salon_payment_settings`
  ADD COLUMN `cliq_enabled` bit(1) NOT NULL DEFAULT b'0',
  ADD COLUMN `cliq_alias` varchar(40) DEFAULT NULL,
  ADD COLUMN `cliq_name` varchar(80) DEFAULT NULL;

ALTER TABLE `booking_payment`
  ADD COLUMN `cliq_alias` varchar(40) DEFAULT NULL,
  ADD COLUMN `cliq_name` varchar(80) DEFAULT NULL;
