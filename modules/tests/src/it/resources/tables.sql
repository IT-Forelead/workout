CREATE TYPE ROLE AS ENUM ('admin', 'user');

CREATE TABLE IF NOT EXISTS users
(
    uuid         UUID PRIMARY KEY,
    fullname     VARCHAR   NOT NULL,
    tel          VARCHAR   NOT NULL,
    birthday     TIMESTAMP NOT NULL,
    user_picture VARCHAR   NOT NULL,
    password     VARCHAR   NOT NULL,
    role         ROLE      NOT NULL DEFAULT 'user'
);

INSERT INTO "users" ("uuid", "fullname","tel","birthday","user_picture", "password", "role")
VALUES ('c1039d34-425b-4f78-9a7f-893f5b4df478', 'Admin','+998901234567','2022-01-01','  ',
        '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=', 'admin');