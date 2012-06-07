drop database if exists sqlhawktesting ;

-- http://stackoverflow.com/a/3241918/10245
-- create temp user if missing to avoid drop error if not present (ugh)
GRANT USAGE ON *.* TO 'sqlhawktesting'@'localhost';
DROP USER 'sqlhawktesting'@'localhost' ;


create database sqlhawktesting;
create user 'sqlhawktesting'@'localhost' identified by 'sqlhawktesting';
grant all on sqlhawktesting.* to 'sqlhawktesting'@'localhost';

