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
package org.sonatype.nexus.metrics;

import java.lang.management.ManagementFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.rest.Resource;

import com.codahale.metrics.jvm.ThreadDump;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import org.springframework.stereotype.Component;

/**
 * Provides current stacktraces for running threads
 */
@Path("/metrics/threads")
@Component
public class ThreadDumpResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ThreadDump threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());

  private final CacheControl cacheControl = new CacheControl();

  public ThreadDumpResource() {
    cacheControl.setMustRevalidate(true);
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }

  @GET
  @RequiresPermissions("nexus:metrics:read")
  public Response dump(
      @DefaultValue("false") @QueryParam("download") final boolean download,
      @DefaultValue("false") @QueryParam("monitors") final boolean monitors,
      @DefaultValue("false") @QueryParam("synchronizers") final boolean synchronizers)
  {
    ResponseBuilder response = Response.ok(dump(monitors, synchronizers), MediaType.TEXT_PLAIN);

    if (download) {
      // we only care if download is true
      response.header(CONTENT_DISPOSITION, "attachment; filename='threads.txt'");
    }
    response.cacheControl(cacheControl);

    return response.build();
  }

  private StreamingOutput dump(final boolean lockedMonitors, final boolean lockedSynchronizers) {
    return out -> threadDump.dump(lockedMonitors, lockedSynchronizers, out);
  }
}
