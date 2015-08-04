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
package org.sonatype.nexus.plugins.repository;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

@Named
@Singleton
@Deprecated
final class DefaultPluginRepositoryManager
    implements NexusPluginRepository
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  private static final Comparator<NexusPluginRepository> REPOSITORY_COMPARATOR =
      new NexusPluginRepositoryComparator();

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  @Inject
  private Map<String, NexusPluginRepository> repositoryMap;

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public String getId() {
    return null;
  }

  public int getPriority() {
    return -1;
  }

  public Map<GAVCoordinate, PluginMetadata> findAvailablePlugins() {
    final Map<GAVCoordinate, PluginMetadata> installedPlugins = new HashMap<GAVCoordinate, PluginMetadata>();
    for (final NexusPluginRepository r : getRepositories(true)) {
      installedPlugins.putAll(r.findAvailablePlugins());
    }
    return installedPlugins;
  }

  public PluginRepositoryArtifact resolveArtifact(final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    for (final NexusPluginRepository r : getRepositories(false)) {
      try {
        return r.resolveArtifact(gav);
      }
      catch (final NoSuchPluginRepositoryArtifactException e) // NOPMD
      {
        // continue
      }
    }
    throw new NoSuchPluginRepositoryArtifactException(null, gav);
  }

  public PluginRepositoryArtifact resolveDependencyArtifact(final PluginRepositoryArtifact plugin,
                                                            final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    try {
      return plugin.getNexusPluginRepository().resolveDependencyArtifact(plugin, gav);
    }
    catch (final NoSuchPluginRepositoryArtifactException e) // NOPMD
    {
      // continue
    }
    for (final NexusPluginRepository r : getRepositories(false)) {
      try {
        if (r != plugin.getNexusPluginRepository()) {
          return r.resolveDependencyArtifact(plugin, gav);
        }
      }
      catch (final NoSuchPluginRepositoryArtifactException e) // NOPMD
      {
        // continue
      }
    }
    throw new NoSuchPluginRepositoryArtifactException(null, gav);
  }

  public PluginMetadata getPluginMetadata(final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    for (final NexusPluginRepository r : getRepositories(false)) {
      try {
        return r.getPluginMetadata(gav);
      }
      catch (final NoSuchPluginRepositoryArtifactException e) // NOPMD
      {
        // continue
      }
    }
    throw new NoSuchPluginRepositoryArtifactException(null, gav);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  private NexusPluginRepository[] getRepositories(final boolean reverse) {
    final Set<NexusPluginRepository> sortedRepositories;
    if (reverse) {
      sortedRepositories = new TreeSet<NexusPluginRepository>(Collections.reverseOrder(REPOSITORY_COMPARATOR));
    }
    else {
      sortedRepositories = new TreeSet<NexusPluginRepository>(REPOSITORY_COMPARATOR);
    }

    for (final NexusPluginRepository repo : repositoryMap.values()) {
      if (repo != this) // avoid recursion since this is also a NexusPluginRepository
      {
        sortedRepositories.add(repo);
      }
    }

    return sortedRepositories.toArray(new NexusPluginRepository[sortedRepositories.size()]);
  }
}
