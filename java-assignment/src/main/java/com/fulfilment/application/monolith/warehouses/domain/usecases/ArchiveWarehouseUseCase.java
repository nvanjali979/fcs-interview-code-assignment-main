package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ArchiveWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.businessUnitCode == null
        || warehouse.businessUnitCode.isBlank()) {
      throw new WebApplicationException("Business unit code is required", 400);
    }

    LOGGER.infof("Archiving warehouse [buCode=%s]", warehouse.businessUnitCode);

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing == null) {
      LOGGER.warnf("Archive rejected – no active warehouse found for buCode=%s",
          warehouse.businessUnitCode);
      throw new WebApplicationException(
          "Active warehouse not found for BU code: " + warehouse.businessUnitCode, 404);
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    LOGGER.infof("Warehouse archived successfully [buCode=%s]", warehouse.businessUnitCode);
  }
}
