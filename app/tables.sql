CREATE TYPE PAYMENT_TYPE AS ENUM ('daily', 'monthly');
CREATE TYPE ARRIVAL_TYPE AS ENUM ('come_in', 'go_out');
CREATE TYPE DELIVERY_STATUS AS ENUM ('sent', 'delivered', 'failed', 'unknown');

CREATE TABLE IF NOT EXISTS users(
    id        UUID PRIMARY KEY,
    firstname VARCHAR        NOT NULL,
    lastname  VARCHAR        NOT NULL,
    phone     VARCHAR UNIQUE NOT NULL,
    password  VARCHAR        NOT NULL,
    deleted   BOOLEAN        NOT NULL DEFAULT false
);

INSERT INTO "users" ("id", "firstname", "lastname", "phone", "password")
VALUES ('76c2c44c-8fbf-4184-9199-19303a042fa0', 'Admin', 'Adminov', '+998901234567',
        '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=');

CREATE TABLE IF NOT EXISTS user_settings(
    user_id       UUID    NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    name          VARCHAR NOT NULL,
    daily_price   NUMERIC NOT NULL DEFAULT 0,
    monthly_price NUMERIC NOT NULL DEFAULT 0
);

INSERT INTO "user_settings" ("user_id", "name", "daily_price", "monthly_price")
VALUES ('76c2c44c-8fbf-4184-9199-19303a042fa0', 'GYM-Forelead', 11000, 88000);

CREATE TABLE IF NOT EXISTS members(
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    firstname       VARCHAR        NOT NULL,
    lastname        VARCHAR        NOT NULL,
    phone           VARCHAR UNIQUE NOT NULL,
    birthday        DATE           NOT NULL,
    active_time     TIMESTAMP      NOT NULL,
    image           VARCHAR        NULL,
    deleted         BOOLEAN        NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS payments(
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    member_id    UUID         NOT NULL
        CONSTRAINT fk_member_id REFERENCES members (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    payment_type PAYMENT_TYPE NOT NULL,
    price        NUMERIC      NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    deleted      BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS arrival_event(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    member_id  UUID         NOT NULL
        CONSTRAINT fk_member_id REFERENCES members (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    created_at TIMESTAMP    NOT NULL,
    arrival    ARRIVAL_TYPE NOT NULL,
    deleted    BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS messages(
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    member_id       UUID NOT NULL
        CONSTRAINT fk_member_id REFERENCES members (id) ON UPDATE CASCADE ON DELETE CASCADE,
    text            VARCHAR NOT NULL,
    sent_date       TIMESTAMP NOT NULL,
    delivery_status DELIVERY_STATUS NOT NULL
);
