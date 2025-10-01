package com.bikeshare.lab3;

import com.bikeshare.model.Bike;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class BikeStructuralTest {

    private Bike standardBike;
    private Bike electricBike;
    private Bike premiumBike;

    @BeforeEach
    void setUp() {
        standardBike = new Bike("B001", Bike.BikeType.STANDARD);
        electricBike = new Bike("E001", Bike.BikeType.ELECTRIC);
        premiumBike = new Bike("P001", Bike.BikeType.PREMIUM);
    }

    // constructor

    @Test
    @DisplayName("Constructor should initialize bike with correct values")
    void constructorInitializesBikeCorrectly() {
        assertAll(
            () -> assertEquals("B001", standardBike.getBikeId()),
            () -> assertEquals(Bike.BikeType.STANDARD, standardBike.getType()),
            () -> assertEquals(Bike.BikeStatus.AVAILABLE, standardBike.getStatus()),
            () -> assertEquals(-1.0, standardBike.getBatteryLevel()),
            () -> assertEquals(0, standardBike.getTotalRides()),
            () -> assertEquals(0.0, standardBike.getTotalDistance()),
            () -> assertNotNull(standardBike.getLastMaintenanceDate()),
            () -> assertFalse(standardBike.needsMaintenance()),
            () -> assertTrue(standardBike.isAvailable()),
            () -> assertEquals(100.0, electricBike.getBatteryLevel())
        );
    }

    @Test
    @DisplayName("Constructor should validate inputs")
    void constructorValidatesInputs() {
        assertThrows(IllegalArgumentException.class, () -> new Bike(null, Bike.BikeType.STANDARD));
        assertThrows(IllegalArgumentException.class, () -> new Bike("", Bike.BikeType.STANDARD));
        assertThrows(IllegalArgumentException.class, () -> new Bike("  ", Bike.BikeType.STANDARD));
        assertThrows(IllegalArgumentException.class, () -> new Bike("B002", null));
    }

    @Test
    @DisplayName("Constructor should trim bike ID")
    void constructorTrimsBikeId() {
        Bike bike = new Bike("  B002  ", Bike.BikeType.STANDARD);
        assertEquals("B002", bike.getBikeId());
    }

    // reserve()

    @Test
    @DisplayName("Reserve bike changes status to RESERVED")
    void reserveBikeChangesStatusToReserved() {
        standardBike.reserve();
        assertEquals(Bike.BikeStatus.RESERVED, standardBike.getStatus());
    }

    @Test
    @DisplayName("Cannot reserve bike in non-AVAILABLE status")
    void cannotReserveBikeInNonAvailableStatus() {
        standardBike.reserve();
        assertThrows(IllegalStateException.class, () -> standardBike.reserve());
    }

    // startRide()

    @Test
    @DisplayName("Start ride changes status to IN_USE")
    void startRideChangesStatusToInUse() {
        standardBike.startRide();
        assertAll(
            () -> assertEquals(Bike.BikeStatus.IN_USE, standardBike.getStatus()),
            () -> assertNotNull(standardBike.getLastUsedDate()));
    }

    @Test
    @DisplayName("Cannot start ride with bike in incorrect status")
    void cannotStartRideWithBikeInIncorrectStatus() {
        // Test BROKEN state
        standardBike.markAsBroken();
        assertThrows(IllegalStateException.class, () -> standardBike.startRide());

        // Test IN_USE state
        standardBike = new Bike("B001", Bike.BikeType.STANDARD);
        standardBike.startRide();
        assertThrows(IllegalStateException.class, () -> standardBike.startRide());

        // Test MAINTENANCE state
        standardBike = new Bike("B001", Bike.BikeType.STANDARD);
        standardBike.sendToMaintenance();
        assertThrows(IllegalStateException.class, () -> standardBike.startRide());
    }

    @Test
    @DisplayName("Cannot start ride with electric bike with low battery")
    void cannotStartRideWithElectricBikeLowBattery() {
        electricBike.startRide();
        electricBike.endRide(47.5); // Set battery to below 10%
        assertThrows(IllegalStateException.class, () -> electricBike.startRide());
    }

    // endRide()

    @Test
    @DisplayName("End ride updates bike statistics correctly")
    void endRideUpdatesBikeStatisticsCorrectly() {
        standardBike.startRide();
        standardBike.endRide(10.5);

        assertEquals(Bike.BikeStatus.AVAILABLE, standardBike.getStatus());
        assertEquals(1, standardBike.getTotalRides());
        assertEquals(10.5, standardBike.getTotalDistance());
    }

    @Test
    @DisplayName("End ride decreases electric bike battery")
    void endRideDecreasesElectricBikeBattery() {
        electricBike.startRide();
        electricBike.endRide(10.0);

        assertEquals(80.0, electricBike.getBatteryLevel()); // 2% per km
    }

    @Test
    @DisplayName("End ride with large distance does not make battery negative")
    void endRideWithLargeDistanceDoesNotMakeBatteryNegative() {
        electricBike.startRide();
        electricBike.endRide(10000.0);

        assertEquals(0.0, electricBike.getBatteryLevel());
    }

    @Test
    @DisplayName("Cannot end ride for bike not in use")
    void cannotEndRideForBikeNotInUse() {
        assertThrows(IllegalStateException.class, () -> standardBike.endRide(10.0));
    }

    @Test
    @DisplayName("Cannot end ride with negative distance")
    void cannotEndRideWithNegativeDistance() {
        standardBike.startRide();
        assertThrows(IllegalArgumentException.class, () -> standardBike.endRide(-5.0));
    }

    // sendToMaintenance()

    @Test
    @DisplayName("Send to maintenance changes status correctly")
    void sendToMaintenanceChangesStatusCorrectly() {
        standardBike.sendToMaintenance();
        assertEquals(Bike.BikeStatus.MAINTENANCE, standardBike.getStatus());
    }

    @Test
    @DisplayName("Cannot send bike to maintenance while in use")
    void cannotSendBikeToMaintenanceWhileInUse() {
        standardBike.startRide();
        assertThrows(IllegalStateException.class, () -> standardBike.sendToMaintenance());
    }

    // completeMaintenance()

    @Test
    @DisplayName("Complete maintenance resets status and flags")
    void completeMaintenanceResetsStatusAndFlags() {
        standardBike.markAsBroken();
        standardBike.sendToMaintenance();
        LocalDateTime oldMaintenanceDate = standardBike.getLastMaintenanceDate();

        standardBike.completeMaintenance();

        assertAll(
            () -> assertEquals(Bike.BikeStatus.AVAILABLE, standardBike.getStatus()),
            () -> assertFalse(standardBike.needsMaintenance()),
            () -> assertTrue(standardBike.getLastMaintenanceDate().isAfter(oldMaintenanceDate))
        );
    }

    @Test
    @DisplayName("Complete maintenance of electric bike restores battery")
    void completeMaintenanceOfElectricBikeRestoresBattery() {
        electricBike.startRide();
        electricBike.endRide(1000.0); // Completely drain battery
        electricBike.sendToMaintenance();
        electricBike.completeMaintenance();

        assertEquals(100.0, electricBike.getBatteryLevel());
    }

    @Test
    @DisplayName("Cannot complete maintenance for bike not in maintenance")
    void cannotCompleteMaintenanceForBikeNotInMaintenance() {
        assertThrows(IllegalStateException.class, () -> standardBike.completeMaintenance());
    }

    // markAsBroken()

    @Test
    @DisplayName("Mark as broken changes status")
    void markAsBrokenChangesStatus() {
        standardBike.markAsBroken();
        assertEquals(Bike.BikeStatus.BROKEN, standardBike.getStatus());
        assertTrue(standardBike.needsMaintenance());
    }

    // chargeBattery()

    @Test
    @DisplayName("Charge battery works correctly")
    void chargeBatteryWorksCorrectly() {
        electricBike.startRide();
        electricBike.endRide(10.0); // Set to 80%
        electricBike.chargeBattery(20);
        assertEquals(100.0, electricBike.getBatteryLevel());
    }

    @Test
    @DisplayName("Cannot charge non-electric bike")
    void cannotChargeNonElectricBike() {
        assertThrows(IllegalStateException.class, () -> standardBike.chargeBattery(20));
    }

    @Test
    @DisplayName("Cannot charge with invalid amount")
    void cannotChargeWithInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () -> electricBike.chargeBattery(-10));
        assertThrows(IllegalArgumentException.class, () -> electricBike.chargeBattery(110));
    }

    // checkMaintenanceRequirement()

    @Test
    @DisplayName("Maintenance required after 100 rides")
    void maintenanceRequiredAfter100Rides() {
        // Simulate 100 rides
        for (int i = 0; i < 100; i++) {
            standardBike.startRide();
            standardBike.endRide(1.0);
        }

        assertTrue(standardBike.needsMaintenance());
    }

    @Test
    @DisplayName("Maintenance required after 1000km")
    void maintenanceRequiredAfter1000km() {
        standardBike.startRide();
        standardBike.endRide(1000.0);

        assertTrue(standardBike.needsMaintenance());
    }

    @Test
    @DisplayName("Electric bike needs maintenance when battery too low")
    void electricBikeNeedsMaintenanceWhenBatteryTooLow() {
        electricBike.startRide();
        electricBike.endRide(50.0); // 100% - (50*2%) = 0%

        assertTrue(electricBike.needsMaintenance());
    }

    // various getters are tested implicitly in other tests

    // setCurrentStationId() and getCurrentStationId()

    @Test
    @DisplayName("Setting and getting station ID works correctly")
    void settingAndGettingStationIdWorksCorrectly() {
        standardBike.setCurrentStationId("S001");
        assertEquals("S001", standardBike.getCurrentStationId());
    }

    // isAvailable()

    @Test
    @DisplayName("Bike is not available when needs maintenance")
    void bikeIsNotAvailableWhenNeedsMaintenance() {
        standardBike.startRide();
        standardBike.endRide(1000.0); // Triggers maintenance required

        assertFalse(standardBike.isAvailable());
    }

    // equals()

    @Test
    @DisplayName("Bike equals method works correctly")
    void bikeEqualsMethodWorksCorrectly() {
        Bike bike1 = new Bike("B001", Bike.BikeType.STANDARD);
        Bike bike2 = new Bike("B001", Bike.BikeType.ELECTRIC);
        Bike bike3 = new Bike("B002", Bike.BikeType.STANDARD);

        assertAll(
            () -> assertEquals(bike1, standardBike),
            () -> assertEquals(bike1, bike2),
            () -> assertNotEquals(bike1, bike3),
            () -> assertNotEquals(bike1, null),
            () -> assertNotEquals(bike1, "Not a bike")
        );
    }

    // hashCode()

    @Test
    @DisplayName("Bike hashCode is consistent")
    void bikeHashCodeIsConsistent() {
        Bike bike1 = new Bike("B001", Bike.BikeType.STANDARD);

        assertEquals(bike1.hashCode(), standardBike.hashCode());
    }

    // toString()

    @Test
    @DisplayName("Bike toString returns expected format")
    void bikeToStringReturnsExpectedFormat() {
        String expected = "Bike{id='B001', type=STANDARD, status=AVAILABLE, battery=-1.0%, rides=0}";
        assertEquals(expected, standardBike.toString());
    }

    // enum BikeType

    @ParameterizedTest
    @ValueSource(strings = {"STANDARD", "ELECTRIC", "PREMIUM"})
    @DisplayName("Bike type enum values are correct")
    void bikeTypeEnumValuesAreCorrect(String typeName) {
        Bike.BikeType type = Bike.BikeType.valueOf(typeName);

        switch (type) {
            case STANDARD -> assertAll(
                () -> assertEquals("Standard", type.getDisplayName()),
                () -> assertEquals(0.0, type.getPriceMultiplier()));
            case ELECTRIC -> assertAll(
                () -> assertEquals("Electric", type.getDisplayName()),
                () -> assertEquals(1.5, type.getPriceMultiplier()));
            case PREMIUM -> assertAll(
                () -> assertEquals("Premium", type.getDisplayName()),
                () -> assertEquals(2.0, type.getPriceMultiplier()));
        }
    }
}
