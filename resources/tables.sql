CREATE TABLE owner(
  id UUID PRIMARY KEY,
  organizations JSONB,
  role VARCHAR NOT NULL,
  password VARCHAR NOT NULL,
  email VARCHAR,
  phone_number VARCHAR UNIQUE NOT NULL,
  firstname VARCHAR,
  lastname VARCHAR,
  birthdate DATE,
  status VARCHAR NOT NULL,
  created_at TIMESTAMP
);

CREATE TABLE organization(
  id UUID PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL,
  status VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE staff(
  id UUID PRIMARY KEY,
  org_id UUID NOT NULL,
  branch_id UUID,
  role VARCHAR NOT NULL,
  username VARCHAR,
  password VARCHAR NOT NULL,
  email VARCHAR,
  phone_number VARCHAR UNIQUE NOT NULL,
  firstname VARCHAR,
  lastname VARCHAR,
  birthdate DATE,
  status VARCHAR NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE category(
  id UUID PRIMARY KEY,
  org_id UUID NOT NULL,
  name VARCHAR UNIQUE NOT NULL,
  status VARCHAR
);

CREATE TABLE branch(
  id UUID PRIMARY KEY,
  org_id UUID NOT NULL,
  name VARCHAR UNIQUE NOT NULL,
  address  JSONB,
  status VARCHAR,
  created_by UUID,
  created_at TIMESTAMP
);

CREATE TABLE product(
  id UUID PRIMARY KEY,
  org_id UUID NOT NULL,
  branch_id UUID,
  name VARCHAR UNIQUE NOT NULL,
  quantity NUMERIC NOT NULL,
  unit_id UUID,
  description TEXT,
  category_id UUID,
  cost_price NUMERIC,
  surcharge NUMERIC,
  price NUMERIC,
  product_code VARCHAR,
  sale_start TIMESTAMP,
  sale_end VARCHAR,
  bar_code VARCHAR,
  qr_code VARCHAR,
  expiration_date DATE,
  last_transaction_time TIMESTAMP,
  image_path VARCHAR,
  status VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE transaction(
  id UUID PRIMARY KEY,
  org_id UUID NOT NULL,
  staff_id UUID NOT NULL,
  order_id UUID NOT NULL,
  branch_id UUID,
  products JSONB,
  payment_type VARCHAR,
  amount NUMERIC,
  discount NUMERIC,
  deadline TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE unit(
  id UUID PRIMARY KEY,
  org_id UUID NOT NULL,
  name VARCHAR UNIQUE NOT NULL,
  status VARCHAR NOT NULL
);

CREATE TABLE orders(
    id UUID PRIMARY KEY,
    staff_id UUID NOT NULL,
    org_id UUID NOT NULL,
    branch_id UUID,
    payment_status VARCHAR NOT NULL,
    order_status VARCHAR NOT NULL,
    products JSONB NOT NULL,
    total_amount NUMERIC  NOT NULL,
    paid_amount NUMERIC NOT NULL,
    unpaid_amount NUMERIC NOT NULL,
    refunded_amount NUMERIC,
    returned_items JSONB,
    created_by UUID NOT NULL,
    updated_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    delivery_time TIMESTAMP
);

CREATE TABLE agent_client(
    id UUID PRIMARY KEY,
    agent_id UUID NOT NULL,
    org_id UUID NOT NULL,
    phone_number VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    address JSONB NOT NULL,
    status VARCHAR NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE recovery(
  phone_number VARCHAR NOT NULL,
  code NUMERIC NOT NULL
);

ALTER TABLE branch ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE branch ADD FOREIGN KEY (created_by) REFERENCES staff (id);

ALTER TABLE transaction ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE transaction ADD FOREIGN KEY (staff_id) REFERENCES staff (id);

ALTER TABLE transaction ADD FOREIGN KEY (branch_id) REFERENCES branch (id);

ALTER TABLE transaction ADD FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE staff ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE staff ADD FOREIGN KEY (branch_id) REFERENCES branch (id);

ALTER TABLE unit ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE product ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE product ADD FOREIGN KEY (unit_id) REFERENCES unit (id);

ALTER TABLE product ADD FOREIGN KEY (category_id) REFERENCES category (id);

ALTER TABLE product ADD FOREIGN KEY (branch_id) REFERENCES branch (id);

ALTER TABLE orders ADD FOREIGN KEY (staff_id) REFERENCES staff (id);

ALTER TABLE orders ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE orders ADD FOREIGN KEY (branch_id) REFERENCES branch (id);

ALTER TABLE agent_client ADD FOREIGN KEY (org_id) REFERENCES organization (id);

ALTER TABLE agent_client ADD FOREIGN KEY (agent_id) REFERENCES staff (id);

ALTER TABLE agent_client ADD FOREIGN KEY (created_by) REFERENCES staff (id);

ALTER TABLE agent_client ADD FOREIGN KEY (updated_by) REFERENCES staff (id);
-- code below: prevents the creation of clients with the same org_id and phone_number pair
ALTER TABLE agent_client ADD CONSTRAINT unique_org_id_and_phone_number UNIQUE (phone_number, org_id);
