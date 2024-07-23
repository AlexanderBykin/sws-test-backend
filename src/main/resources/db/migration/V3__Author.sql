create table author
(
    id serial primary key,
    fio varchar(255) not null,
    date_create timestamp without time zone not null
);

alter table budget add author_id int references author(id) on delete set null;