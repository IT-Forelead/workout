CREATE TYPE ROLE AS ENUM ('admin', 'user');

CREATE TABLE IF NOT EXISTS users
(
    uuid         UUID PRIMARY KEY,
    fullname     VARCHAR   NOT NULL,
    phone        VARCHAR   NOT NULL,
    birthday     TIMESTAMP NOT NULL,
    user_picture VARCHAR   NOT NULL,
    role         ROLE      NOT NULL DEFAULT 'user',
    password     VARCHAR   NOT NULL
);

INSERT INTO "users" ("uuid", "fullname", "phone", "birthday", "user_picture", "password", "role")
VALUES ('c1039d34-425b-4f78-9a7f-893f5b4df478', 'Admin', '+998901234567', '2022-01-01', '  ',
        '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=', 'admin');

CREATE TABLE IF NOT EXISTS payments
(
    uuid       UUID PRIMARY KEY,
    user_id    UUID      NOT NULL
        CONSTRAINT fk_user_id REFERENCES users (uuid) ON UPDATE NO ACTION ON DELETE NO ACTION,
    price      NUMERIC   NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
);