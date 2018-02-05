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
package org.sonatype.nexus.coreui.internal.wonderland;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresUser;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Status resource.
 *
 * @since 3.0
 */
@Named
@Singleton
@Path(StatusResource.RESOURCE_URI)
public class StatusResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/wonderland/status";

  private final ApplicationVersion applicationVersion;

  @Inject
  public StatusResource(final ApplicationVersion applicationVersion) {
    this.applicationVersion = checkNotNull(applicationVersion);
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @RequiresUser
  public StatusXO get() {
    StatusXO result = new StatusXO();
    result.setVersion(applicationVersion.getVersion());
    result.setEdition(applicationVersion.getEdition());
    return result;
  }
}
