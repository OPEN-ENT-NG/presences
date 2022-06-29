-- Add column for viesco params, used in trigger
ALTER TABLE presences.settings ADD COLUMN exclude_alert_absence_no_reason boolean NOT NULL DEFAULT FALSE,
                               ADD COLUMN exclude_alert_lateness_no_reason boolean NOT NULL DEFAULT FALSE,
                               ADD COLUMN exclude_alert_forgotten_notebook boolean NOT NULL DEFAULT FALSE;

-- Table used as constant
CREATE TABLE presences.reason_alert_exclude_rules_type(
    id bigserial,
    rule_type varchar(36) UNIQUE,
    CONSTRAINT reason_alert_exclude_rules_type_pkey PRIMARY KEY(id)
);
-- Add some constant
INSERT INTO presences.reason_alert_exclude_rules_type(id, rule_type) VALUES (1, 'REGULARIZED'),(2, 'UNREGULARIZED'),(3, 'LATENESS');

-- Add new table for exclude reason
CREATE TABLE presences.reason_alert
(
    structure_id character varying(36),
    reason_id    bigint REFERENCES presences.reason(id),
    reason_alert_exclude_rules_type_id bigserial REFERENCES presences.reason_alert_exclude_rules_type(id),
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    deleted_at timestamp without time zone,
    CONSTRAINT uniq_reason_alert UNIQUE (structure_id, reason_id, reason_alert_exclude_rules_type_id)
);

-- By default, all the reasons already created must be excluded, whether they are regularized or not.
do $$
    declare
        reason presences.reason;
    begin
        FOR reason IN SELECT * FROM presences.reason WHERE reason_type_id = 1 LOOP
            INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (reason.structure_id, reason.id, 1);
            INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (reason.structure_id, reason.id, 2);
        end loop;
    end$$;