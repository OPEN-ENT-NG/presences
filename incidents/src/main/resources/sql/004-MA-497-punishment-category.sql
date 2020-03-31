ALTER TABLE incidents.punishment_type DROP periode_required;
ALTER TABLE incidents.punishment_type ADD punishment_category_id bigint NOT NULL;

CREATE TABLE incidents.punishment_category (
	id bigserial,
	label text NOT NULL,
	url_img character varying NOT NULL
);

INSERT INTO incidents.punishment_category(label, url_img)
   VALUES
   ('Devoir supplémentaire', 'resources/public/img/punishment_category/devoir_supplementaire.jpg'),
   ('Retenue', 'resources/public/img/punishment_category/retenue.jpg'),
   ('Blâme', 'resources/public/img/punishment_category/blame.jpg'),
   ('Exclusion', 'resources/public/img/punishment_category/exclusion.jpg');