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

import java.util.Map;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

/**
 * Provides a simple {@link GAVCoordinate} based plugin repository.
 */
@Deprecated
public interface NexusPluginRepository
{
  /**
   * @return Unique repository ID
   */
  String getId();

  /**
   * @return Repository priority; natural ordering, smaller before bigger
   */
  int getPriority();

  /**
   * Queries which plugins are available in this repository.
   *
   * @return Map of available plugins and their metadata
   */
  Map<GAVCoordinate, PluginMetadata> findAvailablePlugins();

  /**
   * Resolves the plugin artifact identified by the given {@link GAVCoordinate}.
   *
   * @param gav The plugin coordinates
   * @return Resolved plugin artifact
   */
  PluginRepositoryArtifact resolveArtifact(GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException;

  /**
   * Resolves the dependency artifact identified by the given {@link GAVCoordinate}.
   *
   * @param plugin The plugin artifact
   * @param gav    The dependency coordinates
   * @return Resolved dependency artifact
   */
  PluginRepositoryArtifact resolveDependencyArtifact(PluginRepositoryArtifact plugin, GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException;

  /**
   * Resolves the plugin artifact and returns the metadata from {@code plugin.xml}.
   *
   * @param gav The plugin coordinates
   * @return Plugin metadata
   */
  PluginMetadata getPluginMetadata(GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException;
}
