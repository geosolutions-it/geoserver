CREATE SCHEMA IF NOT EXISTS pinning;

CREATE TABLE pinning."views" (
	view_id int4 NOT NULL,
	time_original timestamp NOT NULL,
	time_main timestamp NOT NULL,
	layers_list _text NOT NULL,
	driving_layer text NOT NULL,
	last_update timestamp NOT NULL,
	CONSTRAINT views_pkey PRIMARY KEY (view_id, time_main)
);

CREATE TABLE pinning.pinning_run (
	id int4 DEFAULT 1 NOT NULL,
	last_run timestamp NULL,
	CONSTRAINT pinning_run_pk PRIMARY KEY (id)
);