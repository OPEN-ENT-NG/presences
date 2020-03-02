CREATE INDEX idx_mailings_created
ON massmailing.mailing
USING btree
(created DESC);