INSERT INTO categories (category_id, parent_id, category_name, price, last_update)
VALUES
    ('915daef4-e71f-11ec-8fea-0242ac120002', null, 'Root category 1', 100, '2022-05-28T21:12:01.000Z'),
    ('915db3ea-e71f-11ec-8fea-0242ac120002', '915daef4-e71f-11ec-8fea-0242ac120002', 'Sub cat 1.1', 100, '2022-05-28T21:12:01.000Z'),
    ('915db110-e71f-11ec-8fea-0242ac120002', null, 'Root category 2', null, '2022-05-28T21:12:01.000Z'),
    ('915db52a-e71f-11ec-8fea-0242ac120002', '915db110-e71f-11ec-8fea-0242ac120002', 'Sub cat 2.1', null, '2022-05-28T21:12:01.000Z');

INSERT INTO offers (offer_id, category_id, offer_name, price, last_update)
VALUES
    ('915dbed0-e71f-11ec-8fea-0242ac120002', '915db3ea-e71f-11ec-8fea-0242ac120002', 'Offer', 100, '2022-05-28T21:12:01.000Z');