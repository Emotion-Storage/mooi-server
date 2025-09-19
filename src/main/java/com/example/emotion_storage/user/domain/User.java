package com.example.emotion_storage.user.domain;

import com.example.emotion_storage.global.entity.BaseTimeEntity;
import com.example.emotion_storage.notification.domain.Notification;
import com.example.emotion_storage.timecapsule.domain.TimeCapsule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialType socialType;

    @Column(nullable = false)
    private String socialId;

    @Column(unique = true)
    private String email;

    private String profileImageUrl;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(nullable = false)
    private Long keyCount;

    @Column(nullable = false)
    private Long ticketCount;

    @ElementCollection
    @CollectionTable(name = "user_expectations", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "expectation", nullable = false, length = 100)
    @Builder.Default
    private List<String> expectations = new ArrayList<>(); // 추후 확인 필요

    @Column(nullable = false)
    private boolean isTermsAgreed;

    @Column(nullable = false)
    private boolean isPrivacyAgreed;

    @Column(nullable = false)
    private boolean isMarketingAgreed;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimeCapsule> timeCapsules = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    public void addTimeCapsule(TimeCapsule timeCapsule) {
        this.timeCapsules.add(timeCapsule);
        timeCapsule.setUser(this);
    }

    public void removeTimeCapsule(TimeCapsule timeCapsule) {
        this.timeCapsules.remove(timeCapsule);
        timeCapsule.setUser(null);
    }

    public void addNotification(Notification notification) {
        this.notifications.add(notification);
        notification.setUser(this);
    }

    public void removeNotification(Notification notification) {
        this.notifications.remove(notification);
        notification.setUser(null);
    }

    public void useKeys(Long keyCount) {
        this.keyCount -= keyCount;
    }
}
