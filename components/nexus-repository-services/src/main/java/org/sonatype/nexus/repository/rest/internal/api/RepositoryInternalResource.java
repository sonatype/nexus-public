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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @since 3.29
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RepositoryInternalResource.RESOURCE_PATH)
public class RepositoryInternalResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/repositories";

  static final RepositoryXO ALL_REFERENCE = new RepositoryXO(
      RepositorySelector.all().toSelector(),
      "(All Repositories)"
  );

  private final List<Format> formats;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final ProxyType proxyType;

  @Inject
  public RepositoryInternalResource(
      final List<Format> formats,
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final ProxyType proxyType)
  {
    this.formats = checkNotNull(formats);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.proxyType = checkNotNull(proxyType);
  }

  @GET
  @RequiresAuthentication
  public List<RepositoryXO> getRepositories(
      @QueryParam("type") final String type,
      @QueryParam("withAll") final boolean withAll,
      @QueryParam("withFormats") final boolean withFormats)
  {
    List<RepositoryXO> repositories = stream(repositoryManager.browse())
        .filter(repositoryPermissionChecker::userCanBrowseRepository)
        .filter(repository -> isBlank(type) || type.equals(repository.getType().toString()))
        .map(repository -> new RepositoryXO(repository.getName(), repository.getName()))
        .sorted(Comparator.comparing(RepositoryXO::getName))
        .collect(toList());

    List<RepositoryXO> result = new ArrayList<>();
    if (withAll) {
      result.add(ALL_REFERENCE);
    }
    if (withFormats) {
      formats.stream()
          .map(format -> new RepositoryXO(
              RepositorySelector.allOfFormat(format.getValue()).toSelector(),
              "(All " + format.getValue() + " Repositories)"
          ))
          .sorted(Comparator.comparing(RepositoryXO::getName))
          .forEach(result::add);
    }
    result.addAll(repositories);

    return result;
  }

  @GET
  @Path("/details")
  public List<RepositoryDetailXO> getRepositoryDetails()
  {
    return stream(repositoryManager.browse())
        .filter(repositoryPermissionChecker::userCanBrowseRepository)
        .map(this::asRepositoryDetail)
        .collect(toList());
  }

  private RepositoryDetailXO asRepositoryDetail(final Repository repository) {
    boolean online = repository.getConfiguration().isOnline();
    String description = null;
    String reason = null;
    if (proxyType.equals(repository.getType())) {
      HttpClientFacet httpClientFacet = repository.facet(HttpClientFacet.class);
      RemoteConnectionStatus remoteConnectionStatus = httpClientFacet.getStatus();
      description = remoteConnectionStatus.getDescription();
      reason = remoteConnectionStatus.getReason();
    }
    return new RepositoryDetailXO(
      repository.getName(),
      repository.getType().toString(),
      repository.getFormat().toString(),
      repository.getUrl(),
      new RepositoryStatusXO(online, description, reason));
  }
}
