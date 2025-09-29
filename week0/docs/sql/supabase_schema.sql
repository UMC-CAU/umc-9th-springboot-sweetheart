-- UMC Spring Week0 Mission - Supabase Database Schema
-- 음식 배달 & 미션 앱 데이터베이스 생성 스크립트

-- =============================================
-- 1. 기본 테이블 생성 (FK 의존성 없는 테이블들)
-- =============================================

-- 지역 관리 테이블
CREATE TABLE regions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 카테고리 관리 테이블
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 사용자 관리 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    user_type VARCHAR(20) CHECK (user_type IN ('normal', 'business')) DEFAULT 'normal',
    points INTEGER DEFAULT 0 CHECK (points >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true
);

-- =============================================
-- 2. FK 의존성 있는 테이블들 (순서대로 생성)
-- =============================================

-- 가게 관리 테이블 (users, regions 참조)
CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    address TEXT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    phone VARCHAR(20),
    business_hours TEXT,
    avg_rating DECIMAL(3, 2) DEFAULT 0.0 CHECK (avg_rating >= 0 AND avg_rating <= 5),
    total_reviews INTEGER DEFAULT 0 CHECK (total_reviews >= 0),
    status VARCHAR(20) CHECK (status IN ('active', 'inactive', 'suspended')) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 가게-카테고리 연결 테이블
CREATE TABLE store_categories (
    store_id BIGINT REFERENCES stores(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (store_id, category_id)
);

-- 메뉴 관리 테이블 (stores 참조)
CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    image_url VARCHAR(255),
    is_available BOOLEAN DEFAULT true,
    is_signature BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 주문 관리 테이블 (users, stores 참조)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    store_id BIGINT REFERENCES stores(id) ON DELETE CASCADE,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount > 0),
    delivery_fee DECIMAL(10, 2) DEFAULT 0 CHECK (delivery_fee >= 0),
    delivery_address TEXT NOT NULL,
    delivery_phone VARCHAR(20),
    status VARCHAR(20) CHECK (status IN ('pending', 'confirmed', 'preparing', 'delivering', 'delivered', 'cancelled')) DEFAULT 'pending',
    payment_method VARCHAR(50),
    special_requests TEXT,
    ordered_at TIMESTAMPTZ DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 주문 상세 테이블 (orders, menu_items 참조)
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id BIGINT REFERENCES menu_items(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price > 0),
    total_price DECIMAL(10, 2) NOT NULL CHECK (total_price > 0),
    special_requests TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 리뷰 관리 테이블 (users, stores, orders 참조)
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    store_id BIGINT REFERENCES stores(id) ON DELETE CASCADE,
    order_id BIGINT UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    rating DECIMAL(2, 1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    content TEXT,
    is_public BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 리뷰 이미지 관리 테이블 (reviews 참조)
CREATE TABLE review_images (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
    image_url VARCHAR(255) NOT NULL,
    image_order INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 미션 관리 테이블 (stores 참조)
CREATE TABLE missions (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT REFERENCES stores(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    reward_points INTEGER NOT NULL CHECK (reward_points > 0),
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0 CHECK (current_participants >= 0),
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    status VARCHAR(20) CHECK (status IN ('active', 'paused', 'completed', 'cancelled')) DEFAULT 'active',
    mission_type VARCHAR(20) CHECK (mission_type IN ('visit', 'order', 'review')) DEFAULT 'visit',
    requirements JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT valid_date_range CHECK (end_date > start_date)
);

-- 사용자 미션 참여 테이블 (users, missions 참조)
CREATE TABLE user_missions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    mission_id BIGINT REFERENCES missions(id) ON DELETE CASCADE,
    status VARCHAR(20) CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'cancelled')) DEFAULT 'pending',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    earned_points INTEGER DEFAULT 0 CHECK (earned_points >= 0),
    progress_data JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, mission_id)
);

-- 포인트 내역 테이블 (users 참조)
CREATE TABLE point_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    points INTEGER NOT NULL,
    transaction_type VARCHAR(50) CHECK (transaction_type IN ('mission_reward', 'order_payment', 'refund')) NOT NULL,
    description TEXT,
    related_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 알림 관리 테이블 (users 참조)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) CHECK (type IN ('mission', 'order', 'review', 'system')) NOT NULL,
    related_id BIGINT,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    read_at TIMESTAMPTZ
);

-- =============================================
-- 3. 인덱스 생성 (성능 최적화)
-- =============================================

-- 지역별 가게 검색 최적화
CREATE INDEX idx_stores_region_status ON stores(region_id, status);

-- 사용자별 주문 조회 최적화
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

-- 사용자별 미션 조회 최적화
CREATE INDEX idx_user_missions_user_status ON user_missions(user_id, status);

-- 가게별 리뷰 조회 최적화
CREATE INDEX idx_reviews_store_rating ON reviews(store_id, rating);

-- 리뷰 이미지 조회 최적화
CREATE INDEX idx_review_images_review_order ON review_images(review_id, image_order);

-- 가게별 미션 조회 최적화
CREATE INDEX idx_missions_store_status ON missions(store_id, status);

-- 포인트 거래 내역 조회 최적화
CREATE INDEX idx_point_transactions_user_date ON point_transactions(user_id, created_at);

-- 알림 조회 최적화
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);

-- 휴대폰 번호 고유 인덱스 (NULL 허용)
CREATE UNIQUE INDEX users_phone_unique ON users(phone) WHERE phone IS NOT NULL;

-- =============================================
-- 4. Supabase 특화 기능
-- =============================================

-- updated_at 자동 업데이트 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- updated_at 트리거 생성
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stores_updated_at BEFORE UPDATE ON stores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_menu_items_updated_at BEFORE UPDATE ON menu_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_missions_updated_at BEFORE UPDATE ON missions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_missions_updated_at BEFORE UPDATE ON user_missions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 미션 참여자 수 자동 업데이트 함수
CREATE OR REPLACE FUNCTION update_mission_participants()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE missions SET current_participants = current_participants + 1 
        WHERE id = NEW.mission_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE missions SET current_participants = current_participants - 1 
        WHERE id = OLD.mission_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

-- 미션 참여자 수 자동 업데이트 트리거
CREATE TRIGGER trigger_update_mission_participants
    AFTER INSERT OR DELETE ON user_missions
    FOR EACH ROW EXECUTE FUNCTION update_mission_participants();

-- 가게 평점 자동 업데이트 함수
CREATE OR REPLACE FUNCTION update_store_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE stores SET 
        avg_rating = COALESCE((SELECT AVG(rating) FROM reviews WHERE store_id = COALESCE(NEW.store_id, OLD.store_id)), 0),
        total_reviews = (SELECT COUNT(*) FROM reviews WHERE store_id = COALESCE(NEW.store_id, OLD.store_id))
    WHERE id = COALESCE(NEW.store_id, OLD.store_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

-- 가게 평점 자동 업데이트 트리거
CREATE TRIGGER trigger_update_store_rating
    AFTER INSERT OR UPDATE OR DELETE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_store_rating();

-- =============================================
-- 5. Row Level Security (RLS) 설정
-- =============================================

-- RLS 활성화
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_missions ENABLE ROW LEVEL SECURITY;
ALTER TABLE point_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- 기본 RLS 정책 (사용자는 자신의 데이터만 접근 가능)
CREATE POLICY "Users can view own profile" ON users
    FOR SELECT USING (auth.uid()::text = id::text);

CREATE POLICY "Users can update own profile" ON users
    FOR UPDATE USING (auth.uid()::text = id::text);

CREATE POLICY "Users can view own orders" ON orders
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can view own reviews" ON reviews
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can view own missions" ON user_missions
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can view own point transactions" ON point_transactions
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users can view own notifications" ON notifications
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- 공개 데이터 정책 (모든 사용자가 조회 가능)
CREATE POLICY "Everyone can view stores" ON stores
    FOR SELECT USING (true);

CREATE POLICY "Everyone can view categories" ON categories
    FOR SELECT USING (true);

CREATE POLICY "Everyone can view regions" ON regions
    FOR SELECT USING (true);

CREATE POLICY "Everyone can view menu items" ON menu_items
    FOR SELECT USING (true);

CREATE POLICY "Everyone can view missions" ON missions
    FOR SELECT USING (true);

-- =============================================
-- 6. 샘플 데이터 삽입 (선택사항)
-- =============================================

-- 지역 데이터
INSERT INTO regions (name, code, latitude, longitude) VALUES
('서울특별시', 'SEOUL', 37.5665, 126.9780),
('부산광역시', 'BUSAN', 35.1796, 129.0756),
('인천광역시', 'INCHEON', 37.4563, 126.7052);

-- 카테고리 데이터
INSERT INTO categories (name, description) VALUES
('한식', '한국 전통 음식'),
('중식', '중국 음식'),
('일식', '일본 음식'),
('양식', '서양 음식'),
('치킨', '치킨 전문점'),
('피자', '피자 전문점'),
('카페', '카페 및 디저트');

-- =============================================
-- 완료 메시지
-- =============================================

-- 스키마 생성 완료
SELECT 'UMC Spring Week0 Mission Database Schema 생성 완료!' as status;