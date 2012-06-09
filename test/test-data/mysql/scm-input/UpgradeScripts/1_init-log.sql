drop table if exists testlog;

GO

create table testlog (
	id int not null auto_increment,
	name varchar(100),
	primary key (id)
) engine InnoDB;
