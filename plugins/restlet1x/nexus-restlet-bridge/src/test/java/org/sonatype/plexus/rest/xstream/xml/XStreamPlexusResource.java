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
package org.sonatype.plexus.rest.xstream.xml;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import com.thoughtworks.xstream.XStream;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * A simple testing resource. Will "publish" itself on passed in token URI (/token) and will emit "token" for GETs and
 * respond with HTTP 200 to all other HTTP methods (PUT, POST) only if the token equals to entity passed in.
 *
 * @author cstamas
 */
@Named
@Singleton
public class XStreamPlexusResource
    extends AbstractPlexusResource
{

  public XStreamPlexusResource() {
    super();

    setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new SimpleTestObject();
  }

  @Override
  public void configureXStream(XStream xstream) {
    super.configureXStream(xstream);

    xstream.processAnnotations(SimpleTestObject.class);
  }

  @Override
  public String getResourceUri() {
    return "/XStreamPlexusResource";
  }

  @Override
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    SimpleTestObject obj = (SimpleTestObject) payload;
    if (obj.getData() == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
    }

    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return null;
  }

}
