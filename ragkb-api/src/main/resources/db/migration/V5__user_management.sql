-- 1) Novos campos de perfil
ALTER TABLE app_users ADD COLUMN full_name VARCHAR(150) NOT NULL DEFAULT '';
ALTER TABLE app_users ADD COLUMN email VARCHAR(150) NOT NULL DEFAULT '';
ALTER TABLE app_users ADD COLUMN phone VARCHAR(40);

-- E-mail único, ignorando registros ainda sem e-mail preenchido
CREATE UNIQUE INDEX uk_app_users_email ON app_users (email) WHERE email <> '';