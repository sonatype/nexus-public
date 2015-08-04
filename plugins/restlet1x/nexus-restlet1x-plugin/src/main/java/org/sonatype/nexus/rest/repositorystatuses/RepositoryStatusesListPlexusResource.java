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
package org.sonatype.nexus.rest.repositorystatuses;

import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.model.RepositoryStatusListResource;
import org.sonatype.nexus.rest.model.RepositoryStatusListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named
@Singleton
@Path(RepositoryStatusesListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class RepositoryStatusesListPlexusResource
    extends AbstractRepositoryPlexusResource
{
  public static final String RESOURCE_URI = "/repository_statuses";

  private static final String ADMIN_ONLY_KEY = "nexus.repositoryStatuses.adminOnly";

  private final boolean adminOnly = SystemPropertiesHelper.getBoolean(ADMIN_ONLY_KEY, false);

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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:repostatus]");
  }

  /**
   * Get the list of all repository statuses. The remote statuses in case of Proxy repositories are cached (to avoid
   * network flooding). You can force the remote status recheck by adding the "forceCheck" query parameter, but be
   * aware, that this one inbound REST Request will induce as many Nexus outbound requests as many proxy repositories
   * you have defined.
   *
   * @param forceCheck If true, will force a remote check of status (Optional).
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {@QueryParam("forceCheck")},
      output = RepositoryStatusListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    if (adminOnly) {
      Subject subject = ThreadContext.getSubject();
      // skip building reply unless user has admin access (ie. can update status)
      if (subject == null || !subject.isPermitted("nexus:repostatus:update")) {
        response.setStatus(Status.SUCCESS_NO_CONTENT);
        return null;
      }
    }

    RepositoryStatusListResourceResponse result = new RepositoryStatusListResourceResponse();

    RepositoryStatusListResource repoRes;

    Collection<Repository> repositories = getRepositoryRegistry().getRepositories();

    for (Repository repository : repositories) {
      repoRes = new RepositoryStatusListResource();

      repoRes.setResourceURI(createChildReference(request, this, repository.getId()).toString());

      repoRes.setId(repository.getId());

      repoRes.setName(repository.getName());

      repoRes.setRepoType(getRestRepoType(repository));

      if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
        repoRes.setRepoPolicy(repository.adaptToFacet(MavenRepository.class).getRepositoryPolicy().toString());
      }

      repoRes.setFormat(repository.getRepositoryContentClass().getId());

      repoRes.setStatus(new RepositoryStatusResource());

      repoRes.getStatus().setLocalStatus(repository.getLocalStatus().toString());

      if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
        repoRes.getStatus().setRemoteStatus(
            getRestRepoRemoteStatus(
                repository.adaptToFacet(ProxyRepository.class),
                request, response));

        repoRes.getStatus().setProxyMode(
            repository.adaptToFacet(ProxyRepository.class).getProxyMode().toString());
      }

      result.addData(repoRes);
    }

    return result;
  }

}
