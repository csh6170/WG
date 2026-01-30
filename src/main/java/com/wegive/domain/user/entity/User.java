package com.wegive.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * [Entity] USERS 테이블과 1:1 매핑되는 자바 객체
 * 역할: 회원 정보를 담고 있는 DB 원본 데이터
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙상 기본생성자 필수 (외부 호출 방지)
@Table(name = "USERS")
public class User {

    @Id // PK 설정
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_users_gen") // Oracle Sequence 사용
    @SequenceGenerator(name = "seq_users_gen", sequenceName = "SEQ_USERS", allocationSize = 1)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email; // 로그인 ID 역할

    @Column(name = "NICKNAME", nullable = false, length = 50)
    private String nickname;

    @Column(name = "PROFILE_IMG", length = 300)
    private String profileImg;

    @Column(name = "PROVIDER", length = 20)
    private String provider; // KAKAO, NAVER 등

    @Column(name = "PROVIDER_ID", length = 100)
    private String providerId;

    @Column(name = "REFRESH_TOKEN", length = 300)
    private String refreshToken; // 자동 로그인용 토큰 (DB 저장)

    @Column(name = "MANNER_TEMP", columnDefinition = "NUMBER(4,1) DEFAULT 36.5")
    private Double mannerTemp;

    // --- 위치 정보 ---
    @Column(name = "MY_LAT", columnDefinition = "NUMBER(10, 7)")
    private Double myLat; // 위도

    @Column(name = "MY_LON", columnDefinition = "NUMBER(10, 7)")
    private Double myLon; // 경도

    @Column(name = "ADDRESS_NAME", length = 100)
    private String addressName; // 행정동 이름 (예: 역삼동)
    // ----------------

    @Column(name = "ROLE", length = 20)
    private String role; // USER, ADMIN

    @Column(name = "USER_STATUS", length = 20)
    private String userStatus; // ACTIVE, LEFT

    @CreationTimestamp // INSERT 시 자동 시간 저장
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // UPDATE 시 자동 시간 갱신
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // Builder 패턴 생성자 (객체 생성 시 안전성 보장)
    @Builder
    public User(String email, String nickname, String provider, String providerId, String profileImg,
                String role, String userStatus, Double mannerTemp) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImg = profileImg;
        // 들어온 값이 있으면 그걸 쓰고, 없으면(null) 기본값 사용
        this.role = (role != null) ? role : "USER";
        this.userStatus = (userStatus != null) ? userStatus : "ACTIVE";
        this.mannerTemp = (mannerTemp != null) ? mannerTemp : 36.5;
    }

    // [비즈니스 메서드] 리프레시 토큰 변경 (로그인/로그아웃 시 사용)
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // [비즈니스 메서드] 프로필 및 위치 정보 수정 (동네 인증 시 사용)
    public void updateProfile(String nickname, Double lat, Double lon, String address) {
        if (nickname != null) this.nickname = nickname;
        if (lat != null) this.myLat = lat;
        if (lon != null) this.myLon = lon;
        if (address != null) this.addressName = address; // 카카오에서 받아온 주소 저장
    }
    /**
     * [추가] 소셜 로그인 시 닉네임과 프로필 사진 최신화
     */
    public void updateSocialInfo(String nickname, String profileImg) {
        this.nickname = nickname;
        this.profileImg = profileImg;
    }
    // 매너온도 변경 (증가/감소 통합)
    public void changeMannerTemp(double amount) {
        if (this.mannerTemp == null) {
            this.mannerTemp = 36.5; // 초기값이 없을 경우 방어
        }

        this.mannerTemp += amount;

        // 최대 99.9도, 최소 0도 제한
        if (this.mannerTemp > 99.9) {
            this.mannerTemp = 99.9;
        } else if (this.mannerTemp < 0.0) {
            this.mannerTemp = 0.0;
        }
    }

    /**
     * [관리자용] 회원 상태 변경 (정지 <-> 활성)
     */
    public void changeStatus(String newStatus) {
        this.userStatus = newStatus;

    }
}