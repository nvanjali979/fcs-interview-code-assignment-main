package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;

// Alias used in javadoc only; code uses the fully qualified name to avoid ambiguity with the
// generated com.warehouse.api.beans.Warehouse.
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOGGER = Logger.getLogger(WarehouseResourceImpl.class);

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    LOGGER.debug("Listing all active warehouses");
    return warehouseRepository.getAll().stream().map(this::toApiWarehouse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    if (data == null) {
      throw new WebApplicationException("Request body must not be null", 400);
    }
    var domain = toDomainWarehouse(data);
    createWarehouseOperation.create(domain);
    return toApiWarehouse(domain);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    LOGGER.debugf("Fetching warehouse [buCode=%s]", id);
    var warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse not found for BU code: " + id, 404);
    }
    return toApiWarehouse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var placeholder =
        new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    placeholder.businessUnitCode = id;
    archiveWarehouseOperation.archive(placeholder);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    if (data == null) {
      throw new WebApplicationException("Request body must not be null", 400);
    }
    var domain = toDomainWarehouse(data);
    domain.businessUnitCode = businessUnitCode;
    replaceWarehouseOperation.replace(domain);
    return toApiWarehouse(domain);
  }

  // ---------------------------------------------------------------------------
  // Mapping helpers
  // ---------------------------------------------------------------------------

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse api) {
    var w = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    w.businessUnitCode = api.getBusinessUnitCode();
    w.location = api.getLocation();
    w.capacity = api.getCapacity();
    w.stock = api.getStock();
    return w;
  }

  private Warehouse toApiWarehouse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse w) {
    var response = new Warehouse();
    response.setBusinessUnitCode(w.businessUnitCode);
    response.setLocation(w.location);
    response.setCapacity(w.capacity);
    response.setStock(w.stock);
    return response;
  }

  // ---------------------------------------------------------------------------
  // Exception mapper – consistent with StoreResource and ProductResource
  // ---------------------------------------------------------------------------

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    private static final Logger MAPPER_LOGGER = Logger.getLogger(ErrorMapper.class);

    @Inject ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
      int code = 500;
      if (exception instanceof WebApplicationException wae) {
        code = wae.getResponse().getStatus();
        if (code >= 500) {
          MAPPER_LOGGER.errorf(exception, "Warehouse endpoint error [status=%d]", code);
        } else {
          MAPPER_LOGGER.warnf("Warehouse request rejected [status=%d]: %s",
              code, exception.getMessage());
        }
      } else {
        MAPPER_LOGGER.errorf(exception, "Unexpected warehouse endpoint error");
      }

      ObjectNode body = objectMapper.createObjectNode();
      body.put("exceptionType", exception.getClass().getName());
      body.put("code", code);
      if (exception.getMessage() != null) {
        body.put("error", exception.getMessage());
      }

      return Response.status(code).entity(body).build();
    }
  }
}
