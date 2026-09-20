INSERT INTO technologies (name, description) VALUES
    ('Контроль тяги', 'Система, не позволяющая заднему колесу пробуксовывать при разгоне и в поворотах.'),
    ('Квикшифтер', 'Переключение передач без сцепления: вверх и вниз за доли секунды.'),
    ('Активная подвеска', 'Полуактивная подвеска, подстраивающая жёсткость под условия дороги.'),
    ('Радарный круиз-контроль', 'Круиз-контроль с радаром, поддерживающий безопасную дистанцию до впереди идущего транспорта.'),
    ('ABS в поворотах', 'Антиблокировочная система, работающая при наклоне мотоцикла.'),
    ('Инерциальный блок', 'IMU-датчики, отслеживающие наклон и ускорения мотоцикла для электронных помощников.');

DO $$
DECLARE
    v_yzf   BIGINT;
    v_pani  BIGINT;
    v_fire  BIGINT;
    v_h2    BIGINT;
    v_gsxr  BIGINT;
    v_mt    BIGINT;
    v_sf    BIGINT;
    v_af    BIGINT;
    v_multi BIGINT;
    v_sd    BIGINT;
BEGIN
    SELECT id INTO v_yzf   FROM motorcycles WHERE brand = 'Yamaha'   AND model = 'YZF-R1';
    SELECT id INTO v_pani  FROM motorcycles WHERE brand = 'Ducati'   AND model = 'Panigale V4';
    SELECT id INTO v_fire  FROM motorcycles WHERE brand = 'Honda'    AND model = 'Fireblade SP';
    SELECT id INTO v_h2    FROM motorcycles WHERE brand = 'Kawasaki' AND model = 'Ninja H2';
    SELECT id INTO v_gsxr  FROM motorcycles WHERE brand = 'Suzuki'   AND model = 'GSX-R1000R';
    SELECT id INTO v_mt    FROM motorcycles WHERE brand = 'Yamaha'   AND model = 'MT-09 SP';
    SELECT id INTO v_sf    FROM motorcycles WHERE brand = 'Ducati'   AND model = 'Streetfighter V4';
    SELECT id INTO v_af    FROM motorcycles WHERE brand = 'Honda'    AND model = 'Africa Twin';
    SELECT id INTO v_multi FROM motorcycles WHERE brand = 'Ducati'   AND model = 'Multistrada V4 S';
    SELECT id INTO v_sd    FROM motorcycles WHERE brand = 'KTM'      AND model = '1290 Super Duke R';

    INSERT INTO motorcycle_technologies (motorcycle_id, technology_id) VALUES
        (v_yzf,   (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_yzf,   (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_yzf,   (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_yzf,   (SELECT id FROM technologies WHERE name = 'Инерциальный блок')),
        (v_pani,  (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_pani,  (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_pani,  (SELECT id FROM technologies WHERE name = 'Активная подвеска')),
        (v_pani,  (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_pani,  (SELECT id FROM technologies WHERE name = 'Инерциальный блок')),
        (v_fire,  (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_fire,  (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_fire,  (SELECT id FROM technologies WHERE name = 'Активная подвеска')),
        (v_fire,  (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_fire,  (SELECT id FROM technologies WHERE name = 'Инерциальный блок')),
        (v_h2,    (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_h2,    (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_h2,    (SELECT id FROM technologies WHERE name = 'Активная подвеска')),
        (v_h2,    (SELECT id FROM technologies WHERE name = 'Инерциальный блок')),
        (v_gsxr,  (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_gsxr,  (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_mt,    (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_mt,    (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_mt,    (SELECT id FROM technologies WHERE name = 'Инерциальный блок')),
        (v_sf,    (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_sf,    (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_sf,    (SELECT id FROM technologies WHERE name = 'Активная подвеска')),
        (v_sf,    (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_sf,    (SELECT id FROM technologies WHERE name = 'Инерциальный блок')),
        (v_af,    (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_af,    (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_multi, (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_multi, (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_multi, (SELECT id FROM technologies WHERE name = 'Активная подвеска')),
        (v_multi, (SELECT id FROM technologies WHERE name = 'Радарный круиз-контроль')),
        (v_multi, (SELECT id FROM technologies WHERE name = 'ABS в поворотах')),
        (v_sd,    (SELECT id FROM technologies WHERE name = 'Контроль тяги')),
        (v_sd,    (SELECT id FROM technologies WHERE name = 'Квикшифтер')),
        (v_sd,    (SELECT id FROM technologies WHERE name = 'ABS в поворотах'));
END $$;

INSERT INTO news (title, content, image_url) VALUES
('Новый Yamaha YZF-R1: ещё ближе к треку',
 'Yamaha представила обновлённый YZF-R1 2026 года. Инженеры доработали электронику, добавили новые режимы езды и облегчили конструкцию. Кроссовый рядный четырёхцилиндровый двигатель объёмом 998 см³ развивает 200 лошадиных сил. Мотоцикл получил обновлённую аэродинамику и полноцветный TFT-дисплей с трек-режимом.',
 '/images/news/yzf-r1-news.png'),
('Ducati обновила Panigale V4',
 'Ducati выпустила очередное обновление флагманского Panigale V4. Главные изменения — новая прошивка электроники, увеличенная прижимная сила крыльев и доработанная выхлопная система. Итальянский супербайк по-прежнему остаётся одним из самых мощных серийных мотоциклов в своём классе.',
 '/images/news/panigale-news.png'),
('Радарные системы безопасности на мотоциклах',
 'Всё больше мотоциклов получают радарные ассистенты. Радарный круиз-контроль, слежение за слепыми зонами и предупреждение о столкновении становятся стандартом в премиальном сегменте. Разбираемся, как работают эти технологии и насколько они меняют опыт управления.',
 '/images/news/radar-news.png'),
('Топ-3 самых мощных супербайка 2026 года',
 'Мы собрали тройку самых мощных серийных спортбайков: Kawasaki Ninja H2 (310 л.с.), Ducati Panigale V4 (214 л.с.) и Honda Fireblade SP (215 л.с.). Рассказываем, чем они отличаются и какой стоит выбрать в зависимости от стиля езды.',
 '/images/news/top3-news.png');