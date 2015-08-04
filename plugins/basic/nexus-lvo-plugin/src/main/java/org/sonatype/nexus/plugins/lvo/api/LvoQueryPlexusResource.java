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
package org.sonatype.nexus.plugins.lvo.api;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.lvo.DiscoveryResponse;
import org.sonatype.nexus.plugins.lvo.LvoService;
import org.sonatype.nexus.plugins.lvo.NoSuchKeyException;
import org.sonatype.nexus.plugins.lvo.NoSuchStrategyException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class LvoQueryPlexusResource
    extends AbstractPlexusResource
{
  private final LvoService lvoService;

  @Inject
  public LvoQueryPlexusResource(final LvoService lvoService) {
    this.lvoService = checkNotNull(lvoService);
  }

  @Override
  public Object getPayloadInstance() {
    // this happens to be RO resource
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    // unprotected resource
    return new PathProtectionDescriptor("/lvo/*/*", "authcBasic,perms[nexus:status]");
  }

  @Override
  public String getResourceUri() {
    return "/lvo/{key}/{currentVersion}";
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String key = (String) request.getAttributes().get("key");

    String cv = (String) request.getAttributes().get("currentVersion");

    try {
      DiscoveryResponse dr = lvoService.queryLatestVersionForKey(key, cv);

      if (dr.isSuccessful()) {
        return dr;
      }
      else {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No version newer than '" + cv + "' for key='"
            + key + "' found.");
      }
    }
    catch (NoSuchKeyException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage(), e);
    }
    catch (NoSuchStrategyException | NoSuchRepositoryException | IOException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
    }
  }

}
