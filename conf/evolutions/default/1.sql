# --- !Ups

create table subscriber (
    id int unsigned not null AUTO_INCREMENT,
    username varchar(127) not null,
    repository varchar(127) not null,
    token varchar(255) not null,
    webhook_url varchar(1023),
    primary key(id)
);

create table push (
    id int unsigned not null AUTO_INCREMENT,
    pusher varchar(127) not null,
    commit_count int unsigned not null,
    status varchar(16) not null,
    zip_url varchar(1023),
    subscriber_id int unsigned not null,
    primary key(id),
    foreign key(subscriber_id) references subscriber(id) on delete cascade
)

# --- !Downs

drop table push;
drop table subscriber;