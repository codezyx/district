create table t_district
(
	id varchar(50) not null
		constraint t_district_pkey
			primary key,
	adcode varchar(20),
	name varchar(200),
	parent varchar(20),
	level varchar(20),
	update_time timestamp with time zone,
	state integer
)
;
comment on table t_district is '行政区划'
;
comment on column t_district.adcode is '区划编码'
;
comment on column t_district.parent is '父级编码'
;
comment on column t_district.state is '状态：0：正常；-1：已删除'
;
create table t_district_last
(
	id varchar(50) not null
		constraint t_district_test_pkey
			primary key,
	adcode varchar(20),
	name varchar(200),
	parent varchar(20),
	level varchar(20),
	update_time timestamp with time zone,
	state integer
)
;
comment on table t_district_last is '更新-双表备份'
;

