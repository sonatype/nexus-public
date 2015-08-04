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

import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.RestletResource;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

public class NexusRestletResource
    extends RestletResource
{

  public NexusRestletResource(Context context, Request request, Response response, PlexusResource delegate) {
    super(context, request, response, delegate);
  }

  @Override
  public Representation represent(Variant variant)
      throws ResourceException
  {
    try {
      return super.represent(variant);
    }
    catch (ResourceException e) {
      // NEXUS-4238, NEXUS-4290
      // if it's server error based on HTTP Code, but NOT when Nexus throws a known 503
      // (see org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource.handleException(Request, Response, Exception))
      final Status status = e.getStatus();
      if (status == null) {
        handleError(e);
      }
      else {
        final int code = status.getCode();
        if (Status.isServerError(code) && Status.SERVER_ERROR_SERVICE_UNAVAILABLE.getCode() != code) {
          handleError(e);
        }
      }

      throw e;
    }
    catch (RuntimeException e) {
      handleError(e);

      throw e;
    }
  }

  @Override
  public void acceptRepresentation(Representation representation)
      throws ResourceException
  {
    try {
      super.acceptRepresentation(representation);
    }
    catch (ResourceException e) {
      if (Status.isServerError(e.getStatus().getCode())) {
        handleError(e);
      }

      throw e;
    }
    catch (RuntimeException e) {
      handleError(e);

      throw e;
    }
  }

  @Override
  public void storeRepresentation(Representation representation)
      throws ResourceException
  {
    try {
      super.storeRepresentation(representation);
    }
    catch (ResourceException e) {
      if (Status.isServerError(e.getStatus().getCode())) {
        handleError(e);
      }

      throw e;
    }
    catch (RuntimeException e) {
      handleError(e);

      throw e;
    }
  }

  @Override
  public void removeRepresentations()
      throws ResourceException
  {
    try {
      super.removeRepresentations();
    }
    catch (ResourceException e) {
      if (Status.isServerError(e.getStatus().getCode())) {
        handleError(e);
      }
      throw e;
    }
    catch (RuntimeException e) {
      handleError(e);

      throw e;
    }
  }

  protected void handleError(Throwable throwable) {
    logger.error(throwable.toString(), throwable);
  }
}
