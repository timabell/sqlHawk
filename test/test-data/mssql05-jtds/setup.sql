IF EXISTS ( SELECT name FROM sys.databases WHERE name = 'sqlhawktesting')
begin
	DROP DATABASE sqlhawktesting
end

GO

create database sqlhawktesting;

GO

USE sqlhawktesting
-- set up some existing schema that will be modified

GO

CREATE PROCEDURE oldproc AS

select 4;
RAISERROR('oldproc should have been modified', 16, 16);

GO

CREATE PROCEDURE unchangedproc AS

select 5;

GO

CREATE PROCEDURE unwantedproc AS

select 6;
