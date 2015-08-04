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
package org.sonatype.nexus.plugins.exception.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Singleton
@Named
public class ExceptionPlexusResource
    extends AbstractPlexusResource
{
  @Inject
  public ExceptionPlexusResource() {
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return "/exception";
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();

    int requestedStatus = Integer.parseInt(form.getFirstValue("status"));

    throw new ResourceException(requestedStatus);
  }
}
