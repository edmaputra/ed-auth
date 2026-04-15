CREATE TABLE IF NOT EXISTS users (
    username varchar(50) NOT NULL,
    password varchar(500) NOT NULL,
    enabled boolean NOT NULL,
    PRIMARY KEY (username)
);

CREATE TABLE IF NOT EXISTS authorities (
    username varchar(50) NOT NULL,
    authority varchar(50) NOT NULL,
    CONSTRAINT fk_authorities_users
        FOREIGN KEY (username) REFERENCES users (username)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_username
    ON authorities (username, authority);