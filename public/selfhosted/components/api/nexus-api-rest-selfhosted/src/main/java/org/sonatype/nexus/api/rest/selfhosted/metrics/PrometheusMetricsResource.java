/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.api.rest.selfhosted.metrics;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.rest.Resource;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;

@Path("/metrics/prometheus")
@Component
@Singleton
public class PrometheusMetricsResource
    implements Resource
{
  private final CacheControl cacheControl = new CacheControl();

  @Inject
  public PrometheusMetricsResource(final MetricRegistry registry) {
    // Export to Prometheus
    new DropwizardExports(registry).register();

    cacheControl.setMustRevalidate(true);
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }

  @GET
  @RequiresPermissions("nexus:metrics:read")
  public Response read(@QueryParam("name[]") final Set<String> names) {
    return Response.ok(entity(names), TextFormat.CONTENT_TYPE_004).cacheControl(cacheControl).build();
  }

  private StreamingOutput entity(final Set<String> names) {
    return out -> {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(out))) {
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names));
      }
    };
  }
}
