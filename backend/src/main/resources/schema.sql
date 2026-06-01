CREATE TABLE IF NOT EXISTS auth_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128),
    open_id VARCHAR(128) UNIQUE,
    avatar_key VARCHAR(255),
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ban_reason VARCHAR(255),
    real_name VARCHAR(64),
    real_name_phone VARCHAR(20),
    id_card_no VARCHAR(64),
    verified BOOLEAN NOT NULL,
    real_name_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    real_name_reject_reason VARCHAR(255),
    real_name_front_key VARCHAR(255),
    real_name_back_key VARCHAR(255),
    login_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    secondary_verify_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    distribution_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    distribution_opened_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS real_name_face_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(32) NOT NULL UNIQUE,
    jh_order_id VARCHAR(64) UNIQUE,
    real_name VARCHAR(64) NOT NULL,
    id_card_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    fail_reason VARCHAR(255),
    provider VARCHAR(32) NOT NULL DEFAULT 'JUHE_FACE_H5',
    raw_result TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS user_wallet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    available_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    frozen_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_commission DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    title VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    channel VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    related_no VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet_recharge_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recharge_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_trade_type VARCHAR(32),
    payment_prepay_id VARCHAR(128),
    payment_code_url VARCHAR(1024),
    payment_transaction_id VARCHAR(64),
    payment_expire_at TIMESTAMP NULL,
    payment_notified_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS withdraw_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    channel VARCHAR(32) NOT NULL,
    account_name VARCHAR(64) NOT NULL,
    account_no VARCHAR(128) NOT NULL,
    qr_code_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS withdraw_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    account_name VARCHAR(64) NOT NULL,
    account_no VARCHAR(128) NOT NULL,
    qr_code_key VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    reject_reason VARCHAR(255),
    reviewed_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS operation_banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    banner_no VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    image_key VARCHAR(255) NOT NULL,
    link_url VARCHAR(255),
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS operation_shortcut (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shortcut_no VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    icon_key VARCHAR(255),
    link_url VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS operation_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    announcement_no VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    publish_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    config_group_name VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    permissions_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_role_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_admin_role_member (role_id, user_id)
);

CREATE TABLE IF NOT EXISTS studio_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL UNIQUE,
    studio_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    contact_phone VARCHAR(32),
    contact_name VARCHAR(64),
    contact_wechat VARCHAR(64),
    qualification_code VARCHAR(64),
    qualification_material_key VARCHAR(255),
    qualification_note VARCHAR(255),
    review_strategy VARCHAR(32) NOT NULL,
    share_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
    distribution_commission_rate DECIMAL(10,4) NULL,
    active BOOLEAN NOT NULL,
    cooperation_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS studio_withdraw_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(32) NOT NULL UNIQUE,
    studio_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    account_name VARCHAR(64) NOT NULL,
    account_no VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reject_reason VARCHAR(255),
    reviewed_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS studio_operator (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_no VARCHAR(32) NOT NULL UNIQUE,
    studio_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    permissions_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS gun_code_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_key VARCHAR(64) NOT NULL UNIQUE,
    creator VARCHAR(64) NOT NULL,
    source VARCHAR(32),
    badges_text VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS gun_code_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entry_code VARCHAR(64) NOT NULL UNIQUE,
    group_key VARCHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    likes_count INT NOT NULL DEFAULT 0,
    dislikes_count INT NOT NULL DEFAULT 0,
    tags_text VARCHAR(255),
    sort_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS gun_code_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entry_code VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    vote_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_gun_code_vote_user_entry (user_id, entry_code)
);

CREATE TABLE IF NOT EXISTS account_listing (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    listing_no VARCHAR(32) NOT NULL UNIQUE,
    seller_user_id BIGINT NOT NULL,
    seller_nickname VARCHAR(64) NOT NULL,
    seller_type VARCHAR(16) NOT NULL,
    studio_name VARCHAR(100),
    review_strategy VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    province_code VARCHAR(16),
    province_name VARCHAR(64),
    city_code VARCHAR(16),
    city_name VARCHAR(64),
    game_server VARCHAR(64) NOT NULL,
    delivery_method VARCHAR(32) NOT NULL,
    account_level INT NOT NULL,
    rank_name VARCHAR(32) NOT NULL,
    safe_box_level INT NOT NULL,
    haf_currency BIGINT NOT NULL,
    title VARCHAR(512) NOT NULL,
    description TEXT NOT NULL,
    operator_count INT NOT NULL,
    operators_json TEXT NOT NULL,
    weapons_json TEXT NOT NULL,
    weapon_skins_json TEXT NOT NULL,
    other_items TEXT,
    image_keys_json TEXT NOT NULL,
    video_key VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    negotiable BOOLEAN NOT NULL,
    mod_codes_json TEXT,
    publish_attributes_json LONGTEXT,
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    sales_count INT NOT NULL DEFAULT 0,
    cover_image_key VARCHAR(255) NOT NULL,
    suggested_price DECIMAL(10,2) NOT NULL,
    distribution_commission_rate DECIMAL(10,4) NULL,
    estimate_detail TEXT NOT NULL,
    rejection_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL
);

SET @account_listing_add_always_online = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'account_listing'
              AND COLUMN_NAME = 'always_online'
        ),
        'SELECT 1',
        'ALTER TABLE account_listing ADD COLUMN always_online BOOLEAN NOT NULL DEFAULT FALSE AFTER delivery_method'
    )
);
PREPARE account_listing_add_always_online_stmt FROM @account_listing_add_always_online;
EXECUTE account_listing_add_always_online_stmt;
DEALLOCATE PREPARE account_listing_add_always_online_stmt;

CREATE TABLE IF NOT EXISTS trade_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    listing_no VARCHAR(32),
    listing_title VARCHAR(512) NOT NULL,
    listing_summary VARCHAR(255),
    listing_cover_key VARCHAR(255),
    buyer_user_id BIGINT NOT NULL,
    buyer_nickname VARCHAR(64) NOT NULL,
    seller_user_id BIGINT NOT NULL,
    seller_nickname VARCHAR(64) NOT NULL,
    seller_type VARCHAR(16) NOT NULL,
    seller_display_name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32),
    payment_trade_type VARCHAR(32),
    payment_prepay_id VARCHAR(128),
    payment_code_url VARCHAR(1024),
    payment_transaction_id VARCHAR(64),
    payment_expire_at TIMESTAMP NULL,
    payment_notified_at TIMESTAMP NULL,
    item_amount DECIMAL(10,2) NOT NULL,
    service_fee DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    buyer_confirmed_at TIMESTAMP NULL,
    seller_confirmed_at TIMESTAMP NULL,
    extra_items_included TINYINT(1) NOT NULL DEFAULT 0,
    extra_items_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    extra_items_snapshot_json LONGTEXT NULL,
    chat_group_no VARCHAR(32),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    trade_started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    after_sale_at TIMESTAMP NULL,
    after_sale_note VARCHAR(500),
    after_sale_proof_key VARCHAR(255),
    after_sale_handled_at TIMESTAMP NULL,
    refund_requested_at TIMESTAMP NULL,
    refund_reviewed_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    refund_amount DECIMAL(10,2) NULL,
    refund_reason VARCHAR(500),
    refund_review_note VARCHAR(500),
    refund_operator_user_id BIGINT NULL,
    refund_operator_role VARCHAR(32),
    buyer_deleted_at TIMESTAMP NULL,
    seller_deleted_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS listing_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    listing_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_listing_favorite (listing_no, user_id)
);

CREATE TABLE IF NOT EXISTS boosting_service (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_no VARCHAR(32) NOT NULL UNIQUE,
    category_code VARCHAR(32) NOT NULL,
    category_label VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    cycle_code VARCHAR(32) NOT NULL,
    cycle_label VARCHAR(64) NOT NULL,
    guarantee_note VARCHAR(255) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    sales_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    distribution_commission_rate DECIMAL(10,4) NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS distribution_invite_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    promoter_user_id BIGINT NOT NULL,
    invite_code VARCHAR(120) NOT NULL UNIQUE,
    invite_path VARCHAR(255) NOT NULL,
    poster_key VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    invalidated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS distribution_referral (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    promoter_user_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL UNIQUE,
    invite_code VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_channel VARCHAR(32),
    first_paid_order_no VARCHAR(32),
    first_paid_order_type VARCHAR(32),
    registered_at TIMESTAMP NOT NULL,
    effective_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS distribution_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    distribution_no VARCHAR(32) NOT NULL UNIQUE,
    promoter_user_id BIGINT NOT NULL,
    referred_user_id BIGINT NULL,
    buyer_nickname VARCHAR(64) NOT NULL,
    source_order_no VARCHAR(32),
    source_order_type VARCHAR(32),
    source_order_status VARCHAR(32),
    order_amount DECIMAL(12,2) NOT NULL,
    commission_rate DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    commission_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    settled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS boosting_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    service_no VARCHAR(32) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    service_category VARCHAR(64) NOT NULL,
    service_description VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    cycle_label VARCHAR(64) NOT NULL,
    guarantee_note VARCHAR(255) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    buyer_nickname VARCHAR(64) NOT NULL,
    game_region VARCHAR(100) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_password_cipher TEXT NOT NULL,
    character_name VARCHAR(100) NOT NULL,
    special_requirement TEXT,
    status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32),
    progress_percent INT NOT NULL DEFAULT 0,
    progress_summary VARCHAR(255),
    chat_group_no VARCHAR(32),
    after_sale_reason TEXT,
    after_sale_proof_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    canceled_at TIMESTAMP NULL,
    after_sale_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS boosting_progress_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    progress_percent INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS im_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_no VARCHAR(32) NOT NULL UNIQUE,
    scene_code VARCHAR(32) NOT NULL,
    source_order_no VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NULL,
    support_display_name VARCHAR(64) NOT NULL,
    last_message_excerpt VARCHAR(255),
    last_message_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS im_participant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    participant_role VARCHAR(32) NOT NULL,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    joined_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_im_participant (conversation_no, user_id)
);

CREATE TABLE IF NOT EXISTS im_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_no VARCHAR(32) NOT NULL,
    sender_role VARCHAR(32) NOT NULL,
    sender_user_id BIGINT NULL,
    sender_name VARCHAR(64) NOT NULL,
    message_type VARCHAR(16) NOT NULL,
    content_text TEXT,
    file_key VARCHAR(255),
    file_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS im_support_read_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_no VARCHAR(32) NOT NULL UNIQUE,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO studio_profile (owner_user_id, studio_name, review_strategy, active, created_at, updated_at)
SELECT 2, '星火工作室', 'DIRECT_PUBLISH', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM studio_profile WHERE owner_user_id = 2
);

INSERT INTO operation_banner (banner_no, title, image_key, link_url, sort_no, status, created_at, updated_at)
SELECT 'BN202604180001', '春季账号租赁节', 'delta-trade/demo/banner-1.png', '/home', 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM operation_banner WHERE banner_no = 'BN202604180001');

INSERT INTO operation_banner (banner_no, title, image_key, link_url, sort_no, status, created_at, updated_at)
SELECT 'BN202604180002', '代肝服务保障公告', 'delta-trade/demo/banner-2.png', '/boosting', 20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM operation_banner WHERE banner_no = 'BN202604180002');

INSERT INTO operation_shortcut (shortcut_no, name, icon_key, link_url, sort_no, status, created_at, updated_at)
SELECT 'SC202604180001', '热门账号', 'delta-trade/demo/shortcut-hot.png', '/home', 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM operation_shortcut WHERE shortcut_no = 'SC202604180001');

INSERT INTO operation_shortcut (shortcut_no, name, icon_key, link_url, sort_no, status, created_at, updated_at)
SELECT 'SC202604180002', '代肝服务', 'delta-trade/demo/shortcut-boosting.png', '/boosting', 20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM operation_shortcut WHERE shortcut_no = 'SC202604180002');

INSERT INTO operation_announcement (announcement_no, title, content, category, pinned, status, publish_at, created_at, updated_at)
SELECT 'AN202604180001', '平台公告：账号发布审核规则', '普通个人用户发布账号默认进入人工审核，工作室是否免审由平台单独配置。', 'SYSTEM', TRUE, 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM operation_announcement WHERE announcement_no = 'AN202604180001');

INSERT INTO operation_announcement (announcement_no, title, content, category, pinned, status, publish_at, created_at, updated_at)
SELECT 'AN202604180002', '活动公告：春季代肝满减', '哈夫币代肝与安全箱代肝服务已开放组合优惠，具体以订单页展示为准。', 'ACTIVITY', FALSE, 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM operation_announcement WHERE announcement_no = 'AN202604180002');

INSERT INTO admin_role (role_code, role_name, description, permissions_json, status, created_at, updated_at)
SELECT 'SUPER_ADMIN', '超级管理员', '拥有平台全部后台能力', '["dashboard","listing","order","studio","boosting","withdraw","operation","support","user","realName","role"]', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM admin_role WHERE role_code = 'SUPER_ADMIN');

INSERT INTO admin_role (role_code, role_name, description, permissions_json, status, created_at, updated_at)
SELECT 'OPS_ADMIN', '运营管理员', '负责运营配置、账号审核与公告管理', '["dashboard","listing","operation","boosting"]', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM admin_role WHERE role_code = 'OPS_ADMIN');

INSERT INTO admin_role (role_code, role_name, description, permissions_json, status, created_at, updated_at)
SELECT 'SERVICE_ADMIN', '客服管理员', '负责消息、IM 与售后协同', '["dashboard","order","support","user"]', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM admin_role WHERE role_code = 'SERVICE_ADMIN');

INSERT INTO admin_role (role_code, role_name, description, permissions_json, status, created_at, updated_at)
SELECT 'FINANCE_ADMIN', '财务管理员', '负责提现审核、工作室分润结算', '["dashboard","withdraw","studio"]', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM admin_role WHERE role_code = 'FINANCE_ADMIN');

INSERT INTO boosting_service (service_no, category_code, category_label, name, description, price, cycle_code, cycle_label, guarantee_note, provider_type, provider_name, sales_count, status, sort_no, created_at, updated_at)
SELECT 'BS202604200001', 'RANK', '段位冲分', '王牌段位冲分包', '适合已具备稳定战绩的账号，承接星级冲分与赛季保段。', 168.00, 'D1', '24 小时内', '代肝过程全程可追踪，异常战绩波动支持售后复核。', 'STUDIO', '速通互联工作室', 18, 'ACTIVE', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM boosting_service WHERE service_no = 'BS202604200001');

INSERT INTO boosting_service (service_no, category_code, category_label, name, description, price, cycle_code, cycle_label, guarantee_note, provider_type, provider_name, sales_count, status, sort_no, created_at, updated_at)
SELECT 'BS202604200002', 'CURRENCY', '哈夫币代刷', '哈夫币速刷服务', '适合日常搬砖与活动冲刺，按单结算，支持分段交付进度。', 128.00, 'D2', '48 小时内', '平台客服跟进进度，如未按约定周期完成可申请补偿。', 'STUDIO', '速通互联工作室', 11, 'ACTIVE', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM boosting_service WHERE service_no = 'BS202604200002');

INSERT INTO boosting_service (service_no, category_code, category_label, name, description, price, cycle_code, cycle_label, guarantee_note, provider_type, provider_name, sales_count, status, sort_no, created_at, updated_at)
SELECT 'BS202604200003', 'SAFEBOX', '安全箱护送', '安全箱护送保分单', '针对安全箱相关玩法提供稳妥护送与保底服务，适合高价值账号。', 218.00, 'D3', '72 小时内', '支持平台介入核查，若因服务方原因失败可发起售后。', 'PLATFORM', '平台直营', 7, 'ACTIVE', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM boosting_service WHERE service_no = 'BS202604200003');
