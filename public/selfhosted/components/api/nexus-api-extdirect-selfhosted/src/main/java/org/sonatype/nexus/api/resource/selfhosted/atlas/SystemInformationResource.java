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
package org.sonatype.nexus.api.resource.selfhosted.atlas;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.common.atlas.SystemInformationGenerator;
import org.sonatype.nexus.rest.Resource;

import org.springframework.stereotype.Component;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.api.resource.selfhosted.atlas.SystemInformationResource.RESOURCE_URI;

/**
 * Renders system information. This is required for download of the report from the UI.
 */
@Component
@Path(RESOURCE_URI)
public class SystemInformationResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String RESOURCE_URI = "/atlas/system-information";

  private final SystemInformationGenerator systemInformationGenerator;

  @Autowired
  public SystemInformationResource(final SystemInformationGenerator systemInformationGenerator) {
    this.systemInformationGenerator = checkNotNull(systemInformationGenerator);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions("nexus:atlas:read")
  public Response report() {
    Map<String, Object> report = systemInformationGenerator.report();

    // support downloading the json directly
    return Response.ok(report)
        .header("Content-Disposition", "attachment; filename=\"sysinfo.json\"")
        .build();
  }
}
