CREATE TABLE organizations(
  id UUID PRIMARY KEY,
  name VARCHAR NOT NULL,
  image_path VARCHAR,
  status VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE users(
  id UUID PRIMARY KEY,
  org_id UUID,
  role VARCHAR NOT NULL,
  username VARCHAR,
  password VARCHAR NOT NULL,
  email VARCHAR,
  phone_number VARCHAR NOT NULL,
  firstname VARCHAR,
  lastname VARCHAR,
  image_path VARCHAR,
  birthdate DATE,
  status VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE categories(
  id UUID PRIMARY KEY,
  org_id UUID,
  name VARCHAR NOT NULL,
  parent_id UUID,
  image_path VARCHAR,
  language VARCHAR,
  status VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  created_by UUID
);

CREATE TABLE products(
  id UUID PRIMARY KEY,
  org_id UUID,
  name VARCHAR NOT NULL,
  price NUMERIC,
  code VARCHAR,
  description TEXT,
  category_id UUID,
  image_path VARCHAR,
  language VARCHAR,
  status VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  created_by UUID,
  updated_by UUID
);

CREATE TABLE verification(
  phone_number VARCHAR NOT NULL,
  user_id UUID,
  verification_code SERIAL
);

CREATE TABLE sales(
  id UUID PRIMARY KEY,
  user_id UUID,
  sale_total DECIMAL,
  items jsonb,
  created_at TIMESTAMP,
  created_by UUID,
  created_by_name VARCHAR
);

ALTER TABLE verification ADD FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE users ADD FOREIGN KEY (org_id) REFERENCES organizations (id);

ALTER TABLE sales ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE products ADD FOREIGN KEY (org_id) REFERENCES organizations (id);

ALTER TABLE categories ADD FOREIGN KEY (org_id) REFERENCES organizations (id);

ALTER TABLE categories ADD FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE products ADD FOREIGN KEY (category_id) REFERENCES categories (id);

ALTER TABLE products ADD FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE products ADD FOREIGN KEY (updated_by) REFERENCES users (id);

COMMENT ON COLUMN categories.language IS 'UZ, latin by default';

COMMENT ON COLUMN products.language IS 'UZ, latin by default';
