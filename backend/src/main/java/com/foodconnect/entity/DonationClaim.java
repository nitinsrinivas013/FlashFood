package com.foodconnect.entity;

import com.foodconnect.entity.enums.PickupStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents an NGO's claim on a donation.
 * The unique constraint on donation_id ensures each donation can only be claimed once.
 */
@Entity
@Table(name = "donation_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false, unique = true)
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngo_id", nullable = false)
    private NgoProfile ngo;

    @Column(name = "accepted_at", nullable = false)
    @Builder.Default
    private LocalDateTime acceptedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_status", nullable = false)
    @Builder.Default
    private PickupStatus pickupStatus = PickupStatus.ASSIGNED;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
