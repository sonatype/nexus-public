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
package org.sonatype.nexus.repository.content.security;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Asset} permission checks.
 *
 * @since 3.26
 */
@Named
@Singleton
public class AssetPermissionChecker
{
  private final RepositoryManager repositoryManager;

  private final ContentFacetFinder contentFacetFinder;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  @Inject
  public AssetPermissionChecker(
      final RepositoryManager repositoryManager,
      final ContentFacetFinder contentFacetFinder,
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.contentFacetFinder = checkNotNull(contentFacetFinder);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
  }

  /**
   * Finds which of the containing repositories permits access to each of the assets for the given action.
   *
   * Assets that have no permitting repository are not included in the returned mapping.
   *
   * @return mapping from asset to the repository that permits the user to do the action
   */
  public Stream<Entry<Asset, String>> findPermittedAssets(
      final Collection<? extends Asset> assets,
      final String format,
      final String action)
  {
    if (assets.isEmpty()) {
      return Stream.empty();
    }

    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);

    // only do this once - assumes all assets passed in were uploaded to the same repository
    List<String> containingRepositoryNames = containingRepositoryNames(assets.iterator().next());

    return assets.stream().map(asset -> {
      VariableSource source = variableResolverAdapter.fromPath(asset.path(), format);

      return findPermittingRepository(containingRepositoryNames, format, action, source)
          .map(r -> (Entry<Asset, String>) new SimpleImmutableEntry<>((Asset) asset, r))
          .orElse(null);

    }).filter(Objects::nonNull);
  }

  /**
   * Finds which of the containing repositories permits access to the asset for the given action.
   *
   * @return the repository that permits the user to do the action, if it exists
   */
  public Optional<String> isPermitted(final Asset asset, final String format, final String action) {
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);

    List<String> containingRepositoryNames = containingRepositoryNames(asset);
    VariableSource source = variableResolverAdapter.fromPath(asset.path(), format);

    return findPermittingRepository(containingRepositoryNames, format, action, source);
  }

  /**
   * Finds which of the containing repositories permits access to the asset for the given action.
   *
   * @return the repository that permits the user to do the action
   */
  private Optional<String> findPermittingRepository(
      final List<String> containingRepositoryNames,
      final String format,
      final String action,
      final VariableSource variables)
  {
    return containingRepositoryNames.stream()
        .filter(r -> contentPermissionChecker.isPermitted(r, format, action, variables))
        .findFirst();
  }

  /**
   * Returns the list of repositories that contain the given content by virtue of group membership.
   */
  private List<String> containingRepositoryNames(final RepositoryContent repositoryContent) {
    Optional<Repository> repository = contentFacetFinder.findRepository(repositoryContent);
    if (!repository.isPresent()) {
      return ImmutableList.of();
    }

    String repositoryName = repository.get().getName();
    List<String> containingRepositoryNames = repositoryManager.findContainingGroups(repositoryName);
    containingRepositoryNames.add(0, repositoryName);

    return containingRepositoryNames;
  }
}
