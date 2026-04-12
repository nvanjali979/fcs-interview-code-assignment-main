package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


class LocationGatewayTest {

  private LocationGateway locationGateway;

  @BeforeEach
  void setUp() {
    locationGateway = new LocationGateway();
  }

 
  @Nested
  @DisplayName("Positive cases – known identifiers")
  class PositiveCases {

    @Test
    @DisplayName("resolves ZWOLLE-001 with correct maxWarehouses and maxCapacity")
    void resolveZwolle001() {
      Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

      assertNotNull(location);
      assertEquals("ZWOLLE-001", location.identification);
      assertEquals(1, location.maxNumberOfWarehouses);
      assertEquals(40, location.maxCapacity);
    }

    @Test
    @DisplayName("resolves ZWOLLE-002 with correct maxWarehouses and maxCapacity")
    void resolveZwolle002() {
      Location location = locationGateway.resolveByIdentifier("ZWOLLE-002");

      assertNotNull(location);
      assertEquals("ZWOLLE-002", location.identification);
      assertEquals(2, location.maxNumberOfWarehouses);
      assertEquals(50, location.maxCapacity);
    }

    @Test
    @DisplayName("resolves AMSTERDAM-001 – highest-capacity location")
    void resolveAmsterdam001() {
      Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

      assertNotNull(location);
      assertEquals("AMSTERDAM-001", location.identification);
      assertEquals(5, location.maxNumberOfWarehouses);
      assertEquals(100, location.maxCapacity);
    }

    @Test
    @DisplayName("resolves AMSTERDAM-002")
    void resolveAmsterdam002() {
      Location location = locationGateway.resolveByIdentifier("AMSTERDAM-002");

      assertNotNull(location);
      assertEquals("AMSTERDAM-002", location.identification);
      assertEquals(3, location.maxNumberOfWarehouses);
      assertEquals(75, location.maxCapacity);
    }

    @Test
    @DisplayName("resolves TILBURG-001")
    void resolveTilburg001() {
      Location location = locationGateway.resolveByIdentifier("TILBURG-001");

      assertNotNull(location);
      assertEquals("TILBURG-001", location.identification);
    }

    @Test
    @DisplayName("resolves HELMOND-001")
    void resolveHelmond001() {
      Location location = locationGateway.resolveByIdentifier("HELMOND-001");

      assertNotNull(location);
      assertEquals("HELMOND-001", location.identification);
      assertEquals(1, location.maxNumberOfWarehouses);
      assertEquals(45, location.maxCapacity);
    }

    @Test
    @DisplayName("resolves EINDHOVEN-001")
    void resolveEindhoven001() {
      Location location = locationGateway.resolveByIdentifier("EINDHOVEN-001");

      assertNotNull(location);
      assertEquals("EINDHOVEN-001", location.identification);
      assertEquals(2, location.maxNumberOfWarehouses);
      assertEquals(70, location.maxCapacity);
    }

    @Test
    @DisplayName("resolves VETSBY-001 – large single-warehouse location")
    void resolveVetsby001() {
      Location location = locationGateway.resolveByIdentifier("VETSBY-001");

      assertNotNull(location);
      assertEquals("VETSBY-001", location.identification);
      assertEquals(1, location.maxNumberOfWarehouses);
      assertEquals(90, location.maxCapacity);
    }
  }

  // -------------------------------------------------------------------------
  // Negative / error tests
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Negative cases – unknown or edge-case identifiers")
  class NegativeCases {

    @Test
    @DisplayName("returns null for an identifier that does not exist")
    void resolveUnknownLocation() {
      Location location = locationGateway.resolveByIdentifier("UNKNOWN-999");

      assertNull(location, "Expected null for an identifier not in the static list");
    }

    @Test
    @DisplayName("returns null for an empty string identifier")
    void resolveEmptyStringIdentifier() {
      Location location = locationGateway.resolveByIdentifier("");

      assertNull(location);
    }

    @Test
    @DisplayName("returns null for a null identifier (graceful – String.equals(null) is false)")
    void resolveNullIdentifier() {
      // String.equals(null) returns false, so the stream finds no match → null is returned.
      Location location = locationGateway.resolveByIdentifier(null);

      assertNull(location);
    }

    @Test
    @DisplayName("lookup is case-sensitive – lowercase variant returns null")
    void resolveIsCaseSensitive() {
      Location location = locationGateway.resolveByIdentifier("amsterdam-001");

      assertNull(location, "Lookup must be case-sensitive; lowercase should not match");
    }

    @Test
    @DisplayName("partial match returns null – prefix only is not sufficient")
    void resolvePartialIdentifier() {
      Location location = locationGateway.resolveByIdentifier("AMSTERDAM");

      assertNull(location);
    }
  }
}
