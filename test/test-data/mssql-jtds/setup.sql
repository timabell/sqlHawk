IF EXISTS ( SELECT name FROM sys.databases WHERE name = 'sqlhawktesting')
begin
	DROP DATABASE sqlhawktesting
end

GO

create database sqlhawktesting;
