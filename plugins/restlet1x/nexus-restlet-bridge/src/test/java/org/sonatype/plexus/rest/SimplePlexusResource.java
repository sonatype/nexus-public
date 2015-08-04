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
package org.sonatype.plexus.rest;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.apache.commons.fileupload.FileItem;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * A simple testing resource. Will "publish" itself on passed in token URI (/token) and will emit "token" for GETs and
 * respond with HTTP 200 to all other HTTP methods (PUT, POST) only if the token equals to entity passed in.
 *
 * @author cstamas
 */
public class SimplePlexusResource
    extends AbstractPlexusResource
{
  private String token;

  public SimplePlexusResource() {
    super();
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return "/" + token;
  }

  public PathProtectionDescriptor getResourceProtection() {
    return null;
  }

  @Override
  public List<Variant> getVariants() {
    List<Variant> result = new ArrayList<Variant>();

    result.add(new Variant(MediaType.TEXT_PLAIN));

    return result;
  }

  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return token;
  }

  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    if (!token.equals(payload.toString())) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
    }

    return null;
  }

  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    if (!token.equals(payload.toString())) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
    }

    return null;
  }

  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    // nothing
  }

  public Object upload(Context context, Request request, Response response, List<FileItem> files)
      throws ResourceException
  {
    return null;
  }

}
