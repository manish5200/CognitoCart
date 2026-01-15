-- ==========================================
-- 1. IDENTITY & PROFILES
-- ==========================================
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(180) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       phone VARCHAR(20) UNIQUE NOT NULL,
                       date_of_birth DATE,
                       gender VARCHAR(20),
                       role VARCHAR(50) NOT NULL,
                       active BOOLEAN DEFAULT TRUE NOT NULL,
                       primary_address_id BIGINT, -- Constraint added at the end
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP,
                       created_by VARCHAR(255),
                       modified_by VARCHAR(255),
                       version BIGINT DEFAULT 0
);

CREATE TABLE customer_profiles (
                                   user_id BIGINT PRIMARY KEY REFERENCES users(id),
                                   loyalty_points INTEGER DEFAULT 0,
                                   preferences VARCHAR(500)
);

CREATE TABLE seller_profiles (
                                 user_id BIGINT PRIMARY KEY REFERENCES users(id),
                                 store_name VARCHAR(120) NOT NULL,
                                 business_address TEXT,
                                 gstin VARCHAR(15) UNIQUE,
                                 pan_card VARCHAR(255),
                                 kyc_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- ==========================================
-- 2. LOGISTICS (ADDRESS WALLET)
-- ==========================================
CREATE TABLE user_addresses (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL REFERENCES users(id),
                                full_name VARCHAR(255) NOT NULL,
                                phone_number VARCHAR(20) NOT NULL,
                                street_address TEXT NOT NULL,
                                landmark VARCHAR(255),
                                city VARCHAR(100) NOT NULL,
                                state VARCHAR(100) NOT NULL,
                                zip_code VARCHAR(10) NOT NULL,
                                country VARCHAR(100) NOT NULL,
                                is_default BOOLEAN DEFAULT FALSE NOT NULL,
                                is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
                                created_at TIMESTAMP NOT NULL,
                                updated_at TIMESTAMP,
                                created_by VARCHAR(255),
                                modified_by VARCHAR(255),
                                version BIGINT DEFAULT 0
);

-- ==========================================
-- 3. PRODUCT CATALOG & REVIEWS
-- ==========================================
CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            slug VARCHAR(255) UNIQUE NOT NULL,
                            parent_id BIGINT REFERENCES categories(id),
                            created_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP,
                            created_by VARCHAR(255),
                            modified_by VARCHAR(255),
                            version BIGINT DEFAULT 0
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          product_name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) UNIQUE NOT NULL,
                          description TEXT,
                          price DECIMAL(19, 2) NOT NULL,
                          sku VARCHAR(255) UNIQUE NOT NULL,
                          stock_quantity INTEGER NOT NULL DEFAULT 0,
                          is_available BOOLEAN DEFAULT TRUE NOT NULL,
                          average_rating DOUBLE PRECISION DEFAULT 0.0,
                          total_reviews INTEGER DEFAULT 0,
                          seller_id BIGINT NOT NULL,
                          category_id BIGINT REFERENCES categories(id),
                          is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP,
                          created_by VARCHAR(255),
                          modified_by VARCHAR(255),
                          version BIGINT DEFAULT 0
);

CREATE TABLE product_images (
                                product_id BIGINT NOT NULL REFERENCES products(id),
                                image_url VARCHAR(255)
);

CREATE TABLE reviews (
                         id BIGSERIAL PRIMARY KEY,
                         product_id BIGINT NOT NULL REFERENCES products(id),
                         user_id BIGINT NOT NULL REFERENCES users(id),
                         rating INTEGER NOT NULL,
                         comment VARCHAR(1000),
                         created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP,
                         created_by VARCHAR(255),
                         modified_by VARCHAR(255),
                         version BIGINT DEFAULT 0
);

-- ==========================================
-- 4. CART & ORDERS
-- ==========================================
CREATE TABLE carts (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT UNIQUE REFERENCES users(id),
                       total_amount DECIMAL(19, 2) DEFAULT 0.0,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP,
                       created_by VARCHAR(255),
                       modified_by VARCHAR(255),
                       version BIGINT DEFAULT 0
);

CREATE TABLE cart_items (
                            id BIGSERIAL PRIMARY KEY,
                            cart_id BIGINT REFERENCES carts(id),
                            product_id BIGINT REFERENCES products(id),
                            quantity INTEGER NOT NULL,
                            price_at_adding DECIMAL(19, 2),
                            created_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP,
                            created_by VARCHAR(255),
                            modified_by VARCHAR(255),
                            version BIGINT DEFAULT 0
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT REFERENCES users(id),
                        order_date TIMESTAMP,
                        total_amount DECIMAL(19, 2),
                        order_status VARCHAR(50),
                        shipping_full_name VARCHAR(255),
                        shipping_phone VARCHAR(20),
                        shipping_street_address VARCHAR(255),
                        shipping_city VARCHAR(100),
                        shipping_state VARCHAR(100),
                        shipping_zip_code VARCHAR(10),
                        shipping_country VARCHAR(100),
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP,
                        created_by VARCHAR(255),
                        modified_by VARCHAR(255),
                        version BIGINT DEFAULT 0
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT REFERENCES orders(id),
                             product_id BIGINT REFERENCES products(id),
                             quantity INTEGER,
                             price_at_purchase DECIMAL(19, 2)
);

CREATE TABLE user_wishlist (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT REFERENCES users(id),
                               product_id BIGINT REFERENCES products(id),
                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP,
                               created_by VARCHAR(255),
                               modified_by VARCHAR(255),
                               version BIGINT DEFAULT 0
);

CREATE TABLE product_tags (
                              product_id BIGINT NOT NULL REFERENCES products(id),
                              tag VARCHAR(255)
);

-- Circular logic resolution
ALTER TABLE users ADD CONSTRAINT fk_user_primary_address
    FOREIGN KEY (primary_address_id) REFERENCES user_addresses(id);