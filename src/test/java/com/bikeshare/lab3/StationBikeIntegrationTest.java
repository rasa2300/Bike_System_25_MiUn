package com.bikeshare.lab3;

import com.bikeshare.model.Bike;
import com.bikeshare.model.Bike.BikeStatus;
import com.bikeshare.model.Bike.BikeType;
import com.bikeshare.model.Station;
import com.bikeshare.model.Station.StationStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StationBikeIntegrationTest {

    private Station st(String id, int cap) {
        return new Station(id, "Name", "Address", 59.83, 12.06, cap);
    }

    @Test
    void add_and_remove() {
        Station s = st("S1", 2);
        Bike b1 = new Bike("B1", BikeType.STANDARD);
        Bike b2 = new Bike("B2", BikeType.ELECTRIC);

        s.addBike(b1);
        s.addBike(b2);
        assertEquals("S1", b1.getCurrentStationId());
        assertEquals("S1", b2.getCurrentStationId());
        assertEquals(StationStatus.FULL, s.getStatus());

        // error
        assertThrows(IllegalStateException.class, () -> s.addBike(new Bike("B3", BikeType.STANDARD)));

        // remove one
        Bike removed = s.removeBike("B1");
        assertSame(b1, removed);
        assertNull(removed.getCurrentStationId());
    }

    @Test
    void checkout_then_return() {
        Station s = st("S2", 1);
        Bike b = new Bike("B10", BikeType.STANDARD);
        s.addBike(b);

        // checkout
        Bike rented = s.removeBike("B10");
        rented.startRide();
        assertEquals(BikeStatus.IN_USE, rented.getStatus());
        assertEquals(StationStatus.EMPTY, s.getStatus());

        // return
        rented.endRide(0.5);
        s.addBike(rented);
        assertEquals(BikeStatus.AVAILABLE, rented.getStatus());
        assertEquals(StationStatus.FULL, s.getStatus());
    }

    @Test
    void getAvailableBike() {
        Station s = st("S4", 3);
        Bike e1 = new Bike("E1", BikeType.ELECTRIC);
        Bike s1 = new Bike("S1", BikeType.STANDARD);
        s.addBike(e1);
        s.addBike(s1);

        // reserve
        s.reserveBike("E1");

        // if ELECTRIC not available
        Bike pickElectric = s.getAvailableBike(BikeType.ELECTRIC);
        assertNotNull(pickElectric);
        assertEquals(BikeType.STANDARD, pickElectric.getType());

        // if STANDARD
        Bike pickStandard = s.getAvailableBike(BikeType.STANDARD);
        assertNotNull(pickStandard);
        assertEquals(BikeType.STANDARD, pickStandard.getType());
    }

    @Test
    void chargingElectricBikes() {
        Station s = st("S4", 1);
        Bike e = new Bike("E2", BikeType.ELECTRIC);
        s.addBike(e);

        assertThrows(IllegalStateException.class, () -> s.chargeElectricBikes(10));

        s.enableCharging(5.0);
        s.chargeElectricBikes(10.0);

        s.disableCharging();
        assertThrows(IllegalStateException.class, () -> s.chargeElectricBikes(10));
    }
}
