ALTER TABLE massmailing.mailing
DROP CONSTRAINT type_check;

ALTER TABLE massmailing.mailing
ADD CONSTRAINT type_check CHECK (type IN ('MAIL', 'SMS'));

ALTER TABLE massmailing.template
DROP CONSTRAINT type_check;

ALTER TABLE massmailing.template
ADD CONSTRAINT type_check CHECK (type IN ('MAIL', 'SMS'));