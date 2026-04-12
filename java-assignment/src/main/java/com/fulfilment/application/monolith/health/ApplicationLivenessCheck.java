package com.fulfilment.application.monolith.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Liveness probe: confirms the application process is running and responsive.
 *
 * <p>A liveness failure tells the container orchestrator (e.g. Kubernetes) to
 * restart the pod. This check is intentionally trivial — a liveness probe
 * should only fail when the process is stuck or deadlocked, not for transient
 * downstream issues (those belong in the readiness probe).
 *
 * <p>Exposed at: {@code GET /q/health/live}
 */
@Liveness
@ApplicationScoped
public class ApplicationLivenessCheck implements HealthCheck {

  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.named("application-live").up().build();
  }
}
