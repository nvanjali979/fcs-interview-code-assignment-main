package com.fulfilment.application.monolith.health;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

/**
 * Readiness probe: verifies that the warehouse data layer is reachable.
 *
 * <p>Quarkus already adds an automatic datasource liveness probe when
 * quarkus-hibernate-orm is on the classpath. This check complements that by
 * exercising the actual query path used by the application, so the pod only
 * receives traffic once the full warehouse read path is confirmed working.
 *
 * <p>Exposed at: {@code GET /q/health/ready}
 */
@Readiness
@ApplicationScoped
public class WarehouseReadinessCheck implements HealthCheck {

  private static final Logger LOGGER = Logger.getLogger(WarehouseReadinessCheck.class);

  @Inject WarehouseRepository warehouseRepository;

  @Override
  public HealthCheckResponse call() {
    try {
      int count = warehouseRepository.getAll().size();
      return HealthCheckResponse.named("warehouse-store")
          .up()
          .withData("activeWarehouses", count)
          .build();
    } catch (Exception e) {
      LOGGER.errorf(e, "Warehouse readiness check failed");
      return HealthCheckResponse.named("warehouse-store")
          .down()
          .withData("error", e.getMessage())
          .build();
    }
  }
}
