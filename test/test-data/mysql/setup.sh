#!/bin/sh
echo "Setting up 'sqlhawktesting' user and database on mysql..."
mysql -u root < setup.sql
echo "Done."
