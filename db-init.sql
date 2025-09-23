-- user pagatu
create user pagatu WITH PASSWORD '<redacted>';
comment on role pagatu is 'pagatu user';

-- db auth
create database auth
    with owner pagatu;

comment on database auth is 'auth db';
GRANT ALL PRIVILEGES ON DATABASE auth to pagatu;

-- db coffee
create database coffee
    with owner pagatu;

comment on database coffee is 'coffee db';
GRANT ALL PRIVILEGES ON DATABASE coffee to pagatu;
