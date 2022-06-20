CREATE TYPE GENDER AS ENUM ('all', 'male', 'female');
CREATE TYPE ROLE AS ENUM ('admin', 'user');

CREATE TABLE IF NOT EXISTS users
(
    uuid     UUID PRIMARY KEY,
    name     VARCHAR        NOT NULL,
    email    VARCHAR UNIQUE NOT NULL,
    gender   GENDER         NOT NULL,
    password VARCHAR        NOT NULL,
    role     ROLE           NOT NULL DEFAULT 'user'
);

INSERT INTO "users" ("uuid", "name", "email", "gender", "password", "role")
VALUES ('c1039d34-425b-4f78-9a7f-893f5b4df478', 'Admin', 'saroyadmin@gmail.com', 'male',
        '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=', 'admin');