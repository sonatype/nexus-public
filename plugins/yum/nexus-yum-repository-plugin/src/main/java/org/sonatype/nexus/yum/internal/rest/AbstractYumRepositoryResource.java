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
package org.sonatype.nexus.yum.internal.rest;

import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * @since yum 3.0
 */
public abstract class AbstractYumRepositoryResource
    extends AbstractPlexusResource
{

  private final UrlPathParser requestSegmentInterpetor;

  public AbstractYumRepositoryResource() {
    this.requestSegmentInterpetor = new UrlPathParser(getUrlPrefixName(), getSegmentCountAfterPrefix());
  }

  @Override
  public Object getPayloadInstance() {
    // if you allow PUT or POST you would need to return your object.
    return null;
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    try {
      final UrlPathInterpretation interpretation = requestSegmentInterpetor.parse(request);

      if (interpretation.isRedirect()) {
        response.redirectPermanent(interpretation.getRedirectUri());
        return null;
      }

      return createRepresentation(interpretation, getYumRepository(request, interpretation));
    }
    catch (ResourceException e) {
      throw e;
    }
    catch (Exception e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
    }
  }

  private Representation createRepresentation(UrlPathInterpretation interpretation,
                                              YumRepository yumRepository)
  {
    return interpretation.isIndex() ? new IndexRepresentation(interpretation, yumRepository)
        : new YumFileRepresentation(interpretation, yumRepository);
  }

  protected abstract String getUrlPrefixName();

  protected abstract YumRepository getYumRepository(Request request, UrlPathInterpretation interpretation)
      throws Exception;

  protected abstract int getSegmentCountAfterPrefix();
}
