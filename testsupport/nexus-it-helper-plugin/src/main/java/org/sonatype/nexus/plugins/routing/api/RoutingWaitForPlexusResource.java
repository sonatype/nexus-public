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
package org.sonatype.nexus.plugins.routing.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.internal.ManagerImpl;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exposing {@link ManagerImpl#isUpdatePrefixFileJobRunning()} for ITs.
 *
 * @author cstamas
 * @since 2.4
 */
@Named
@Singleton
public class RoutingWaitForPlexusResource
    extends AbstractPlexusResource
{

  private static final String RESOURCE_URI = "/routing/waitFor";

  private final Manager manager;

  @Inject
  public RoutingWaitForPlexusResource(final Manager manager) {
    this.manager = checkNotNull(manager);
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "anon");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();
    final long timeout = Long.parseLong(form.getFirstValue("timeout", "60000"));
    try {
      final long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() - startTime <= timeout) {
        if (!((ManagerImpl) manager).isUpdatePrefixFileJobRunning()) {
          response.setStatus(Status.SUCCESS_OK);
          return "Ok";
        }
        Thread.sleep(500);
      }
    }
    catch (final InterruptedException ignore) {
      // ignore
    }
    response.setStatus(Status.SUCCESS_ACCEPTED);
    return "Still munching on them...";
  }
}
