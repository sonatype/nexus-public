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
package org.sonatype.nexus.testsuite.helpers;

import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;

import org.joda.time.DateTime;


public interface ComponentAssetTestHelper
{
  /**
   * Get the created time of the blob associated with the asset at the path in the given repository.
   *
   * @param repository the containing repository
   * @param path the path of the asset
   */
  DateTime getCreatedTime(Repository repository, String path);

  /**
   * Get the updated time of the blob associated with the asset at the path in the given repository.
   *
   * @param repository the containing repository
   * @param path the path of the asset
   */
  DateTime getUpdatedTime(Repository repository, String path);

  /**
   * Get the last downloaded time for a path in the given repository.
   */
  DateTime getLastDownloadedTime(Repository repository, String path);

  /**
   * Delete a component in a repository from the database.
   */
  void deleteComponent(Repository repository, String namespace, String name, String version);

  /**
   * Remove an asset from a repository using ComponentMaintenance or similar.
   */
  void removeAsset(final Repository repository, final String path);

  /**
   * Retrieve the paths for all assets in a repository.
   */
  List<String> findAssetPaths(final String repositoryName);

  /**
   * Verify that an asset at the given path exists for the specified repository.
   */
  boolean assetExists(Repository repository, String path);

  /**
   * Verify that an asset at the given path exists for the specified repository, and associated with a component.
   */
  boolean assetWithComponentExists(Repository repository, String path, String group, String name);

  /**
   * Verify that an asset at the given path exists for the specified repository, and associated with a component.
   */
  boolean assetWithComponentExists(Repository repository, String path, String group, String name, String version);

  /**
   * Verify that an asset at the given path exists for the specified repository, and is not associated with a component.
   */
  boolean assetWithoutComponentExists(Repository repository, String path);

  /**
   * Verify that a component with the given name and version exists for the specified repository.
   */
  boolean componentExists(Repository repository, String name, String version);

  /**
   * Verify that a component at the given namespace, name, and version exists for the specified repository.
   */
  boolean componentExists(Repository repository, String namespace, String name, String version);

  /**
   * Retrieve content type for a path within the repository.
   */
  String contentTypeFor(final String repositoryName, final String path);

  /**
   * Count the number of components in the given repository.
   */
  int countComponents(final Repository repository);

  /**
   * Retrieve the attributes for the asset at the given path
   */
  NestedAttributesMap attributes(Repository repository, String path);

  /**
   * Retrieve the attributes for the component
   */
  NestedAttributesMap componentAttributes(Repository repository, String namespace, String name, String version);

  static class AssetNotFoundException
      extends RuntimeException
  {
    AssetNotFoundException(final Repository repository, final String path) {
      super("Missing asset: " + path + " from repository: " + repository.getName());
    }
  }

  static class ComponentNotFoundException
      extends RuntimeException
  {
    ComponentNotFoundException(
        final Repository repository,
        final String namespace,
        final String name,
        final String version)
    {
      super(String.format("Repository:%s namespace:%s name:%s version:%s", repository.getName(), namespace, name,
          version));
    }
  }
}
