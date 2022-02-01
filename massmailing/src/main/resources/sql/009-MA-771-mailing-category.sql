ALTER TABLE massmailing.template
    ADD category varchar(255);
UPDATE massmailing.template SET category = 'ALL'
WHERE category IS NULL;
ALTER TABLE massmailing.template ALTER COLUMN category SET NOT NULL;