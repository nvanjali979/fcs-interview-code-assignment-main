package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CreateWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  // Reusable test location: 5 warehouses allowed, max total capacity 100
  private static final Location AMSTERDAM_001 = new Location("AMSTERDAM-001", 5, 100);

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

 
  /** Returns a warehouse that passes all validations when the location list is empty. */
  private Warehouse validWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "NEW.001";
    w.location = "AMSTERDAM-001";
    w.capacity = 20;
    w.stock = 10;
    return w;
  }

  private Warehouse activeWarehouseAtLocation(String location, int capacity) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "EXISTING.001";
    w.location = location;
    w.capacity = capacity;
    w.stock = 5;
    return w;
  }

  // ---------------------------------------------------------------------------
  // Positive case
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Happy path")
  class HappyPath {

    @Test
    @DisplayName("creates warehouse and sets createdAt when all conditions are met")
    void createSucceeds_whenAllConditionsMet() {
      Warehouse warehouse = validWarehouse();

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of()); // no existing warehouses

      useCase.create(warehouse);

      verify(warehouseStore).create(warehouse);
      assertNotNull(warehouse.createdAt, "createdAt must be set by the use-case");
    }

    @Test
    @DisplayName("creates warehouse alongside an existing warehouse at the same location")
    void createSucceeds_whenLocationHasRoomForMoreWarehouses() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 30;
      // Location allows 5 warehouses, max capacity 100; existing uses 30 → 30+30=60 ≤ 100
      Warehouse existing = activeWarehouseAtLocation("AMSTERDAM-001", 30);

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      useCase.create(warehouse);

      verify(warehouseStore).create(warehouse);
    }

    @Test
    @DisplayName("creates warehouse when stock equals capacity (boundary: stock == capacity)")
    void createSucceeds_whenStockEqualsCapacity() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 15;
      warehouse.stock = 15; // exactly at capacity, not over

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of());

      useCase.create(warehouse);

      verify(warehouseStore).create(warehouse);
    }
  }

  // ---------------------------------------------------------------------------
  // Negative / error cases
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Rule 1 – Business Unit Code must be unique among active warehouses")
  class BuCodeUniqueness {

    @Test
    @DisplayName("throws 409 when an active warehouse with the same BU code already exists")
    void throws409_whenBuCodeAlreadyActive() {
      Warehouse warehouse = validWarehouse();

      Warehouse existing = new Warehouse();
      existing.businessUnitCode = "NEW.001";
      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(existing);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(409, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }
  }

  @Nested
  @DisplayName("Rule 2 – Location must be valid")
  class LocationValidation {

    @Test
    @DisplayName("throws 400 when the location identifier is not recognised")
    void throws400_whenLocationNotFound() {
      Warehouse warehouse = validWarehouse();

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(null);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }
  }

  @Nested
  @DisplayName("Rule 3 – Max warehouses per location must not be exceeded")
  class MaxWarehousesPerLocation {

    @Test
    @DisplayName("throws 400 when location is already at max warehouse count")
    void throws400_whenMaxWarehousesReached() {
      Warehouse warehouse = validWarehouse();
      // Location allows only 1 warehouse; one already exists
      Location singleSlotLocation = new Location("AMSTERDAM-001", 1, 100);
      Warehouse existing = activeWarehouseAtLocation("AMSTERDAM-001", 10);

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(singleSlotLocation);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("throws 400 when location is at max – boundary: count == maxNumberOfWarehouses")
    void throws400_whenCountEqualsMax() {
      Warehouse warehouse = validWarehouse();
      Location twoSlotLocation = new Location("AMSTERDAM-001", 2, 200);
      Warehouse e1 = activeWarehouseAtLocation("AMSTERDAM-001", 10);
      Warehouse e2 = activeWarehouseAtLocation("AMSTERDAM-001", 10);
      e2.businessUnitCode = "EXISTING.002";

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(twoSlotLocation);
      when(warehouseStore.getAll()).thenReturn(List.of(e1, e2));

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
    }
  }

  @Nested
  @DisplayName("Rule 4 – Total capacity at location must not exceed location max")
  class TotalCapacityAtLocation {

    @Test
    @DisplayName("throws 400 when adding new capacity would exceed location max")
    void throws400_whenCapacityExceedsLocationMax() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 90; // 90 + existing 30 = 120 > 100
      Warehouse existing = activeWarehouseAtLocation("AMSTERDAM-001", 30);

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("throws 400 at exact boundary: existing + new == maxCapacity + 1")
    void throws400_atCapacityBoundary() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 1;                          // existing 100 + 1 = 101 > 100
      Warehouse existing = activeWarehouseAtLocation("AMSTERDAM-001", 100);

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("succeeds at exact boundary: existing + new == maxCapacity")
    void succeeds_whenTotalCapacityEqualsMax() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 60; // existing 40 + 60 = 100 == maxCapacity → OK
      warehouse.stock = 5;
      Warehouse existing = activeWarehouseAtLocation("AMSTERDAM-001", 40);

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      useCase.create(warehouse);

      verify(warehouseStore).create(warehouse);
    }
  }

  @Nested
  @DisplayName("Rule 5 – Warehouse capacity must accommodate declared stock")
  class StockCapacityConstraint {

    @Test
    @DisplayName("throws 400 when stock exceeds capacity")
    void throws400_whenStockExceedsCapacity() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 5;
      warehouse.stock = 10; // 10 > 5

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of());

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("throws 400 at boundary: stock == capacity + 1")
    void throws400_atStockCapacityBoundary() {
      Warehouse warehouse = validWarehouse();
      warehouse.capacity = 9;
      warehouse.stock = 10;

      when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of());

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

      assertEquals(400, ex.getResponse().getStatus());
    }
  }
}
