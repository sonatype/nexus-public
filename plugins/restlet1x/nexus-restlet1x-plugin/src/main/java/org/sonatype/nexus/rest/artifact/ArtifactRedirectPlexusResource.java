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
package org.sonatype.nexus.rest.artifact;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * POM Resource handler.
 *
 * @author cstamas
 */
@Named
@Singleton
@Path("/artifact/maven/redirect")
public class ArtifactRedirectPlexusResource
    extends AbstractArtifactPlexusResource
{

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return "/artifact/maven/redirect";
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:artifact]");
  }

  /**
   * Emits HTTP redirects (or Not found) to the location of the requested artifact.
   *
   * <p>
   * This resource never returns any content, only <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.8">HTTP
   * RFC -
   * 10.3.8 307 Temporary Redirect</a> or <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">
   * HTTP RFC - 10.4.5 404 Not Found</a>. The HTTP client accessing this resource has to follow the redirection to
   * get
   * to the content of the artifact.
   * <p>
   * Prior versions of this resource may have responded with an inappropriate <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.2">HTTP RFC - 10.3.2 301 Moved Permanently
   * </a>instead of 307 Temporary Redirect.
   *
   * @param g Group id of the artifact (Required).
   * @param a Artifact id of the artifact (Required).
   * @param v Version of the artifact (Required) Supports resolving of "LATEST", "RELEASE" and snapshot versions
   *          ("1.0-SNAPSHOT") too.
   * @param r Repository that the artifact is contained in (Required).
   * @param p Packaging type of the artifact (Optional).
   * @param c Classifier of the artifact (Optional).
   * @param e Extension of the artifact (Optional).
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {
      @QueryParam("g"), @QueryParam("a"), @QueryParam("v"),
      @QueryParam("r"), @QueryParam("p"), @QueryParam("c"), @QueryParam("e")
  }, output = String.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return getContent(variant, true, request, response);
  }

}
