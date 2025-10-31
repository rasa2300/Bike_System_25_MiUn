package com.bikeshare.lab6;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bikeshare.model.Bike;
import com.bikeshare.model.Station;
import com.bikeshare.repository.BikeRepository;
import com.bikeshare.service.BikeService;
import com.bikeshare.service.exception.BikeNotAvailableException;

public class BikeServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @InjectMocks
    private BikeService bikeService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRentBike_Success() {
        String bikeId = "BIKE-123";
        Bike bike = new Bike(bikeId, Bike.BikeType.STANDARD);
        when(bikeRepository.findById(bikeId)).thenReturn(java.util.Optional.of(bike));

        bikeService.rentBike(bikeId, "USER-456");

        assertEquals(Bike.BikeStatus.IN_USE, bike.getStatus());
        verify(bikeRepository).save(bike);
    }

    @Test
    public void testRentBike_BikeNotAvailable() {
        String bikeId = "BIKE-123";
        Bike bike = new Bike(bikeId, Bike.BikeType.STANDARD);
        bike.startRide(); // Make bike unavailable
        when(bikeRepository.findById(bikeId)).thenReturn(java.util.Optional.of(bike));

        assertThrows(BikeNotAvailableException.class, () -> {
            bikeService.rentBike(bikeId, "USER-456");
        });
    }

    @Test
    public void testReturnBike_Success() {
        String bikeId = "BIKE-123";
        Bike bike = new Bike(bikeId, Bike.BikeType.STANDARD);
        bike.startRide();
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn("STATION-1");
        when(station.isFull()).thenReturn(false);
        when(bikeRepository.findById(bikeId)).thenReturn(java.util.Optional.of(bike));

        bikeService.returnBike(bikeId, station);

        assertTrue(bike.isAvailable());
        assertEquals("STATION-1", bike.getCurrentStationId());
        verify(bikeRepository).save(bike);
    }

    @Test
    public void testFindBikeById_Available() {
        String bikeId = "BIKE-123";
        Bike bike = new Bike(bikeId, Bike.BikeType.STANDARD);
        when(bikeRepository.findById(bikeId)).thenReturn(java.util.Optional.of(bike));

        Bike foundBike = bikeService.findBikeById(bikeId);

        assertTrue(foundBike.isAvailable());
        assertEquals(bikeId, foundBike.getBikeId());
    }
}