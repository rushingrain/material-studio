USE material_studio;

INSERT INTO app_user (id, nickname)
VALUES (1, '演示用户')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO template
    (code, name, category, description, cover_url, example_image_url,
     required_image_count, allowed_mime_types, max_image_size_bytes, sort_order, active)
VALUES
    ('portrait-editorial', '柔光人像', '人像写真', '适合单人肖像与自然光氛围的竖版写真模板。',
     '/assets/templates/portrait-cover.svg', '/assets/templates/portrait-sample.svg',
     1, JSON_ARRAY('image/jpeg', 'image/png', 'image/webp'), 10485760, 10, TRUE),
    ('product-studio', '产品静物', '商品展示', '为小型商品生成干净、明亮的棚拍视觉。',
     '/assets/templates/product-cover.svg', '/assets/templates/product-sample.svg',
     2, JSON_ARRAY('image/jpeg', 'image/png'), 15728640, 20, TRUE),
    ('travel-postcard', '旅行明信片', '旅行记录', '将旅行照片整理为简洁的明信片构图。',
     '/assets/templates/travel-cover.svg', '/assets/templates/travel-sample.svg',
     3, JSON_ARRAY('image/jpeg', 'image/webp'), 8388608, 30, TRUE),
    ('food-menu', '餐桌故事', '美食餐饮', '适用于菜品与餐桌场景的横版宣传视觉。',
     '/assets/templates/food-cover.svg', '/assets/templates/food-sample.svg',
     4, JSON_ARRAY('image/jpeg', 'image/png', 'image/webp'), 20971520, 40, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    description = VALUES(description),
    cover_url = VALUES(cover_url),
    example_image_url = VALUES(example_image_url),
    required_image_count = VALUES(required_image_count),
    allowed_mime_types = VALUES(allowed_mime_types),
    max_image_size_bytes = VALUES(max_image_size_bytes),
    sort_order = VALUES(sort_order),
    active = VALUES(active);
