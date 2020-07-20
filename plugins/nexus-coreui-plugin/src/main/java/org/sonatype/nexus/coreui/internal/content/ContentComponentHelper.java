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
package org.sonatype.nexus.coreui.internal.content;

import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.coreui.ComponentXO;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Content-based {@link ComponentHelper}.
 *
 * @since 3.next
 */
@Named
@Singleton
public class ContentComponentHelper
    extends ComponentSupport
    implements ComponentHelper
{
  @Override
  public List<AssetXO> readComponentAssets(final Repository repository, final ComponentXO componentXO) {
    log.warn("NOT YET IMPLEMENTED: readComponentAssets({}, {})", repository.getName(), componentXO);
    return ImmutableList.of();
  }

  @Override
  public PageResult<AssetXO> previewAssets(
      final RepositorySelector repositorySelector,
      final List<Repository> selectedRepositories,
      final String jexlExpression,
      final QueryOptions queryOptions)
  {
    log.warn("NOT YET IMPLEMENTED: previewAssets({}, {}, {}, {})",
        repositorySelector, selectedRepositories, jexlExpression, queryOptions);
    return null;
  }

  @Override
  public ComponentXO readComponent(final Repository repository, final EntityId componentId) {
    log.warn("NOT YET IMPLEMENTED: readComponent({}, {})", repository.getName(), componentId);
    return new ComponentXO();
  }

  @Override
  public boolean canDeleteComponent(final Repository repository, final ComponentXO componentXO) {
    log.warn("NOT YET IMPLEMENTED: canDeleteComponent({}, {})", repository.getName(), componentXO);
    return false;
  }

  @Override
  public Set<String> deleteComponent(final Repository repository, final ComponentXO componentXO) {
    log.warn("NOT YET IMPLEMENTED: deleteComponent({}, {})", repository.getName(), componentXO);
    return ImmutableSet.of();
  }

  @Override
  public AssetXO readAsset(final Repository repository, final EntityId assetId) {
    log.warn("NOT YET IMPLEMENTED: readAsset({}, {})", repository.getName(), assetId);
    return new AssetXO();
  }

  @Override
  public boolean canDeleteAsset(final Repository repository, final EntityId assetId) {
    log.warn("NOT YET IMPLEMENTED: canDeleteAsset({}, {})", repository.getName(), assetId);
    return false;
  }

  @Override
  public Set<String> deleteAsset(final Repository repository, final EntityId assetId) {
    log.warn("NOT YET IMPLEMENTED: deleteAsset({}, {})", repository.getName(), assetId);
    return ImmutableSet.of();
  }

  @Override
  public boolean canDeleteFolder(final Repository repository, final String path) {
    log.warn("NOT YET IMPLEMENTED: canDeleteFolder({}, {})", repository.getName(), path);
    return false;
  }

  @Override
  public void deleteFolder(final Repository repository, final String path) {
    log.warn("NOT YET IMPLEMENTED: deleteFolder({}, {})", repository.getName(), path);
  }
}
