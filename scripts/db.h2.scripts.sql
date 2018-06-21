DROP TABLE IF EXISTS t_district;
CREATE TABLE t_district(
  id VARCHAR(36) PRIMARY KEY,
	adcode varchar(20),
	name varchar(200),
	parent varchar(20),
	level varchar(20),
	update_time timestamp
  );
DROP TABLE IF EXISTS t_district_last;
create table t_district_last
(
	id varchar(36)
			primary key,
	adcode varchar(20),
	name varchar(200),
	parent varchar(20),
	level varchar(20),
	update_time timestamp
)
