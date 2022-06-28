CREATE TYPE ROLE AS ENUM ('admin', 'user');

CREATE TABLE IF NOT EXISTS users
(
    uuid      UUID PRIMARY KEY,
    firstname VARCHAR        NOT NULL,
    lastname  VARCHAR        NOT NULL,
    phone     VARCHAR UNIQUE NOT NULL,
    gym_name  VARCHAR        NOT NULL,
    password  VARCHAR        NOT NULL
);

CREATE TABLE IF NOT EXISTS members
(
    uuid         UUID PRIMARY KEY,
    gym_id       UUID           NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (uuid) ON UPDATE NO ACTION ON DELETE NO ACTION,
    firstname    VARCHAR        NOT NULL,
    lastname     VARCHAR        NOT NULL,
    phone        VARCHAR UNIQUE NOT NULL,
    birthday     DATE           NOT NULL,
    user_picture VARCHAR        NULL
);

CREATE TABLE IF NOT EXISTS payments
(
    uuid       UUID PRIMARY KEY,
    user_id    UUID      NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (uuid) ON UPDATE NO ACTION ON DELETE NO ACTION,
    price      NUMERIC   NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
);

INSERT INTO "users" ("uuid", "fullname", "phone", "birthday", "user_picture", "password", "role")
VALUES ('76c2c44c-8fbf-4184-9199-19303a042fa0', 'Admin Adminov', '+998901237771', '2022-01-01',
        'images/84385647-4114-4e3f-9caf-358b54f6b955.jpg',
        '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=', 'admin');