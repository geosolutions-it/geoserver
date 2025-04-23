CREATE TABLE public."msg_fes_rdt" (
	fid int4 NOT NULL,
	point public.geometry(point, 4326) NULL,
	testtime timestamp NOT NULL,
	pin int4 DEFAULT 0 NULL
);

INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31374280,'SRID=4326;POINT (16.38 29.46)'::public.geometry,'2025-03-12 00:00:00.000Z',0),
	 (31374293,'SRID=4326;POINT (-57.81 -5.32)'::public.geometry,'2025-03-12 00:00:00.000Z',0),
	 (31375004,'SRID=4326;POINT (-62.28 -9.86)'::public.geometry,'2025-03-12 00:30:00.000Z',0),
	 (31375005,'SRID=4326;POINT (-62.78 -10.32)'::public.geometry,'2025-03-12 00:30:00.000Z',0),
	 (31376091,'SRID=4326;POINT (-30.7 0.61)'::public.geometry,'2025-03-12 01:00:00.000Z',0),
	 (31375887,'SRID=4326;POINT (36.42 -28.1)'::public.geometry,'2025-03-12 01:00:00.000Z',0),
	 (31376873,'SRID=4326;POINT (-39.28 -0.49)'::public.geometry,'2025-03-12 01:30:00.000Z',0),
	 (31376906,'SRID=4326;POINT (-54.28 4.23)'::public.geometry,'2025-03-12 01:30:00.000Z',0),
	 (31377995,'SRID=4326;POINT (13.88 -4.85)'::public.geometry,'2025-03-12 02:00:00.000Z',0),
	 (31377994,'SRID=4326;POINT (13.76 -4.76)'::public.geometry,'2025-03-12 02:00:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31378534,'SRID=4326;POINT (-2.47 -0.01)'::public.geometry,'2025-03-12 02:30:00.000Z',0),
	 (31378884,'SRID=4326;POINT (-57.18 2.42)'::public.geometry,'2025-03-12 02:30:00.000Z',0),
	 (31379525,'SRID=4326;POINT (3.04 -1.78)'::public.geometry,'2025-03-12 03:00:00.000Z',0),
	 (31379524,'SRID=4326;POINT (8.09 2.51)'::public.geometry,'2025-03-12 03:00:00.000Z',0),
	 (31380709,'SRID=4326;POINT (44.57 -9.5)'::public.geometry,'2025-03-12 03:30:00.000Z',0),
	 (31380710,'SRID=4326;POINT (43.72 -9.96)'::public.geometry,'2025-03-12 03:30:00.000Z',0),
	 (31381303,'SRID=4326;POINT (-19.42 1.25)'::public.geometry,'2025-03-12 04:00:00.000Z',0),
	 (31381163,'SRID=4326;POINT (-1.2 -1.54)'::public.geometry,'2025-03-12 04:00:00.000Z',0),
	 (31382220,'SRID=4326;POINT (-47.49 -27.7)'::public.geometry,'2025-03-12 04:30:00.000Z',0),
	 (31382221,'SRID=4326;POINT (-48.77 -30.53)'::public.geometry,'2025-03-12 04:30:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31382783,'SRID=4326;POINT (-29.63 -0.95)'::public.geometry,'2025-03-12 05:00:00.000Z',0),
	 (31382784,'SRID=4326;POINT (-30.45 -3.58)'::public.geometry,'2025-03-12 05:00:00.000Z',0),
	 (31383517,'SRID=4326;POINT (-41.44 3.66)'::public.geometry,'2025-03-12 05:30:00.000Z',0),
	 (31383523,'SRID=4326;POINT (-13.11 2.05)'::public.geometry,'2025-03-12 05:30:00.000Z',0),
	 (31384540,'SRID=4326;POINT (-8.31 -40.36)'::public.geometry,'2025-03-12 06:00:00.000Z',0),
	 (31384561,'SRID=4326;POINT (-51.29 2.18)'::public.geometry,'2025-03-12 06:00:00.000Z',0),
	 (31385095,'SRID=4326;POINT (-29.96 -31.79)'::public.geometry,'2025-03-12 06:30:00.000Z',0),
	 (31385096,'SRID=4326;POINT (-21.22 -35.54)'::public.geometry,'2025-03-12 06:30:00.000Z',0),
	 (31385998,'SRID=4326;POINT (-46.35 0.26)'::public.geometry,'2025-03-12 07:00:00.000Z',0),
	 (31386156,'SRID=4326;POINT (-2.39 0.47)'::public.geometry,'2025-03-12 07:00:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31386916,'SRID=4326;POINT (8.72 38.13)'::public.geometry,'2025-03-12 07:30:00.000Z',0),
	 (31386914,'SRID=4326;POINT (11.37 40.49)'::public.geometry,'2025-03-12 07:30:00.000Z',0),
	 (31387447,'SRID=4326;POINT (29.11 -3.56)'::public.geometry,'2025-03-12 08:00:00.000Z',0),
	 (31387442,'SRID=4326;POINT (32.16 -1.9)'::public.geometry,'2025-03-12 08:00:00.000Z',0),
	 (31388199,'SRID=4326;POINT (37.98 3.43)'::public.geometry,'2025-03-12 08:30:00.000Z',0),
	 (31388401,'SRID=4326;POINT (-2.72 -1.55)'::public.geometry,'2025-03-12 08:30:00.000Z',0),
	 (31388777,'SRID=4326;POINT (-9.55 -1.35)'::public.geometry,'2025-03-12 09:00:00.000Z',0),
	 (31389125,'SRID=4326;POINT (1.9 -1.22)'::public.geometry,'2025-03-12 09:00:00.000Z',0),
	 (31389881,'SRID=4326;POINT (33.28 3.27)'::public.geometry,'2025-03-12 09:30:00.000Z',0),
	 (31389532,'SRID=4326;POINT (-38.04 1.54)'::public.geometry,'2025-03-12 09:30:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31390516,'SRID=4326;POINT (-34.6 -3.79)'::public.geometry,'2025-03-12 10:00:00.000Z',0),
	 (31390378,'SRID=4326;POINT (-32.37 -3.06)'::public.geometry,'2025-03-12 10:00:00.000Z',0),
	 (31391430,'SRID=4326;POINT (28.79 -0.7)'::public.geometry,'2025-03-12 10:30:00.000Z',0),
	 (31391547,'SRID=4326;POINT (6.94 -0.72)'::public.geometry,'2025-03-12 10:30:00.000Z',0),
	 (31392596,'SRID=4326;POINT (-3.26 -3.62)'::public.geometry,'2025-03-12 11:00:00.000Z',0),
	 (31392564,'SRID=4326;POINT (10.58 42.06)'::public.geometry,'2025-03-12 11:00:00.000Z',0),
	 (31393120,'SRID=4326;POINT (37.91 10.72)'::public.geometry,'2025-03-12 11:30:00.000Z',0),
	 (31393440,'SRID=4326;POINT (-3.53 6.82)'::public.geometry,'2025-03-12 11:30:00.000Z',0),
	 (31394706,'SRID=4326;POINT (-51.44 -14.92)'::public.geometry,'2025-03-12 12:00:00.000Z',0),
	 (31394323,'SRID=4326;POINT (-16.44 0.49)'::public.geometry,'2025-03-12 12:00:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31395735,'SRID=4326;POINT (27.18 -24.86)'::public.geometry,'2025-03-12 12:30:00.000Z',0),
	 (31395568,'SRID=4326;POINT (13.71 26.71)'::public.geometry,'2025-03-12 12:30:00.000Z',0),
	 (31397094,'SRID=4326;POINT (15.05 -11.89)'::public.geometry,'2025-03-12 13:00:00.000Z',0),
	 (31397491,'SRID=4326;POINT (28.61 -9.79)'::public.geometry,'2025-03-12 13:00:00.000Z',0),
	 (31402229,'SRID=4326;POINT (40.1 -11.8)'::public.geometry,'2025-03-12 13:30:00.000Z',0),
	 (31402228,'SRID=4326;POINT (31.28 -0.18)'::public.geometry,'2025-03-12 13:30:00.000Z',0),
	 (31400853,'SRID=4326;POINT (23.58 -12.08)'::public.geometry,'2025-03-12 14:00:00.000Z',0),
	 (31401032,'SRID=4326;POINT (11.93 44.38)'::public.geometry,'2025-03-12 14:00:00.000Z',0),
	 (31398583,'SRID=4326;POINT (-31.82 -5.06)'::public.geometry,'2025-03-12 14:30:00.000Z',0),
	 (31398590,'SRID=4326;POINT (-66.02 -5.56)'::public.geometry,'2025-03-12 14:30:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31405584,'SRID=4326;POINT (29.18 -30.41)'::public.geometry,'2025-03-12 15:00:00.000Z',0),
	 (31405727,'SRID=4326;POINT (23.83 -14.16)'::public.geometry,'2025-03-12 15:00:00.000Z',0),
	 (31404597,'SRID=4326;POINT (20.61 -23.38)'::public.geometry,'2025-03-12 15:30:00.000Z',0),
	 (31404616,'SRID=4326;POINT (-34.73 -28.01)'::public.geometry,'2025-03-12 15:30:00.000Z',0),
	 (31407685,'SRID=4326;POINT (24.29 -15.84)'::public.geometry,'2025-03-12 16:00:00.000Z',0),
	 (31407448,'SRID=4326;POINT (-47.94 -13.39)'::public.geometry,'2025-03-12 16:00:00.000Z',0),
	 (31408977,'SRID=4326;POINT (-24.74 -2.92)'::public.geometry,'2025-03-12 16:30:00.000Z',0),
	 (31409478,'SRID=4326;POINT (32.38 -20.57)'::public.geometry,'2025-03-12 16:30:00.000Z',0),
	 (31410691,'SRID=4326;POINT (-42.88 0.7)'::public.geometry,'2025-03-12 17:00:00.000Z',0),
	 (31411238,'SRID=4326;POINT (16.65 -11.33)'::public.geometry,'2025-03-12 17:00:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31413119,'SRID=4326;POINT (26.36 2.26)'::public.geometry,'2025-03-12 17:30:00.000Z',0),
	 (31412710,'SRID=4326;POINT (-52.55 6.51)'::public.geometry,'2025-03-12 17:30:00.000Z',0),
	 (31414201,'SRID=4326;POINT (-28.55 -6.98)'::public.geometry,'2025-03-12 18:00:00.000Z',0),
	 (31414377,'SRID=4326;POINT (-43.91 2.35)'::public.geometry,'2025-03-12 18:00:00.000Z',0),
	 (31415791,'SRID=4326;POINT (7.11 8.65)'::public.geometry,'2025-03-12 18:30:00.000Z',0),
	 (31415814,'SRID=4326;POINT (63.94 5.94)'::public.geometry,'2025-03-12 18:30:00.000Z',0),
	 (31417730,'SRID=4326;POINT (-48.35 -14.87)'::public.geometry,'2025-03-12 19:00:00.000Z',0),
	 (31418109,'SRID=4326;POINT (-60.79 -4.46)'::public.geometry,'2025-03-12 19:00:00.000Z',0),
	 (31419463,'SRID=4326;POINT (-62.8 -8.11)'::public.geometry,'2025-03-12 19:30:00.000Z',0),
	 (31419082,'SRID=4326;POINT (48.84 -6.29)'::public.geometry,'2025-03-12 19:30:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31421088,'SRID=4326;POINT (-5.39 -0.85)'::public.geometry,'2025-03-12 20:00:00.000Z',0),
	 (31420461,'SRID=4326;POINT (-19.47 2.85)'::public.geometry,'2025-03-12 20:00:00.000Z',0),
	 (31422283,'SRID=4326;POINT (29.26 -2.89)'::public.geometry,'2025-03-12 20:30:00.000Z',0),
	 (31422394,'SRID=4326;POINT (12.99 -2.11)'::public.geometry,'2025-03-12 20:30:00.000Z',0),
	 (31423377,'SRID=4326;POINT (-46.58 -7.36)'::public.geometry,'2025-03-12 21:00:00.000Z',0),
	 (31423275,'SRID=4326;POINT (44.14 -8.63)'::public.geometry,'2025-03-12 21:00:00.000Z',0),
	 (31424608,'SRID=4326;POINT (-41.2 -6)'::public.geometry,'2025-03-12 21:30:00.000Z',0),
	 (31424841,'SRID=4326;POINT (-53.8 2.17)'::public.geometry,'2025-03-12 21:30:00.000Z',0),
	 (31425984,'SRID=4326;POINT (-45 -26.79)'::public.geometry,'2025-03-12 22:00:00.000Z',0),
	 (31425919,'SRID=4326;POINT (-48.55 -25.84)'::public.geometry,'2025-03-12 22:00:00.000Z',0);
INSERT INTO "msg_fes_rdt" (fid,point,testtime,pin) VALUES
	 (31427046,'SRID=4326;POINT (-47.87 -3.61)'::public.geometry,'2025-03-12 22:30:00.000Z',0),
	 (31427049,'SRID=4326;POINT (48.05 -11.58)'::public.geometry,'2025-03-12 22:30:00.000Z',0),
	 (31428226,'SRID=4326;POINT (-52.67 0.99)'::public.geometry,'2025-03-12 23:00:00.000Z',0),
	 (31428331,'SRID=4326;POINT (-46.55 -1.14)'::public.geometry,'2025-03-12 23:00:00.000Z',0),
	 (31428867,'SRID=4326;POINT (62.57 5.57)'::public.geometry,'2025-03-12 23:30:00.000Z',0),
	 (31429291,'SRID=4326;POINT (-53.61 5.45)'::public.geometry,'2025-03-12 23:30:00.000Z',0);
