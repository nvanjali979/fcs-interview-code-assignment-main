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
class ReplaceWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private ReplaceWarehouseUseCase useCase;

  private static final String BU_CODE = "MWH.001";
  private static final Location AMSTERDAM_001 = new Location("AMSTERDAM-001", 5, 100);

  @BeforeEach
  void setUp() {
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private Warehouse existingWarehouse(int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = BU_CODE;
    w.location = "AMSTERDAM-001";
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  private Warehouse newWarehouse(int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = BU_CODE;
    w.location = "AMSTERDAM-001";
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  // ---------------------------------------------------------------------------
  // Positive case
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Happy path")
  class HappyPath {

    @Test
    @DisplayName("archives old warehouse and creates new one when all conditions are met")
    void replace_succeeds_whenAllConditionsMet() {
      // existing: capacity=50, stock=20
      // new:      capacity=40, stock=20  (capacity >= old stock, stock matches, fits in location)
      Warehouse existing = existingWarehouse(50, 20);
      Warehouse newW = newWarehouse(40, 20);

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      // getAll() returns only the existing warehouse (same BU code → filtered out in capacity sum)
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      useCase.replace(newW);

      // Old warehouse must have been archived
      assertNotNull(existing.archivedAt, "Old warehouse must have archivedAt set");
      verify(warehouseStore).update(existing);

      // New warehouse must have been created with createdAt set
      assertNotNull(newW.createdAt, "New warehouse must have createdAt set");
      verify(warehouseStore).create(newW);
    }

    @Test
    @DisplayName("succeeds when new capacity exactly equals old stock (boundary)")
    void replace_succeeds_whenNewCapacityEqualsOldStock() {
      Warehouse existing = existingWarehouse(50, 30);
      Warehouse newW = newWarehouse(30, 30); // capacity == old stock exactly

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      useCase.replace(newW);

      verify(warehouseStore).update(existing);
      verify(warehouseStore).create(newW);
    }

    @Test
    @DisplayName("succeeds when other warehouses exist at the same location but capacity fits")
    void replace_succeeds_whenOtherWarehousesAtLocationAndCapacityFits() {
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(30, 10); // replaces existing; other uses 60 → 60+30=90 ≤ 100

      Warehouse other = new Warehouse();
      other.businessUnitCode = "MWH.002";
      other.location = "AMSTERDAM-001";
      other.capacity = 60;
      other.stock = 5;

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      // getAll() returns both; the replaced one (MWH.001) is excluded from capacity sum
      when(warehouseStore.getAll()).thenReturn(List.of(existing, other));

      useCase.replace(newW);

      verify(warehouseStore).update(existing);
      verify(warehouseStore).create(newW);
    }
  }

  // ---------------------------------------------------------------------------
  // Negative / error cases
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Rule 1 – Active warehouse for BU code must exist")
  class ActiveWarehouseMustExist {

    @Test
    @DisplayName("throws 404 when no active warehouse exists for the given BU code")
    void throws404_whenNoActiveWarehouseFound() {
      Warehouse newW = newWarehouse(30, 10);

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(null);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(404, ex.getResponse().getStatus());
      verify(warehouseStore, never()).update(any());
      verify(warehouseStore, never()).create(any());
    }
  }

  @Nested
  @DisplayName("Rule 2 – New location must be valid")
  class LocationMustBeValid {

    @Test
    @DisplayName("throws 400 when the new warehouse location does not exist")
    void throws400_whenLocationInvalid() {
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(30, 10);

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(null);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).update(any());
      verify(warehouseStore, never()).create(any());
    }
  }

  @Nested
  @DisplayName("Rule 3 – New capacity must accommodate old warehouse stock")
  class CapacityMustAccommodateOldStock {

    @Test
    @DisplayName("throws 400 when new capacity is less than existing stock")
    void throws400_whenNewCapacityLessThanOldStock() {
      // existing stock=25; new capacity=20 → 20 < 25, cannot accommodate
      Warehouse existing = existingWarehouse(50, 25);
      Warehouse newW = newWarehouse(20, 25);

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("throws 400 at boundary: new capacity == old stock - 1")
    void throws400_atCapacityAccommodationBoundary() {
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(9, 10); // 9 < 10

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
    }
  }

  @Nested
  @DisplayName("Rule 4 – New warehouse stock must match old warehouse stock")
  class StockMustMatch {

    @Test
    @DisplayName("throws 400 when new stock is higher than old stock")
    void throws400_whenNewStockHigherThanOldStock() {
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(50, 15); // stock 15 ≠ 10

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("throws 400 when new stock is lower than old stock")
    void throws400_whenNewStockLowerThanOldStock() {
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(50, 5); // stock 5 ≠ 10

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("throws 400 when new stock is zero but old stock is non-zero")
    void throws400_whenNewStockIsZeroAndOldStockIsNonZero() {
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(50, 0);

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
    }
  }

  @Nested
  @DisplayName("Rule 5 – New warehouse capacity must not exceed location max")
  class LocationCapacityNotExceeded {

    @Test
    @DisplayName("throws 400 when new capacity + capacity of other warehouses exceeds location max")
    void throws400_whenNewCapacityExceedsLocationMax() {
      // Location max = 100; other warehouse uses 80; new wants 30 → 80+30=110 > 100
      Warehouse existing = existingWarehouse(50, 10);
      Warehouse newW = newWarehouse(30, 10);

      Warehouse other = new Warehouse();
      other.businessUnitCode = "MWH.002";
      other.location = "AMSTERDAM-001";
      other.capacity = 80;
      other.stock = 5;

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      // existing (MWH.001) is filtered out; only other (MWH.002) contributes capacity
      when(warehouseStore.getAll()).thenReturn(List.of(existing, other));

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("throws 400 at exact boundary: other capacity + new capacity == maxCapacity + 1")
    void throws400_atLocationCapacityBoundary() {
      // other = 70; new = 31 → 101 > 100
      Warehouse existing = existingWarehouse(50, 5);
      Warehouse newW = newWarehouse(31, 5);

      Warehouse other = new Warehouse();
      other.businessUnitCode = "MWH.002";
      other.location = "AMSTERDAM-001";
      other.capacity = 70;
      other.stock = 2;

      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
      when(warehouseStore.getAll()).thenReturn(List.of(existing, other));

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.replace(newW));

      assertEquals(400, ex.getResponse().getStatus());
    }
  }
}
