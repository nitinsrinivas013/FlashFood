package com.foodconnect.service;

import com.foodconnect.config.AppConfig;
import com.foodconnect.dto.CreateDonationRequest;
import com.foodconnect.dto.DonationClaimResponse;
import com.foodconnect.dto.DonationResponse;
import com.foodconnect.entity.*;
import com.foodconnect.entity.enums.*;
import com.foodconnect.exception.ConflictException;
import com.foodconnect.exception.DonationNotAvailableException;
import com.foodconnect.exception.UnauthorizedActionException;
import com.foodconnect.mapper.DonationClaimMapper;
import com.foodconnect.mapper.DonationMapper;
import com.foodconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private DonationRepository donationRepository;
    @Mock private DonationClaimRepository donationClaimRepository;
    @Mock private RestaurantProfileRepository restaurantProfileRepository;
    @Mock private NgoProfileRepository ngoProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private DonationMapper donationMapper;
    @Mock private DonationClaimMapper donationClaimMapper;
    @Mock private NotificationService notificationService;
    @Mock private AppConfig appConfig;

    @InjectMocks
    private DonationService donationService;

    private User restaurantUser;
    private User ngoUser;
    private RestaurantProfile restaurant;
    private NgoProfile ngo;
    private Donation availableDonation;

    @BeforeEach
    void setUp() {
        restaurantUser = User.builder().id(1L).name("Restaurant Owner")
                .email("rest@test.com").role(Role.RESTAURANT).build();
        ngoUser = User.builder().id(2L).name("NGO Contact")
                .email("ngo@test.com").role(Role.NGO).build();

        restaurant = RestaurantProfile.builder().id(1L).user(restaurantUser)
                .restaurantName("Test Restaurant")
                .latitude(BigDecimal.valueOf(28.6139)).longitude(BigDecimal.valueOf(77.2090)).build();

        ngo = NgoProfile.builder().id(1L).user(ngoUser).ngoName("Test NGO")
                .latitude(BigDecimal.valueOf(28.6200)).longitude(BigDecimal.valueOf(77.2100)).build();

        availableDonation = Donation.builder()
                .id(1L).restaurant(restaurant).foodName("Rice")
                .quantity(BigDecimal.valueOf(10)).quantityUnit(QuantityUnit.KG)
                .status(DonationStatus.AVAILABLE)
                .pickupDeadline(LocalDateTime.now().plusHours(4))
                .availableUntil(LocalDateTime.now().plusHours(3))
                .latitude(BigDecimal.valueOf(28.6139)).longitude(BigDecimal.valueOf(77.2090))
                .address("123 Test St").build();
    }

    @Test
    void createDonation_byRestaurant_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(restaurantUser));
        when(restaurantProfileRepository.findByUserId(1L)).thenReturn(Optional.of(restaurant));
        when(donationRepository.save(any())).thenReturn(availableDonation);
        when(donationMapper.toResponse(any(Donation.class))).thenReturn(
                DonationResponse.builder().id(1L).foodName("Rice").build());

        CreateDonationRequest request = CreateDonationRequest.builder()
                .foodName("Rice").quantity(BigDecimal.valueOf(10))
                .quantityUnit(QuantityUnit.KG)
                .availableUntil(LocalDateTime.now().plusHours(3))
                .pickupDeadline(LocalDateTime.now().plusHours(4))
                .latitude(28.6139).longitude(77.2090).address("123 Test St")
                .build();

        DonationResponse response = donationService.createDonation(1L, request);
        assertThat(response).isNotNull();
        assertThat(response.getFoodName()).isEqualTo("Rice");
        verify(donationRepository).save(any());
    }

    @Test
    void createDonation_byNgo_throwsUnauthorized() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));

        CreateDonationRequest request = CreateDonationRequest.builder()
                .foodName("Rice").quantity(BigDecimal.valueOf(10))
                .quantityUnit(QuantityUnit.KG)
                .availableUntil(LocalDateTime.now().plusHours(3))
                .pickupDeadline(LocalDateTime.now().plusHours(4))
                .latitude(28.6139).longitude(77.2090).address("123 Test St")
                .build();

        assertThatThrownBy(() -> donationService.createDonation(2L, request))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void acceptDonation_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(ngoProfileRepository.findByUserId(2L)).thenReturn(Optional.of(ngo));
        when(donationRepository.findById(1L)).thenReturn(Optional.of(availableDonation));
        when(donationClaimRepository.existsByDonationId(1L)).thenReturn(false);
        when(donationRepository.updateStatusAtomically(1L, DonationStatus.AVAILABLE, DonationStatus.ACCEPTED))
                .thenReturn(1);
        when(donationClaimRepository.save(any())).thenReturn(
                DonationClaim.builder().id(1L).donation(availableDonation).ngo(ngo).build());
        when(donationClaimMapper.toResponse(any())).thenReturn(
                DonationClaimResponse.builder().id(1L).donationId(1L).ngoId(1L).build());

        DonationClaimResponse response = donationService.acceptDonation(2L, 1L);
        assertThat(response).isNotNull();
        assertThat(response.getDonationId()).isEqualTo(1L);
    }

    @Test
    void acceptDonation_alreadyAccepted_throwsConflict() {
        availableDonation.setStatus(DonationStatus.ACCEPTED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(ngoProfileRepository.findByUserId(2L)).thenReturn(Optional.of(ngo));
        when(donationRepository.findById(1L)).thenReturn(Optional.of(availableDonation));

        assertThatThrownBy(() -> donationService.acceptDonation(2L, 1L))
                .isInstanceOf(DonationNotAvailableException.class);
    }

    @Test
    void acceptDonation_raceLost_throwsConflict() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(ngoProfileRepository.findByUserId(2L)).thenReturn(Optional.of(ngo));
        when(donationRepository.findById(1L)).thenReturn(Optional.of(availableDonation));
        when(donationClaimRepository.existsByDonationId(1L)).thenReturn(false);
        // Atomic update returns 0 = another NGO won the race
        when(donationRepository.updateStatusAtomically(1L, DonationStatus.AVAILABLE, DonationStatus.ACCEPTED))
                .thenReturn(0);

        assertThatThrownBy(() -> donationService.acceptDonation(2L, 1L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptDonation_expiredDeadline_throwsException() {
        availableDonation.setPickupDeadline(LocalDateTime.now().minusHours(1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(ngoProfileRepository.findByUserId(2L)).thenReturn(Optional.of(ngo));
        when(donationRepository.findById(1L)).thenReturn(Optional.of(availableDonation));

        assertThatThrownBy(() -> donationService.acceptDonation(2L, 1L))
                .isInstanceOf(DonationNotAvailableException.class);
    }

    @Test
    void acceptDonation_byRestaurant_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(restaurantUser));

        assertThatThrownBy(() -> donationService.acceptDonation(1L, 1L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void acceptDonation_ownDonation_throwsUnauthorized() {
        // Restaurant user tries to accept own donation via an NGO account (same user id)
        User dualUser = User.builder().id(3L).name("Dual").email("dual@test.com").role(Role.NGO).build();
        NgoProfile dualNgo = NgoProfile.builder().id(2L).user(dualUser).ngoName("Dual NGO").build();
        // Donation owned by user 3 (same user)
        RestaurantProfile restProfile = RestaurantProfile.builder().id(2L).user(dualUser).build();
        Donation ownDonation = Donation.builder().id(2L).restaurant(restProfile)
                .status(DonationStatus.AVAILABLE)
                .pickupDeadline(LocalDateTime.now().plusHours(4))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(dualUser));
        when(ngoProfileRepository.findByUserId(3L)).thenReturn(Optional.of(dualNgo));
        when(donationRepository.findById(2L)).thenReturn(Optional.of(ownDonation));

        assertThatThrownBy(() -> donationService.acceptDonation(3L, 2L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void cancelDonation_byOwner_success() {
        when(donationRepository.findById(1L)).thenReturn(Optional.of(availableDonation));
        when(donationRepository.save(any())).thenReturn(availableDonation);
        when(donationMapper.toResponse(any(Donation.class))).thenReturn(
                DonationResponse.builder().id(1L).status(DonationStatus.CANCELLED).build());

        DonationResponse response = donationService.cancelDonation(1L, 1L);
        assertThat(response.getStatus()).isEqualTo(DonationStatus.CANCELLED);
    }

    @Test
    void cancelDonation_byOtherUser_throwsUnauthorized() {
        when(donationRepository.findById(1L)).thenReturn(Optional.of(availableDonation));

        assertThatThrownBy(() -> donationService.cancelDonation(99L, 1L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void calculateDistance_knownPoints() {
        // Delhi to Mumbai (approximate)
        double distance = DonationService.calculateDistance(28.6139, 77.2090, 19.0760, 72.8777);
        assertThat(distance).isBetween(1140.0, 1160.0); // ~1150 km
    }

    @Test
    void calculateDistance_samePoint_returnsZero() {
        double distance = DonationService.calculateDistance(28.6139, 77.2090, 28.6139, 77.2090);
        assertThat(distance).isEqualTo(0.0);
    }
}
