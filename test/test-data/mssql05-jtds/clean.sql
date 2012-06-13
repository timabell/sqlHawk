use master

-- kill active connections
-- ref http://www.julian-kuiters.id.au/article.php/sql-server-2005-snippet-drop-db-conn
ALTER DATABASE [sqlhawktesting]
SET OFFLINE WITH ROLLBACK IMMEDIATE
ALTER DATABASE [sqlhawktesting]
SET ONLINE

IF EXISTS ( SELECT name FROM sys.databases WHERE name = 'sqlhawktesting')
begin
	DROP DATABASE sqlhawktesting
end
