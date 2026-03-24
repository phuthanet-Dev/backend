package com.mysticcard.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_astrology_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAstrologyProfile {

    @Id
    @Column(name = "user_id")
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @JsonIgnore
    private User user;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_time")
    private LocalTime birthTime;

    @Column(name = "zodiac_sign", length = 50)
    private String zodiacSign;

    @Column(length = 20)
    private String element;

    @Column(name = "focus_area", length = 50)
    private String focusArea;

    @Column(name = "preferred_color_tone", length = 50)
    private String preferredColorTone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
