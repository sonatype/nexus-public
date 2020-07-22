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
package org.sonatype.nexus.repository.content.maintenance.internal;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;

import org.apache.shiro.authz.AuthorizationException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @since 3.next
 */
@Named
@Singleton
public class MaintenanceServiceImpl
    extends ComponentSupport
    implements MaintenanceService
{
  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  @Inject
  public MaintenanceServiceImpl(
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
  }

  @Override
  public Set<String> deleteAsset(final Repository repository, final Asset asset) {
    checkNotNull(repository);
    checkNotNull(asset);

    if (!canDeleteAsset(repository, asset)) {
      throw new AuthorizationException();
    }

    return getContentMaintenanceFacet(repository).deleteAsset(asset);
  }

  @Override
  public Set<String> deleteComponent(final Repository repository, final Component component) {
    checkNotNull(repository);
    checkNotNull(component);

    if (!canDeleteComponent(repository, component)) {
      throw new AuthorizationException();
    }

    return getContentMaintenanceFacet(repository).deleteComponent(component);
  }

  @Override
  public void deleteFolder(final Repository repository, final String path) {
    checkNotNull(repository);
    checkNotNull(path);

    if (!canDeleteFolder(repository, path)) {
      throw new AuthorizationException();
    }

    log.warn("NOT YET IMPLEMENTED: deleteFolder({}, {})", repository.getName(), path);
  }

  @Override
  public boolean canDeleteComponent(final Repository repository, final Component component) {
    log.warn("NOT YET IMPLEMENTED: canDeleteComponent({}, {})", repository.getName(), component.name());
    return false;
  }

  @Override
  public boolean canDeleteAsset(final Repository repository, final Asset asset) {
    log.warn("NOT YET IMPLEMENTED: canDeleteAsset({}, {})", repository.getName(), asset.path());
    return false;
  }

  @Override
  public boolean canDeleteFolder(final Repository repository, final String folder) {
    return repositoryPermissionChecker.userCanDeleteInRepository(repository);
  }

  private ContentMaintenanceFacet getContentMaintenanceFacet(final Repository repository) {
    try {
      return repository.facet(ContentMaintenanceFacet.class);
    }
    catch (MissingFacetException e) {
      throw new IllegalOperationException(format("Deleting from repository %s of type %s is not supported",
          repository.getName(), repository.getFormat()), e);
    }
  }
}
