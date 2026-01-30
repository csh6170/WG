/* ===============================================================
   [초기화 섹션]
   기존 테이블과 시퀀스가 있다면 삭제하여 깨끗한 상태로 만듭니다.
   (처음 실행 시 "존재하지 않습니다" 에러는 무시하셔도 됩니다.)
   =============================================================== */
DROP TABLE WISHLISTS CASCADE CONSTRAINTS;
DROP TABLE REPORTS CASCADE CONSTRAINTS;
DROP TABLE REQUESTS CASCADE CONSTRAINTS;
DROP TABLE ITEM_IMAGES CASCADE CONSTRAINTS;
DROP TABLE ITEMS CASCADE CONSTRAINTS;
DROP TABLE USERS CASCADE CONSTRAINTS;
DROP TABLE CHAT_MESSAGE CASCADE CONSTRAINTS;
DROP TABLE CHAT_ROOM CASCADE CONSTRAINTS;

DROP SEQUENCE SEQ_WISHLISTS;
DROP SEQUENCE SEQ_REPORTS;
DROP SEQUENCE SEQ_REQUESTS;
DROP SEQUENCE SEQ_ITEM_IMAGES;
DROP SEQUENCE SEQ_ITEMS;
DROP SEQUENCE SEQ_USERS;
DROP SEQUENCE SEQ_CHAT_MESSAGE;
DROP SEQUENCE SEQ_CHAT_ROOM;

/* ===============================================================
   [1. 회원 (USERS)]
   소셜 로그인 정보를 기반으로 회원을 관리하는 테이블입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_USERS NOCACHE; -- 회원 ID 자동 발급기

CREATE TABLE USERS (
    USER_ID          NUMBER PRIMARY KEY,            -- 회원 고유 번호 (PK)
    EMAIL            VARCHAR2(100) NOT NULL UNIQUE, -- 이메일 (소셜에서 제공받음, ID찾기 및 연락용, 중복불가)
    NICKNAME         VARCHAR2(50) NOT NULL,         -- 닉네임 (화면에 표시될 이름)
    PROFILE_IMG      VARCHAR2(300),                 -- 프로필 이미지 URL (소셜 프로필 혹은 변경 가능)
    PROVIDER         VARCHAR2(20),                  -- 가입 경로 (예: 'KAKAO', 'NAVER')
    PROVIDER_ID      VARCHAR2(100),                 -- 소셜 서비스에서 주는 고유 식별자 (비밀번호 대신 사용)

    REFRESH_TOKEN    VARCHAR2(300),                 -- JWT 리프레시 토큰 (자동 로그인 유지용)
    MANNER_TEMP      NUMBER(4,1) DEFAULT 36.5,      -- 매너 온도 (기본 36.5도, 활동에 따라 증감)

    MY_LAT           NUMBER(10, 7),                 -- 나의 동네 위도 (내 주변 물건 찾기용)
    MY_LON           NUMBER(10, 7),                 -- 나의 동네 경도
    ADDRESS_NAME     VARCHAR2(100),                 -- 사람이 읽는 주소 (예: '서울시 강남구 역삼동')

    ROLE             VARCHAR2(20) DEFAULT 'USER',   -- 회원 등급 (USER:일반, ADMIN:관리자)
    USER_STATUS      VARCHAR2(20) DEFAULT 'ACTIVE', -- 계정 상태 (ACTIVE:활동중, BANNED:정지, LEFT:탈퇴)

    CREATED_AT       DATE DEFAULT SYSDATE,          -- 가입 일시
    UPDATED_AT       DATE DEFAULT SYSDATE           -- 정보 수정 일시 (닉네임 변경 등 추적)
);

/* ===============================================================
   [2. 상품 (ITEMS)]
   무료 나눔 물품 정보를 저장하는 핵심 테이블입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_ITEMS NOCACHE; -- 상품 ID 자동 발급기

CREATE TABLE ITEMS (
    ITEM_ID          NUMBER PRIMARY KEY,            -- 상품 고유 번호 (PK)
    SELLER_ID        NUMBER NOT NULL,               -- 나눔이(나눔 실천자) ID (USERS 테이블 참조)

    BUYER_ID         NUMBER,                        -- [중요] 최종 당첨자(구매자) ID (나눔 완료 시 업데이트됨)

    TITLE            VARCHAR2(200) NOT NULL,        -- 게시글 제목

    -- 카테고리 (데이터 오염 방지를 위해 정해진 값만 입력 가능하도록 제약 설정)
    CATEGORY         VARCHAR2(50),
    CONSTRAINT CK_ITEMS_CATEGORY CHECK (CATEGORY IN ('전자기기', '가구/인테리어', '의류', '도서', '생활용품', '스포츠/레저', '기타')),

    DESCRIPTION      CLOB,                          -- 상세 설명 (긴 글 저장을 위해 CLOB 사용)

    ITEM_LAT         NUMBER(10, 7) NOT NULL,        -- 거래 희망 장소 위도
    ITEM_LON         NUMBER(10, 7) NOT NULL,        -- 거래 희망 장소 경도
    ADDRESS_NAME     VARCHAR2(100) NOT NULL,        -- 거래 희망 장소 주소명 (지도 API 최소화용)

    OPEN_CHAT_URL    VARCHAR2(300) NOT NULL,        -- 오픈채팅방 링크 (필수 입력, 유일한 소통 창구)

    -- 상품 상태 (AVAILABLE:나눔중, RESERVED:예약중, COMPLETED:완료, HIDDEN:숨김)
    STATUS           VARCHAR2(20) DEFAULT 'AVAILABLE',

    STOCK            NUMBER DEFAULT 1,              -- 재고 수량 (보통 1개)
    VIEW_COUNT       NUMBER DEFAULT 0,              -- 조회수

    CREATED_AT       DATE DEFAULT SYSDATE,          -- 작성 일시
    UPDATED_AT       DATE DEFAULT SYSDATE,          -- 수정 일시 (끌어올리기 등)

    -- 외래키 연결
    CONSTRAINT FK_ITEMS_SELLER FOREIGN KEY (SELLER_ID) REFERENCES USERS(USER_ID),
    CONSTRAINT FK_ITEMS_BUYER  FOREIGN KEY (BUYER_ID)  REFERENCES USERS(USER_ID)
);

/* ===============================================================
   [3. 상품 이미지 (ITEM_IMAGES)]
   하나의 상품에 여러 이미지를 첨부하기 위한 테이블입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_ITEM_IMAGES NOCACHE; -- 이미지 ID 자동 발급기

CREATE TABLE ITEM_IMAGES (
    IMG_ID           NUMBER PRIMARY KEY,            -- 이미지 고유 번호 (PK)
    ITEM_ID          NUMBER NOT NULL,               -- 어떤 상품의 이미지인지 (ITEMS 테이블 참조)
    ORIGINAL_NAME    VARCHAR2(200),                 -- 사용자가 올린 원래 파일명 (예: my_photo.jpg)
    STORED_NAME      VARCHAR2(200),                 -- 서버에 저장된 암호화된 파일명 (예: uuid_2819.jpg)
    IS_THUMBNAIL     CHAR(1) DEFAULT 'N',           -- 대표 이미지 여부 (Y:대표, N:일반 - Boolean 대용)

    CREATED_AT       DATE DEFAULT SYSDATE,          -- 업로드 일시

    CONSTRAINT CK_IMG_THUMB CHECK (IS_THUMBNAIL IN ('Y', 'N')),
    CONSTRAINT FK_IMG_ITEM FOREIGN KEY (ITEM_ID) REFERENCES ITEMS(ITEM_ID) ON DELETE CASCADE
);

/* ===============================================================
   [4. 나눔 신청 (REQUESTS)]
   선착순 나눔을 신청한 내역을 저장합니다. 0.001초 단위 기록이 핵심입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_REQUESTS NOCACHE; -- 신청 ID 자동 발급기

CREATE TABLE REQUESTS (
    REQ_ID           NUMBER PRIMARY KEY,            -- 신청 고유 번호 (PK)
    ITEM_ID          NUMBER NOT NULL,               -- 신청한 상품 ID
    BUYER_ID         NUMBER NOT NULL,               -- 신청한 사람 ID

    -- 신청 상태 (WAITING:대기중, ACCEPTED:수락됨/당첨, REJECTED:거절됨/탈락)
    REQ_STATUS       VARCHAR2(20) DEFAULT 'WAITING',

    REQ_TIME         TIMESTAMP DEFAULT SYSTIMESTAMP,-- 신청 시각 (선착순 판별을 위해 밀리초 단위 TIMESTAMP 사용)
    UPDATED_AT       DATE DEFAULT SYSDATE,          -- 상태 변경 일시 (수락/거절 시점)

    CONSTRAINT FK_REQ_ITEM FOREIGN KEY (ITEM_ID) REFERENCES ITEMS(ITEM_ID),
    CONSTRAINT FK_REQ_BUYER FOREIGN KEY (BUYER_ID) REFERENCES USERS(USER_ID),
    CONSTRAINT UQ_REQ_USER_ITEM UNIQUE (ITEM_ID, BUYER_ID) -- [중복 방지] 한 사람이 같은 물건 두 번 신청 불가
);

/* ===============================================================
   [5. 찜하기 (WISHLISTS)]
   관심 있는 상품을 저장해두는 목록입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_WISHLISTS NOCACHE; -- 찜 ID 자동 발급기

CREATE TABLE WISHLISTS (
    WISH_ID          NUMBER PRIMARY KEY,            -- 찜 고유 번호 (PK)
    USER_ID          NUMBER NOT NULL,               -- 누가 찜했는지
    ITEM_ID          NUMBER NOT NULL,               -- 무엇을 찜했는지
    CREATED_AT       DATE DEFAULT SYSDATE,          -- 찜한 일시

    CONSTRAINT FK_WISH_USER FOREIGN KEY (USER_ID) REFERENCES USERS(USER_ID) ON DELETE CASCADE,
    CONSTRAINT FK_WISH_ITEM FOREIGN KEY (ITEM_ID) REFERENCES ITEMS(ITEM_ID) ON DELETE CASCADE,
    CONSTRAINT UQ_WISH_USER_ITEM UNIQUE (USER_ID, ITEM_ID) -- [중복 방지] 같은 물건 또 찜하기 불가
);

/* ===============================================================
   [6. 신고 내역 (REPORTS)]
   비매너 유저나 사기 게시글을 신고하는 관리용 테이블입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_REPORTS NOCACHE; -- 신고 ID 자동 발급기

CREATE TABLE REPORTS (
    REPORT_ID        NUMBER PRIMARY KEY,            -- 신고 고유 번호 (PK)
    REPORTER_ID      NUMBER NOT NULL,               -- 신고자 ID
    REPORTED_ID      NUMBER NOT NULL,               -- 신고 대상자(피신고자) ID
    ITEM_ID          NUMBER,                        -- 문제의 상품 ID (없을 수도 있음 - 프로필 신고 등)

    REASON           VARCHAR2(50) NOT NULL,         -- 신고 사유 (예: '노쇼', '욕설', '사기')
    DESCRIPTION      VARCHAR2(500),                 -- 신고 상세 내용

    STATUS           VARCHAR2(20) DEFAULT 'PENDING',-- 처리 상태 (PENDING:접수, RESOLVED:처리완료, REJECTED:반려)
    CREATED_AT       DATE DEFAULT SYSDATE,          -- 신고 일시
    UPDATED_AT       DATE DEFAULT SYSDATE,          -- 관리자 처리 일시

    CONSTRAINT FK_RPT_REPORTER FOREIGN KEY (REPORTER_ID) REFERENCES USERS(USER_ID),
    CONSTRAINT FK_RPT_REPORTED FOREIGN KEY (REPORTED_ID) REFERENCES USERS(USER_ID)
);

/* ===============================================================
   [7. 채팅방 (CHAT_ROOM)]
   상품을 매개로 판매자와 구매자가 대화하는 방입니다.
   =============================================================== */
CREATE SEQUENCE SEQ_CHAT_ROOM NOCACHE;

CREATE TABLE CHAT_ROOM (
    ROOM_ID          NUMBER PRIMARY KEY,            -- 채팅방 고유 번호 (PK)
    ITEM_ID          NUMBER NOT NULL,               -- 관련 상품 ID
    SELLER_ID        NUMBER NOT NULL,               -- 나눔이 ID
    BUYER_ID         NUMBER NOT NULL,               -- 신청자 ID
    CREATED_AT       DATE DEFAULT SYSDATE,          -- 생성 일시

    CONSTRAINT FK_ROOM_ITEM   FOREIGN KEY (ITEM_ID)   REFERENCES ITEMS(ITEM_ID) ON DELETE CASCADE,
    CONSTRAINT FK_ROOM_SELLER FOREIGN KEY (SELLER_ID) REFERENCES USERS(USER_ID),
    CONSTRAINT FK_ROOM_BUYER  FOREIGN KEY (BUYER_ID)  REFERENCES USERS(USER_ID)
);

/* ===============================================================
   [8. 채팅 메시지 (CHAT_MESSAGE)]
   실제 주고받은 메시지 내역을 저장합니다.
   =============================================================== */
CREATE SEQUENCE SEQ_CHAT_MESSAGE NOCACHE;

CREATE TABLE CHAT_MESSAGE (
    MESSAGE_ID       NUMBER PRIMARY KEY,            -- 메시지 고유 번호 (PK)
    ROOM_ID          NUMBER NOT NULL,               -- 소속된 채팅방 ID
    SENDER_ID        NUMBER NOT NULL,               -- 보낸 사람 ID
    MESSAGE          VARCHAR2(1000) NOT NULL,       -- 메시지 내용
    SEND_TIME        TIMESTAMP DEFAULT SYSTIMESTAMP, -- 보낸 시각 (정밀도 위해 TIMESTAMP)

    -- 🔴 여기에 IS_READ를 처음부터 포함시켜 생성합니다.
    IS_READ          NUMBER(1) DEFAULT 0 NOT NULL,   -- 읽음 여부 (0: 안읽음, 1: 읽음)

    CONSTRAINT FK_MSG_ROOM   FOREIGN KEY (ROOM_ID)   REFERENCES CHAT_ROOM(ROOM_ID) ON DELETE CASCADE,
    CONSTRAINT FK_MSG_SENDER FOREIGN KEY (SENDER_ID) REFERENCES USERS(USER_ID)
);

/* ===============================================================
   [최종 저장]
   모든 변경 사항을 DB에 영구 반영합니다.
   =============================================================== */
COMMIT;