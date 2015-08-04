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
package org.sonatype.nexus.rest.index;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.rest.indexng.SearchNGIndexPlexusResource;
import org.sonatype.nexus.rest.model.SearchResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named("DefaultIndexPlexusResource")
@Singleton
@Path(DefaultIndexPlexusResource.RESOURCE_URI)
public class DefaultIndexPlexusResource
    extends AbstractIndexPlexusResource
{
  public static final String RESOURCE_URI = "/data_index";

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:index]");
  }

  @Override
  protected boolean getIsFullReindex() {
    return true;
  }

  /**
   * <em>This resource is deprecated, please use {@link SearchNGIndexPlexusResource} resource on path {@code
   * /lucene/search} instead!</em>
   * Search against all repositories using provided parameters. Note there are a few different types of searches you
   * can perform. If you provide the 'q' query parameter, a keyword search will be performed. If you provide the 'g,
   * a, v, p or c' query parameters, a maven coordinate search will be performed. If you provide the 'cn' query
   * parameter, a classname search will be performed. If you provide the 'sha1' query parameter, a checksum search
   * will be performed.
   *
   * @param q     provide this param for a keyword search (g, a, v, p, c, cn, sha1 params will be ignored).
   * @param sha1  provide this param for a checksum search (g, a, v, p, c, cn params will be ignored).
   * @param cn    provide this param for a classname search (g, a, v, p, c params will be ignored).
   * @param g     group id to perform a maven search against (can be combined with a, v, p & c params as well).
   * @param a     artifact id to perform a maven search against (can be combined with g, v, p & c params as well).
   * @param v     version to perform a maven search against (can be combined with g, a, p & c params as well).
   * @param p     packaging type to perform a maven search against (can be combined with g, a, v & c params as well).
   * @param c     classifier to perform a maven search against (can be combined with g, a, v & p params as well).
   * @param from  result index to start retrieving results from.
   * @param count number of results to have returned to you.
   * @deprecated Use {@link SearchNGIndexPlexusResource} instead (resource path {@code /lucene/search})!
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {
      @QueryParam("q"), @QueryParam("g"), @QueryParam("a"),
      @QueryParam("v"), @QueryParam("p"), @QueryParam("c"), @QueryParam("cn"), @QueryParam("sha1"),
      @QueryParam("from"), @QueryParam("count")
  }, output = SearchResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return super.get(context, request, response, variant);
  }
}
