package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    validateInput(newWarehouse);

    LOGGER.infof("Replacing warehouse [buCode=%s, newLocation=%s]",
        newWarehouse.businessUnitCode, newWarehouse.location);

    // 1. Find the currently active warehouse to be replaced
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      LOGGER.warnf("Warehouse replacement rejected – no active warehouse for buCode=%s",
          newWarehouse.businessUnitCode);
      throw new WebApplicationException(
          "Active warehouse not found for BU code: " + newWarehouse.businessUnitCode, 404);
    }

    // 2. New warehouse location must be valid
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      LOGGER.warnf("Warehouse replacement rejected – unknown location: %s", newWarehouse.location);
      throw new WebApplicationException("Invalid location: " + newWarehouse.location, 400);
    }

    // 3. New warehouse capacity must accommodate the stock from the replaced warehouse
    if (newWarehouse.capacity < existing.stock) {
      LOGGER.warnf(
          "Warehouse replacement rejected – new capacity %d cannot accommodate existing stock %d",
          newWarehouse.capacity, existing.stock);
      throw new WebApplicationException(
          "New warehouse capacity must accommodate the existing stock of " + existing.stock, 400);
    }

    // 4. Stock of the new warehouse must match the stock of the replaced warehouse
    if (!newWarehouse.stock.equals(existing.stock)) {
      LOGGER.warnf(
          "Warehouse replacement rejected – stock mismatch: new=%d, existing=%d",
          newWarehouse.stock, existing.stock);
      throw new WebApplicationException(
          "New warehouse stock must match the existing stock of " + existing.stock, 400);
    }

    // 5. Capacity at the new location must not be exceeded (excluding the replaced warehouse
    //    when it shares the same location, since it will be archived)
    int occupiedCapacity =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.location.equals(newWarehouse.location)
                        && !w.businessUnitCode.equals(newWarehouse.businessUnitCode))
            .mapToInt(w -> w.capacity)
            .sum();
    if (occupiedCapacity + newWarehouse.capacity > location.maxCapacity) {
      LOGGER.warnf(
          "Warehouse replacement rejected – capacity %d + occupied %d exceeds max %d at %s",
          newWarehouse.capacity, occupiedCapacity, location.maxCapacity, newWarehouse.location);
      throw new WebApplicationException(
          "New warehouse capacity would exceed the maximum capacity for location: "
              + newWarehouse.location,
          400);
    }

    // Archive the current warehouse
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
    LOGGER.infof("Warehouse archived [buCode=%s]", existing.businessUnitCode);

    // Create the replacement warehouse with the same BU code
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
    LOGGER.infof("Replacement warehouse created [buCode=%s, location=%s]",
        newWarehouse.businessUnitCode, newWarehouse.location);
  }

  /** Guards against null or structurally invalid input before any business logic runs. */
  private void validateInput(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse payload must not be null", 400);
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WebApplicationException("Business unit code is required", 400);
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WebApplicationException("Location is required", 400);
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WebApplicationException("Capacity must be a positive integer", 400);
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WebApplicationException("Stock must be zero or a positive integer", 400);
    }
  }
}
