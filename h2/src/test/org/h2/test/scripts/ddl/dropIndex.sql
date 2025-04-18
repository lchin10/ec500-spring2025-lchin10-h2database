-- Copyright 2004-2025 H2 Group. Multiple-Licensed under the MPL 2.0,
-- and the EPL 1.0 (https://h2database.com/html/license.html).
-- Initial Developer: H2 Group
--

CREATE SCHEMA TEST;
> ok

CREATE TABLE TEST.TBL (
    NAME VARCHAR
);
> ok

CREATE UNIQUE INDEX NAME_INDEX ON TEST.TBL(NAME);
> ok

SET MODE MySQL;
> ok

-- MySQL compatibility syntax
ALTER TABLE TEST.TBL DROP INDEX NAME_INDEX;
> ok

CREATE UNIQUE INDEX NAME_INDEX ON TEST.TBL(NAME);
> ok

-- MySQL compatibility syntax
ALTER TABLE TEST.TBL DROP INDEX TEST.NAME_INDEX;
> ok

ALTER TABLE TEST.TBL ADD CONSTRAINT NAME_INDEX UNIQUE (NAME);
> ok

-- MySQL compatibility syntax
ALTER TABLE TEST.TBL DROP INDEX NAME_INDEX;
> ok

ALTER TABLE TEST.TBL ADD CONSTRAINT NAME_INDEX UNIQUE (NAME);
> ok

-- MySQL compatibility syntax
ALTER TABLE TEST.TBL DROP INDEX TEST.NAME_INDEX;
> ok

DROP SCHEMA TEST CASCADE;
> ok

create table test(id int primary key, name varchar);
> ok

alter table test alter column id int auto_increment;
> ok

create table otherTest(id int primary key, name varchar);
> ok

alter table otherTest add constraint fk foreign key(id) references test(id);
> ok

-- MySQL compatibility syntax
alter table otherTest drop foreign key fk;
> ok

create unique index idx on otherTest(name);
> ok

-- MySQL compatibility syntax
alter table otherTest drop index idx;
> ok

drop table test, otherTest;
> ok

SET MODE Regular;
> ok
