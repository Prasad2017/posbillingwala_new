-- Allow customer mobile = dealer mobile; enforce mobile unique per role.
-- Run once on existing databases that still have UNIQUE(contact_number).

ALTER TABLE `users` DROP INDEX `contact_number`;

ALTER TABLE `users`
  ADD UNIQUE KEY `users_role_contact` (`role_id`, `contact_number`(20));
