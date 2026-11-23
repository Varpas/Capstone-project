-- Seed data: categories, authors, publishers, 30 books, coupons.

INSERT INTO categories (name) VALUES
 ('Romance'), ('Mystery'), ('Science Fiction'), ('Fantasy'), ('Historical'), ('Biography'),
 ('Self-help'), ('Memoir'), ('Travel'), ('Cooking'), ('Children''s'), ('Young Adult'),
 ('Comics & Graphic Novels'), ('Poetry'), ('Drama'), ('Science'), ('Philosophy'), ('Religion'),
 ('Language Learning'), ('Fiction'), ('Non-Fiction');

INSERT INTO publishers (name) VALUES
 ('ABC Publishers'), ('Lantern Press'), ('Orbit Ink'), ('Northwind Books'), ('Saffron House');

INSERT INTO authors (name, bio, photo_url) VALUES
 ('Daniel Reed',   'Daniel Reed writes about intentional living and has coached thousands of readers towards simpler lives.', 'https://images.example.com/authors/daniel-reed.jpg'),
 ('Maya Iyer',     'Maya Iyer is a romance novelist whose stories are set between Chennai and London.', 'https://images.example.com/authors/maya-iyer.jpg'),
 ('Tom Calloway',  'Tom Calloway is a former detective turned crime novelist.', 'https://images.example.com/authors/tom-calloway.jpg'),
 ('Elena Marsh',   'Elena Marsh writes science fiction about first contact and quiet futures.', 'https://images.example.com/authors/elena-marsh.jpg'),
 ('Arjun Menon',   'Arjun Menon is a fantasy author inspired by the myths of the Western Ghats.', 'https://images.example.com/authors/arjun-menon.jpg'),
 ('Priya Nair',    'Priya Nair is a historian and award-winning biographer.', 'https://images.example.com/authors/priya-nair.jpg'),
 ('Sam Whitaker',  'Sam Whitaker is a travel writer who has visited 90 countries.', 'https://images.example.com/authors/sam-whitaker.jpg'),
 ('Lena Hoffmann', 'Lena Hoffmann is a chef and cookbook author.', 'https://images.example.com/authors/lena-hoffmann.jpg'),
 ('Ravi Shankar Das','Ravi Shankar Das writes children''s stories and poetry.', 'https://images.example.com/authors/ravi-das.jpg'),
 ('Clara Boyd',    'Clara Boyd writes young adult fiction about growing up online.', 'https://images.example.com/authors/clara-boyd.jpg'),
 ('Dr. Anil Kapoor','Anil Kapoor is a physicist who explains science without the equations.', 'https://images.example.com/authors/anil-kapoor.jpg'),
 ('Sofia Alvarez', 'Sofia Alvarez writes about philosophy, language and learning.', 'https://images.example.com/authors/sofia-alvarez.jpg');

INSERT INTO books (title, isbn, description, author_id, publisher_id, format, language, price, stock, delivery_days,
                   cover_image_url, back_cover_image_url, published_date, copies_sold, created_at)
SELECT v.title, v.isbn, v.descr, a.id, p.id, v.fmt, v.lang, v.price, v.stock, v.days,
       'https://images.example.com/covers/' || v.isbn || '-front.jpg',
       'https://images.example.com/covers/' || v.isbn || '-back.jpg',
       CAST(v.pub AS DATE), v.sold, CURRENT_TIMESTAMP
FROM (VALUES
 ('Joy of Minimalism',            '9780000000001', 'A practical guide to owning less and living more.',                    'Daniel Reed',     'ABC Publishers',  'PAPERBACK', 'English', 149.00, 40, 3, '2025-03-10', 145),
 ('The Quiet Habit',              '9780000000002', 'Small daily routines that compound into a calmer life.',               'Daniel Reed',     'ABC Publishers',  'EBOOK',     'English',  99.00,  0, 0, '2026-09-15',  60),
 ('Letters from Chennai',         '9780000000003', 'A love story told across two cities and forty letters.',               'Maya Iyer',       'Saffron House',   'PAPERBACK', 'English', 199.00, 25, 4, '2025-11-02',  88),
 ('Monsoon Hearts',               '9780000000004', 'Two rivals, one rainy season, and a wedding that must not happen.',    'Maya Iyer',       'Saffron House',   'HARDCOVER', 'English', 349.00, 12, 5, '2026-10-20',  35),
 ('The Last Witness',             '9780000000005', 'A detective has one night to find the only witness to a murder.',      'Tom Calloway',    'Northwind Books', 'PAPERBACK', 'English', 229.00, 30, 3, '2024-08-19', 210),
 ('Cold Case Files',              '9780000000006', 'Five unsolved crimes and the detective who would not let go.',         'Tom Calloway',    'Northwind Books', 'EBOOK',     'English', 129.00,  0, 0, '2026-08-05',  72),
 ('Signal from Kepler',           '9780000000007', 'The first message from another star arrives in a small village.',      'Elena Marsh',     'Orbit Ink',       'PAPERBACK', 'English', 279.00, 18, 4, '2025-06-21', 164),
 ('A Quiet Future',               '9780000000008', 'A near-future novel about the last unplugged town on Earth.',          'Elena Marsh',     'Orbit Ink',       'HARDCOVER', 'English', 399.00,  8, 6, '2026-11-01',  22),
 ('The Ghats Remember',           '9780000000009', 'A young archer learns that the mountains keep old promises.',          'Arjun Menon',     'Lantern Press',   'PAPERBACK', 'English', 259.00, 22, 4, '2025-01-30', 190),
 ('Crown of Monsoon Clouds',      '9780000000010', 'The second book of the Ghats cycle.',                                   'Arjun Menon',     'Lantern Press',   'EBOOK',     'English', 159.00,  0, 0, '2026-10-12',  95),
 ('The Spice Road Queen',         '9780000000011', 'The life of a trader who ruled the pepper coast.',                      'Priya Nair',      'Lantern Press',   'HARDCOVER', 'English', 449.00, 10, 5, '2024-12-11',  58),
 ('Empires of Ink',               '9780000000012', 'How printing presses built and broke empires.',                         'Priya Nair',      'Lantern Press',   'PAPERBACK', 'English', 299.00, 20, 4, '2025-09-09',  77),
 ('Biography of a Bookseller',    '9780000000013', 'The life of the man who opened the first bookstore in Bangalore.',      'Priya Nair',      'ABC Publishers',  'PAPERBACK', 'English', 189.00, 15, 3, '2023-05-17',  41),
 ('One Backpack, Ninety Countries','9780000000014','Travel stories and lessons from a life lived on the road.',              'Sam Whitaker',    'Northwind Books', 'PAPERBACK', 'English', 219.00, 35, 3, '2025-04-04', 120),
 ('Slow Trains of India',         '9780000000015', 'Eleven journeys across India by rail.',                                 'Sam Whitaker',    'Northwind Books', 'HARDCOVER', 'English', 379.00,  9, 5, '2026-07-23',  47),
 ('Salt, Fat, Heat',              '9780000000016', 'The fundamentals of home cooking, explained.',                          'Lena Hoffmann',   'ABC Publishers',  'HARDCOVER', 'English', 499.00, 14, 4, '2024-10-30', 133),
 ('15-Minute Dinners',            '9780000000017', 'Seventy fast recipes for busy evenings.',                               'Lena Hoffmann',   'ABC Publishers',  'PAPERBACK', 'English', 249.00, 28, 3, '2026-06-18',  99),
 ('The Moon Who Lost Her Way',    '9780000000018', 'A bedtime story about a very lost moon.',                               'Ravi Shankar Das','Saffron House',   'HARDCOVER', 'English', 179.00, 50, 3, '2025-02-14', 240),
 ('Rhymes for Rainy Days',        '9780000000019', 'Forty poems for children, and for anyone who still likes puddles.',     'Ravi Shankar Das','Saffron House',   'PAPERBACK', 'English', 129.00, 45, 3, '2023-11-25',  66),
 ('Offline',                      '9780000000020', 'Maya deletes every app for a month. Her friends do not take it well.',  'Clara Boyd',      'Orbit Ink',       'PAPERBACK', 'English', 199.00, 33, 3, '2026-03-03', 112),
 ('Group Chat',                   '9780000000021', 'A group chat, a secret, and six teenagers.',                            'Clara Boyd',      'Orbit Ink',       'EBOOK',     'English', 119.00,  0, 0, '2026-10-30',  54),
 ('The Cosmos in Plain Words',    '9780000000022', 'Black holes, time and light, with no equations.',                       'Dr. Anil Kapoor', 'Northwind Books', 'PAPERBACK', 'English', 269.00, 26, 4, '2025-07-07', 175),
 ('Why Is the Sky Blue?',         '9780000000023', 'Answers to the questions children (and adults) really ask.',            'Dr. Anil Kapoor', 'Northwind Books', 'EBOOK',     'English',  89.00,  0, 0, '2026-02-20',  81),
 ('Thinking Slowly',              '9780000000024', 'An introduction to philosophy for impatient people.',                   'Sofia Alvarez',   'Lantern Press',   'PAPERBACK', 'English', 239.00, 19, 4, '2025-05-12',  63),
 ('Learn Spanish in 30 Days',     '9780000000025', 'A structured plan with daily exercises and audio links.',               'Sofia Alvarez',   'ABC Publishers',  'PAPERBACK', 'English', 189.00, 40, 3, '2024-09-01', 150),
 ('The Habit Loop Journal',       '9780000000026', 'A guided journal to build one habit at a time.',                        'Daniel Reed',     'ABC Publishers',  'PAPERBACK', 'English', 139.00, 60, 3, '2026-05-05',  70),
 ('Where the Rivers Meet',        '9780000000027', 'A memoir of growing up between two rivers and two languages.',          'Priya Nair',      'Saffron House',   'PAPERBACK', 'English', 209.00, 21, 3, '2025-10-10',  52),
 ('The Midnight Library Heist',   '9780000000028', 'A thriller set in the world''s most secure rare-book vault.',            'Tom Calloway',    'Northwind Books', 'HARDCOVER', 'English', 359.00, 11, 5, '2026-11-10',  18),
 ('Stars Over Kerala',            '9780000000029', 'A graphic novel about a girl who maps the night sky.',                  'Arjun Menon',     'Lantern Press',   'PAPERBACK', 'English', 289.00, 16, 4, '2026-04-14',  38),
 ('Drama Club Disaster',          '9780000000030', 'A school play, a missing lead, and a lot of glitter.',                  'Clara Boyd',      'Saffron House',   'PAPERBACK', 'English', 169.00, 37, 3, '2025-12-01',  44)
) AS v(title, isbn, descr, author, publisher, fmt, lang, price, stock, days, pub, sold)
JOIN authors a ON a.name = v.author
JOIN publishers p ON p.name = v.publisher;

INSERT INTO book_categories (book_id, category_id)
SELECT b.id, c.id
FROM (VALUES
 ('9780000000001', 'Self-help'), ('9780000000001', 'Non-Fiction'),
 ('9780000000002', 'Self-help'), ('9780000000002', 'Non-Fiction'),
 ('9780000000003', 'Romance'),   ('9780000000003', 'Fiction'),
 ('9780000000004', 'Romance'),   ('9780000000004', 'Fiction'),
 ('9780000000005', 'Mystery'),   ('9780000000005', 'Fiction'),
 ('9780000000006', 'Mystery'),   ('9780000000006', 'Non-Fiction'),
 ('9780000000007', 'Science Fiction'), ('9780000000007', 'Fiction'),
 ('9780000000008', 'Science Fiction'), ('9780000000008', 'Fiction'),
 ('9780000000009', 'Fantasy'),   ('9780000000009', 'Fiction'),
 ('9780000000010', 'Fantasy'),   ('9780000000010', 'Fiction'),
 ('9780000000011', 'Historical'),('9780000000011', 'Biography'), ('9780000000011', 'Non-Fiction'),
 ('9780000000012', 'Historical'),('9780000000012', 'Non-Fiction'),
 ('9780000000013', 'Biography'), ('9780000000013', 'Non-Fiction'),
 ('9780000000014', 'Travel'),    ('9780000000014', 'Memoir'), ('9780000000014', 'Non-Fiction'),
 ('9780000000015', 'Travel'),    ('9780000000015', 'Non-Fiction'),
 ('9780000000016', 'Cooking'),   ('9780000000016', 'Non-Fiction'),
 ('9780000000017', 'Cooking'),   ('9780000000017', 'Non-Fiction'),
 ('9780000000018', 'Children''s'),('9780000000018', 'Fiction'),
 ('9780000000019', 'Children''s'),('9780000000019', 'Poetry'),
 ('9780000000020', 'Young Adult'),('9780000000020', 'Fiction'),
 ('9780000000021', 'Young Adult'),('9780000000021', 'Fiction'),
 ('9780000000022', 'Science'),   ('9780000000022', 'Non-Fiction'),
 ('9780000000023', 'Science'),    ('9780000000023', 'Children''s'),
 ('9780000000024', 'Philosophy'),('9780000000024', 'Non-Fiction'),
 ('9780000000025', 'Language Learning'), ('9780000000025', 'Non-Fiction'),
 ('9780000000026', 'Self-help'), ('9780000000026', 'Non-Fiction'),
 ('9780000000027', 'Memoir'),    ('9780000000027', 'Non-Fiction'),
 ('9780000000028', 'Mystery'),   ('9780000000028', 'Fiction'),
 ('9780000000029', 'Comics & Graphic Novels'), ('9780000000029', 'Fantasy'),
 ('9780000000030', 'Drama'),     ('9780000000030', 'Young Adult')
) AS m(isbn, category)
JOIN books b ON b.isbn = m.isbn
JOIN categories c ON c.name = m.category;

INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, valid_from, valid_to, active) VALUES
 ('WELCOME10', 'PERCENT', 10.00,   0.00, NULL, NULL, TRUE),
 ('FLAT100',   'FIXED',  100.00, 500.00, NULL, NULL, TRUE),
 ('EXPIRED20', 'PERCENT', 20.00,   0.00, TIMESTAMP WITH TIME ZONE '2020-01-01 00:00:00+00', TIMESTAMP WITH TIME ZONE '2020-12-31 00:00:00+00', TRUE);
