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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.apache.maven.model.Model;
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
@Path(ArtifactPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class ArtifactPlexusResource
    extends AbstractArtifactPlexusResource
{
  public static final String RESOURCE_URI = "/artifact/maven";

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:artifact]");
  }

  /**
   * Returns POM model in a serialized form (it is NOT consumable by Maven, the returned content is not XML
   * representation of Maven POM!) for provided GAV coordinates.
   *
   * @param g Group id of the pom (Required).
   * @param a Artifact id of the pom (Required).
   * @param v Version of the artifact (Required) Supports resolving of "LATEST", "RELEASE" and snapshot versions
   *          ("1.0-SNAPSHOT") too.
   * @param r Repository to retrieve the pom from (Required).
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {
      @QueryParam("g"), @QueryParam("a"), @QueryParam("v"),
      @QueryParam("r")
  }, output = Model.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return getPom(variant, request, response);
  }

}
