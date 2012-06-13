-- This script creates the user that the MsSql integration test uses to create and use test database.
-- Run it before running the integration test.

CREATE LOGIN [sqlhawktesting] WITH PASSWORD='sqlhawktesting', DEFAULT_DATABASE=master, CHECK_EXPIRATION=OFF, CHECK_POLICY=OFF
EXEC sys.sp_addsrvrolemember @loginame = N'sqlhawktesting', @rolename = N'dbcreator'
