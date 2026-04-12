package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    validateInput(warehouse);

    LOGGER.infof("Creating warehouse [buCode=%s, location=%s]",
        warehouse.businessUnitCode, warehouse.location);

    // 1. Business Unit Code must not already be in active use
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      LOGGER.warnf("Warehouse creation rejected – BU code already active: %s",
          warehouse.businessUnitCode);
      throw new WebApplicationException(
          "Business unit code already in use: " + warehouse.businessUnitCode, 409);
    }

    // 2. Location must be valid
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      LOGGER.warnf("Warehouse creation rejected – unknown location: %s", warehouse.location);
      throw new WebApplicationException("Invalid location: " + warehouse.location, 400);
    }

    // 3. Max number of warehouses at this location must not be exceeded
    long activeAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location))
            .count();
    if (activeAtLocation >= location.maxNumberOfWarehouses) {
      LOGGER.warnf("Warehouse creation rejected – max warehouses (%d) reached at %s",
          location.maxNumberOfWarehouses, warehouse.location);
      throw new WebApplicationException(
          "Maximum number of warehouses reached for location: " + warehouse.location, 400);
    }

    // 4. Total capacity at location (including new) must not exceed location max capacity
    int existingCapacity =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location))
            .mapToInt(w -> w.capacity)
            .sum();
    if (existingCapacity + warehouse.capacity > location.maxCapacity) {
      LOGGER.warnf(
          "Warehouse creation rejected – capacity %d + existing %d exceeds max %d at %s",
          warehouse.capacity, existingCapacity, location.maxCapacity, warehouse.location);
      throw new WebApplicationException(
          "New warehouse capacity would exceed the maximum capacity for location: "
              + warehouse.location,
          400);
    }

    // 5. Warehouse capacity must be able to handle the declared stock
    if (warehouse.stock > warehouse.capacity) {
      LOGGER.warnf("Warehouse creation rejected – stock %d exceeds capacity %d",
          warehouse.stock, warehouse.capacity);
      throw new WebApplicationException("Stock cannot exceed warehouse capacity", 400);
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);

    LOGGER.infof("Warehouse created successfully [buCode=%s]", warehouse.businessUnitCode);
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
