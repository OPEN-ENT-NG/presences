ALTER TABLE massmailing.mailing
ADD COLUMN file_id character varying (36),
ADD COLUMN metadata json;