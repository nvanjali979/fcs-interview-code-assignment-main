package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ArchiveWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;

  private ArchiveWarehouseUseCase useCase;

  private static final String BU_CODE = "MWH.001";

  @BeforeEach
  void setUp() {
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private Warehouse activeWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = BU_CODE;
    w.location = "AMSTERDAM-001";
    w.capacity = 50;
    w.stock = 20;
    return w;
  }

  /** Minimal placeholder carrying only the BU code – mirrors the pattern used by the resource. */
  private Warehouse placeholder(String buCode) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    return w;
  }

  // ---------------------------------------------------------------------------
  // Positive cases
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Happy path")
  class HappyPath {

    @Test
    @DisplayName("sets archivedAt on the existing warehouse and persists it")
    void archive_setsArchivedAt_whenWarehouseExists() {
      Warehouse existing = activeWarehouse();
      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);

      LocalDateTime before = LocalDateTime.now().minusSeconds(1);

      useCase.archive(placeholder(BU_CODE));

      // archivedAt must be set to roughly now
      assertNotNull(existing.archivedAt, "archivedAt must be populated");
      assertTrue(
          !existing.archivedAt.isBefore(before),
          "archivedAt should not be earlier than the test start");

      verify(warehouseStore).update(existing);
    }

    @Test
    @DisplayName("passes the warehouse with archivedAt already set to update()")
    void archive_passesCorrectObjectToUpdate() {
      Warehouse existing = activeWarehouse();
      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);

      useCase.archive(placeholder(BU_CODE));

      ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
      verify(warehouseStore).update(captor.capture());

      Warehouse persisted = captor.getValue();
      assertEquals(BU_CODE, persisted.businessUnitCode);
      assertNotNull(persisted.archivedAt, "Captured warehouse must have archivedAt set");
    }

    @Test
    @DisplayName("does not alter other fields (location, capacity, stock) on the warehouse")
    void archive_doesNotAlterOtherFields() {
      Warehouse existing = activeWarehouse();
      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);

      useCase.archive(placeholder(BU_CODE));

      assertEquals("AMSTERDAM-001", existing.location);
      assertEquals(50, existing.capacity);
      assertEquals(20, existing.stock);
    }

    @Test
    @DisplayName("does not call create() – archiving must never create a new record")
    void archive_neverCallsCreate() {
      Warehouse existing = activeWarehouse();
      when(warehouseStore.findByBusinessUnitCode(BU_CODE)).thenReturn(existing);

      useCase.archive(placeholder(BU_CODE));

      verify(warehouseStore, never()).create(any());
    }
  }

  // ---------------------------------------------------------------------------
  // Error cases
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Error cases")
  class ErrorCases {

    @Test
    @DisplayName("throws 404 when no active warehouse exists for the given BU code")
    void throws404_whenWarehouseNotFound() {
      when(warehouseStore.findByBusinessUnitCode("MISSING.999")).thenReturn(null);

      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class, () -> useCase.archive(placeholder("MISSING.999")));

      assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("does not call update() when the warehouse is not found")
    void doesNotCallUpdate_whenWarehouseNotFound() {
      when(warehouseStore.findByBusinessUnitCode("MISSING.999")).thenReturn(null);

      assertThrows(
          WebApplicationException.class, () -> useCase.archive(placeholder("MISSING.999")));

      verify(warehouseStore, never()).update(any());
    }

    @Test
    @DisplayName("throws 404 for an empty BU code that resolves to nothing")
    void throws404_whenBuCodeIsEmptyString() {
      when(warehouseStore.findByBusinessUnitCode("")).thenReturn(null);

      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> useCase.archive(placeholder("")));

      assertEquals(404, ex.getResponse().getStatus());
    }
  }
}
