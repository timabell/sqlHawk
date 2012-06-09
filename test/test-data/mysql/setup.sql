drop database if exists sqlhawktesting;
GO

-- http://stackoverflow.com/a/3241918/10245
-- create temp user if missing to avoid drop error if not present (ugh)
GRANT USAGE ON *.* TO 'sqlhawktesting'@'localhost';
GO
DROP USER 'sqlhawktesting'@'localhost';
GO


create database sqlhawktesting;
GO
create user 'sqlhawktesting'@'localhost' identified by 'sqlhawktesting';
GO
grant all on sqlhawktesting.* to 'sqlhawktesting'@'localhost';
GO

-- set up some existing schema that will be modified

CREATE PROCEDURE sqlhawktesting.oldproc ()

select 4;

