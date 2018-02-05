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
package org.sonatype.nexus.repository.maintenance.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.security.BreadActions;

import org.apache.shiro.authz.AuthorizationException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @since 3.3
 */
@Named
public class MaintenanceServiceImpl
    implements MaintenanceService
{
  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  @Inject
  public MaintenanceServiceImpl(final ContentPermissionChecker contentPermissionChecker,
                                final VariableResolverAdapterManager variableResolverAdapterManager)
  {
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
  }

  @Override
  public void deleteAsset(final Repository repository, final Asset asset) {
    checkNotNull(repository);
    checkNotNull(asset);

    String repositoryFormat = repository.getFormat().toString();
    if (!canDeleteAssetInRepository(repository, repositoryFormat, variableResolverAdapterManager.get(repositoryFormat),
        asset)) {
      throw new AuthorizationException();
    }

    getComponentMaintenanceFacet(repository).deleteAsset(asset.getEntityMetadata().getId());
  }

  @Override
  public void deleteComponent(final Repository repository, final Component component) {
    checkNotNull(repository);
    checkNotNull(component);

    String repositoryFormat = repository.getFormat().toString();
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(repositoryFormat);

    StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get();

    try {
      storageTx.begin();
      for (Asset asset : storageTx.browseAssets(component)) {
        if (!canDeleteAssetInRepository(repository, repositoryFormat, variableResolverAdapter, asset)) {
          throw new AuthorizationException();
        }
      }
    }
    finally {
      storageTx.close();
    }

    getComponentMaintenanceFacet(repository).deleteComponent(component.getEntityMetadata().getId());
  }

  private boolean canDeleteAssetInRepository(final Repository repository,
                                             final String repositoryFormat,
                                             final VariableResolverAdapter variableResolverAdapter, final Asset asset)
  {
    return contentPermissionChecker.isPermitted(repository.getName(), repositoryFormat, BreadActions.DELETE,
        variableResolverAdapter.fromAsset(asset));
  }

  private ComponentMaintenance getComponentMaintenanceFacet(final Repository repository) {
    try {
      return repository.facet(ComponentMaintenance.class);
    }
    catch (MissingFacetException e) {
      throw new IllegalOperationException(
          format("Deleting from repository %s of type %s is not supported", repository.getName(),
              repository.getFormat()), e);
    }
  }
}
