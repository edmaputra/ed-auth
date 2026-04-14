CREATE TABLE IF NOT EXISTS user_profiles (
    username varchar(50) NOT NULL,
    full_name varchar(150) NOT NULL,
    email varchar(254) NOT NULL,
    email_verified boolean NOT NULL,
    locale varchar(35) NOT NULL,
    zoneinfo varchar(100) NOT NULL,
    department varchar(100) NOT NULL,
    tenant varchar(100) NOT NULL,
    updated_at bigint NOT NULL,
    PRIMARY KEY (username),
    CONSTRAINT fk_user_profiles_users
        FOREIGN KEY (username) REFERENCES users (username)
);