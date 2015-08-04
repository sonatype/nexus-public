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
package org.sonatype.nexus.rest;

import org.sonatype.plexus.rest.PlexusResourceFinder;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.RestletResource;

import org.restlet.Context;
import org.restlet.Handler;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class NexusPlexusResourceFinder
    extends PlexusResourceFinder
{
  private PlexusResource plexusResource;

  private Context context;

  public NexusPlexusResourceFinder(Context context, PlexusResource resource) {
    super(context, resource);

    this.plexusResource = resource;
    this.context = context;
  }

  @Override
  public Handler createTarget(Request request, Response response) {
    RestletResource restletResource = new NexusRestletResource(getContext(), request, response, plexusResource);

    // init must-have stuff
    restletResource.setContext(context);
    restletResource.setRequest(request);
    restletResource.setResponse(response);

    return restletResource;
  }
}
