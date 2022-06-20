CREATE TABLE categories (
    category_id UUID PRIMARY KEY,
    parent_id UUID REFERENCES categories (category_id) ON DELETE CASCADE,
    category_name VARCHAR NOT NULL,
    price BIGINT,
    last_update TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE offers (
    offer_id UUID PRIMARY KEY,
    category_id UUID REFERENCES categories (category_id) ON DELETE CASCADE,
    offer_name VARCHAR NOT NULL,
    price BIGINT NOT NULL,
    last_update TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE offers_statistics (
    id BIGSERIAL PRIMARY KEY,
    offer_id UUID REFERENCES offers (offer_id) ON DELETE CASCADE,
    category_id UUID,
    offer_name VARCHAR NOT NULL,
    price BIGINT NOT NULL,
    last_update TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE categories_statistics (
    id BIGSERIAL PRIMARY KEY,
    category_id UUID REFERENCES categories (category_id) ON DELETE CASCADE,
    parent_id UUID,
    category_name VARCHAR NOT NULL,
    price BIGINT,
    last_update TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT cat_stat_entry_unique UNIQUE (category_id, last_update)
);

CREATE VIEW common_statistics AS
SELECT
    category_id AS element_id,
    parent_id,
    category_name AS element_name,
    'CATEGORY' as element_type,
    price,
    last_update
FROM categories_statistics
UNION ALL
    SELECT
        offer_id AS element_id,
        category_id AS parent_id,
        offer_name AS element_name,
        'OFFER' as element_type,
        price,
        last_update
    FROM offers_statistics;

CREATE FUNCTION insert_offer_statistics()
    RETURNS trigger AS
$$
BEGIN
    INSERT INTO offers_statistics(offer_id, category_id, offer_name, price, last_update)
    VALUES (NEW.offer_id, NEW.category_id, NEW.offer_name, NEW.price, NEW.last_update);
    RETURN NEW;
END;
$$
    LANGUAGE 'plpgsql';

CREATE TRIGGER on_offer_updated
    AFTER INSERT OR UPDATE
    ON offers
    FOR EACH ROW
EXECUTE PROCEDURE insert_offer_statistics();

CREATE FUNCTION insert_category_statistics()
    RETURNS trigger AS
$$
BEGIN
    INSERT INTO categories_statistics(category_id, parent_id, category_name, price, last_update)
    VALUES (NEW.category_id, NEW.parent_id, NEW.category_name, NEW.price, NEW.last_update)
    ON CONFLICT ON CONSTRAINT cat_stat_entry_unique
        DO UPDATE SET category_id = excluded.category_id, parent_id = excluded.parent_id,
                      category_name = excluded.category_name, price = excluded.price,
                      last_update = excluded.last_update;
    RETURN NEW;
END;
$$
    LANGUAGE 'plpgsql';

CREATE TRIGGER on_category_updated
    AFTER INSERT OR UPDATE
    ON categories
    FOR EACH ROW
EXECUTE PROCEDURE insert_category_statistics();