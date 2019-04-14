CREATE TABLE IF NOT EXISTS QueryItem (
    id int8 not null,
    category varchar(255),
    name varchar(255),
    likes int8,
    primary key (id)
);