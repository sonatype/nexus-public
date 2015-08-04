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
package org.sonatype.nexus.rest.contentclasses;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.RepositoryContentClassListResource;
import org.sonatype.nexus.rest.model.RepositoryContentClassListResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named
@Singleton
@Path(ContentClassComponentListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class ContentClassComponentListPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String RESOURCE_URI = "/components/repo_content_classes";

  private final RepositoryTypeRegistry repoTypeRegistry;

  @Inject
  public ContentClassComponentListPlexusResource(final RepositoryTypeRegistry repoTypeRegistry) {
    this.repoTypeRegistry = repoTypeRegistry;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:componentscontentclasses]");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  /**
   * Retrieve the list of content classes availabe in nexus.  Plugins can contribute to this list.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = RepositoryContentClassListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    RepositoryContentClassListResourceResponse contentClasses = new RepositoryContentClassListResourceResponse();

    for (ContentClass contentClass : repoTypeRegistry.getContentClasses().values()) {
      RepositoryContentClassListResource resource = new RepositoryContentClassListResource();
      resource.setContentClass(contentClass.getId());
      resource.setName(contentClass.getName());
      resource.setGroupable(contentClass.isGroupable());

      for (String compClass : repoTypeRegistry.getCompatibleContentClasses(contentClass)) {
        resource.addCompatibleType(compClass);
      }

      contentClasses.addData(resource);
    }

    return contentClasses;
  }
}
