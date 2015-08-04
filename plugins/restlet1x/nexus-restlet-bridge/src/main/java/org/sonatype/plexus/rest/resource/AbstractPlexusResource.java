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
package org.sonatype.plexus.rest.resource;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.sisu.goodies.common.Loggers;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.fileupload.FileItem;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;

public abstract class AbstractPlexusResource
    implements PlexusResource
{
  private final Logger logger = Loggers.getLogger(getClass());

  private boolean available = true;

  private boolean readable = true;

  private boolean modifiable = false;

  private boolean negotiateContent = true;

  private boolean requireStrictChecking = true;

  protected Logger getLogger() {
    return logger;
  }

  // GETTER/SETTERS, will be unlikely overridden

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public boolean isReadable() {
    return readable;
  }

  public void setReadable(boolean readable) {
    this.readable = readable;
  }

  public boolean isModifiable() {
    return modifiable;
  }

  public void setModifiable(boolean modifiable) {
    this.modifiable = modifiable;
  }

  public boolean isNegotiateContent() {
    return negotiateContent;
  }

  public void setNegotiateContent(boolean negotiateContent) {
    this.negotiateContent = negotiateContent;
  }

  // to be implemented subclasses

  public abstract String getResourceUri();

  public abstract PathProtectionDescriptor getResourceProtection();

  public abstract Object getPayloadInstance();

  // to be overridden by subclasses if needed

  public List<Variant> getVariants() {
    ArrayList<Variant> result = new ArrayList<Variant>();

    result.add(new Variant(MediaType.APPLICATION_XML));

    result.add(new Variant(MediaType.APPLICATION_JSON));

    return result;
  }

  public boolean acceptsUpload() {
    // since this property will not change during the lifetime of a resource, it is needed to be overrided
    return false;
  }

  public boolean requireStrictChecking() {
    return requireStrictChecking;
  }

  public void setRequireStrictChecking(boolean requireStrictChecking) {
    this.requireStrictChecking = requireStrictChecking;
  }

  public void configureXStream(XStream xstream) {
    // a dummy implementation to be overridden if needed
  }

  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
  }

  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
  }

  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
  }

  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
  }

  public Object upload(Context context, Request request, Response response, List<FileItem> files)
      throws ResourceException
  {
    throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED);
  }
  
  
  public void options(Context context, Request request, Response response) throws ResourceException {
  	// do noting by default, override to customize
  }

  public Object getPayloadInstance(org.restlet.data.Method method) {
    return getPayloadInstance();
  }

  /**
   * Adds an HTTP header to restlet response.
   *
   * @param response Restlet response to add the HTTP header to
   * @param name     HTTP header name
   * @param value    HTTP header value
   */
  public static void addHttpResponseHeader(final Response response, final String name, final String value) {
    Form responseHeaders = (Form) response.getAttributes().get("org.restlet.http.headers");

    if (responseHeaders == null) {
      responseHeaders = new Form();
      response.getAttributes().put("org.restlet.http.headers", responseHeaders);
    }

    responseHeaders.add(name, value);
  }

}
