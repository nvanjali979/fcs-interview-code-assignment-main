package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseRepository.class);

  /** Returns only active (non-archived) warehouses. */
  @Override
  public List<Warehouse> getAll() {
    return this.find("archivedAt is null").list().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    LOGGER.debugf("Persisting new warehouse [buCode=%s]", warehouse.businessUnitCode);
    DbWarehouse db = new DbWarehouse();
    db.businessUnitCode = warehouse.businessUnitCode;
    db.location = warehouse.location;
    db.capacity = warehouse.capacity;
    db.stock = warehouse.stock;
    db.createdAt = warehouse.createdAt;
    db.archivedAt = warehouse.archivedAt;
    this.persist(db);
  }

  /**
   * Updates the currently active warehouse record identified by {@code warehouse.businessUnitCode}.
   * JPA dirty-tracking flushes field changes at transaction commit – no explicit merge needed.
   */
  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    LOGGER.debugf("Updating warehouse [buCode=%s]", warehouse.businessUnitCode);
    DbWarehouse db =
        this.find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
            .firstResult();
    if (db == null) {
      throw new WebApplicationException(
          "Active warehouse not found for BU code: " + warehouse.businessUnitCode, 404);
    }
    db.location = warehouse.location;
    db.capacity = warehouse.capacity;
    db.stock = warehouse.stock;
    db.archivedAt = warehouse.archivedAt;
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    LOGGER.debugf("Removing warehouse [buCode=%s]", warehouse.businessUnitCode);
    DbWarehouse db =
        this.find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
            .firstResult();
    if (db != null) {
      this.delete(db);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return this.find("businessUnitCode = ?1 and archivedAt is null", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }
}
