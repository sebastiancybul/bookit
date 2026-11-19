ALTER TABLE businesses ADD COLUMN city_key varchar(80);

UPDATE businesses
SET city_key = trim(BOTH '-' FROM regexp_replace(
        lower(translate(city, 'ąćęłńóśźżĄĆĘŁŃÓŚŹŻ', 'acelnoszzACELNOSZZ')),
        '[^a-z0-9]+', '-', 'g'));

ALTER TABLE businesses ALTER COLUMN city_key SET NOT NULL;

DROP INDEX ix_businesses_city_category;

CREATE INDEX ix_businesses_city_key_category ON businesses (city_key, category_id);
