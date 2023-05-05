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

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;

import org.joda.time.DateTime;


public interface ComponentAssetTestHelper
{
  /**
   * Get the created time of the the asset at the path in the given repository.
   *
   * @param repository the containing repository
   * @param path the path of the asset
   */
  DateTime getAssetCreatedTime(Repository repository, String path);

  /**
   * Get the updated time of the blob associated with the asset at the path in the given repository.
   *
   * @param repository the containing repository
   * @param path the path of the asset
   */
  DateTime getBlobUpdatedTime(Repository repository, String path);

  /**
   * Get the last downloaded time for a path in the given repository.
   */
  DateTime getLastDownloadedTime(Repository repository, String path);

  /**
   * Get the 'created by'(user who created the asset) for a path in the given repository.
   */
  String getCreatedBy(Repository repository, String path);

  /**
   * Get the 'created by ip'(IP address of a user who created the asset) for a path in the given repository.
   */
  String getCreatedByIP(Repository repository, String path);

  /**
   * Retrieves the CacheInfo for the asset if it exists.
   *
   * @param repository
   * @param path
   */
  @Nullable
  CacheInfo getCacheInfo(Repository repository, String path);

  /**
   * Delete a component in a repository from the database.
   */
  void deleteComponent(Repository repository, String namespace, String name, String version);

  /**
   * Delete a component in a repository from the database.
   */
  void deleteComponent(Repository repository, String name, String version);

  /**
   * Remove an component from a repository using ComponentMaintenance or similar.
   */
  void removeComponent(final Repository repository, String namespace, String name, String version);

  /**
   * Remove an asset from a repository using ComponentMaintenance or similar.
   */
  void removeAsset(final Repository repository, final String path);

  /**
   * Retrieve the paths for all assets in a repository.
   */
  List<String> findAssetPaths(final String repositoryName);

  /**
   * Retrieve the AssetKind from asset at the given pat.
   */
  String assetKind(Repository repository, String path);

  /**
   * Retrieve the asset checksums.
   */
  Map<String, String> checksums(Repository repository, String path);

  /**
   * Verify that an asset at the given path exists for the specified repository.
   */
  boolean assetExists(Repository repository, String path);

  /**
   *  Verify that an asset at the given component name and format extension exists for the specified repository.
   */
  boolean assetExists(Repository repository, String componentName, String formatExtension);

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
   * Verify that a component with the given name exists for the specified repository.
   */
  boolean componentExists(Repository repository, String name);

  /**
   * Verify that a component with the given name mather exists for the specified repository.
   */
  boolean checkComponentExist(Repository repository, Predicate<String> nameMatcher);

  /**
   * Verify that a component with the given name and version exists for the specified repository.
   */
  boolean componentExists(Repository repository, String name, String version);

  /**
   * Verify that a component at the given namespace, name, and version exists for the specified repository.
   */
  boolean componentExists(Repository repository, String namespace, String name, String version);

  /**
   * Verify that a component with an asset that matches the path exists.
   */
  boolean componentExistsWithAssetPathMatching(Repository repository, Predicate<String> pathMatcher);

  /**
   * Retrieve content type for a path within the repository.
   */
  String contentTypeFor(final Repository repository, final String path);

  /**
   * Count the number of assets in the given repository.
   */
  int countAssets(Repository repository);

  /**
   * Count the number of components in the given repository.
   */
  int countComponents(final Repository repository);

  /**
   * Retrieve the attributes for the asset at the given path
   */
  NestedAttributesMap attributes(Repository repository, String path);

  /**
   * Retrieve the format specific attributes for the asset at the given path*
   */
  default NestedAttributesMap formatAttributes(final Repository repository, final String path) {
    return attributes(repository, path).child(repository.getFormat().getValue());
  }

  /**
   * Retrieve the attributes for the component
   */
  NestedAttributesMap componentAttributes(Repository repository, String namespace, String name, String version);

  /**
   * Retrieve the attributes for the component.
   *
   * NOTE: this is intended for formats which do not have versions (e.g. raw)
   */
  NestedAttributesMap componentAttributes(Repository repository, String namespace, String name);

  /**
   * Retrieve the attributes for a snapshot component.
   */
  NestedAttributesMap snapshotComponentAttributes(Repository repository, String name, String version);

  /**
   * Set the last downloaded time for all assets in a repository.
   */
  void setLastDownloadedTime(Repository repository, int minusSeconds);

  /**
   * Set the last downloaded time for assets where path matches regex in a repository.
   */
  void setLastDownloadedTime(Repository repository, int minusSeconds, String regex);

  /**
   * Set the last downloaded time for the asset
   */
  void setLastDownloadedTime(Repository repository, String path, Date date);

  /**
   * Set the last updated time for all components in a repository.
   */
  void setComponentLastUpdatedTime(Repository repository, final Date date);

  /**
   * Semantically set the date the asset was originally created. For Orient this will be the BlobCreated time, for SQL
   * this will be Asset.created
   */
  void setAssetCreatedTime(Repository repository, String path, Date date);

  /**
   * Set the last updated time for all assets in a repository.
   */
  void setAssetLastUpdatedTime(final Repository repository, final Date date);

  /**
   * Set the last updated time for a single asset
   */
  void setAssetLastUpdatedTime(final Repository repository, final String path, final Date date);

  /**
   * Semantically sets the date when the blob was changed. For Orient this is the blobLastUpdated, for SQL this is the
   * AssetBlob.created time.
   */
  void setBlobUpdatedTime(final Repository repository, final String pathRegex, final Date date);

  /**
   * Set the last modified date associated with remote content.
   *
   * Note: For SQL this is only applicable to proxy repositories, while for Orient this is set for both
   */
  void setAssetContentLastModified(Repository repository, String path, Date date);

  /**
   * Set null to the last downloaded time column for all assets in a repository.
   */
  void setLastDownloadedTimeNull(Repository repository);

  /**
   * Set the last downloaded time for any asset matching the pathMatcher in the given repository.
   */
  void setLastDownloadedTime(Repository repository, int minusSeconds, Predicate<String> pathMatcher);

  /**
   * Adjust {@code path} for differences between Orient and Datastore
   */
  String adjustedPath(final String path);

  /**
   * Read an asset from the specified repository.
   */
  Optional<InputStream> read(Repository repository, String path);

  /**
   * Delete the blob associated with the asset with the specified path.
   */
  void deleteAssetBlob(Repository repository, String assetPath);

  /**
   * Get the blob associated with the asset with the specified path.
   */
  Optional<Blob> getBlob(Repository repository, String assetPath);

  /**
   * Obtains a blob ref of an asset in the given repo with the specified path.
   */
  BlobRef getBlobRefOfAsset(Repository repository, String path);

  /**
   * Gets the id of the component associated with the specified asset and repository.
   */
  @Nullable
  EntityId getComponentId(Repository repository, String assetPath);

  NestedAttributesMap getAttributes(Repository repository);

  void modifyAttributes(final Repository repository, String child1, final String child2, final int value);

  /**
   * Deletes all components from the given repository.
   *
   * @param repository the repository  from which all components are going to be deleted
   */
  void deleteAllComponents(final Repository repository);

  /**
   * Do not implement this method, it is not correct to do so. Orient & SQL have different semantics for the lifecycle
   * of blobs:<br/>
   *
   * {@code orient.blobCreated == sql.assetCreated}<br/>
   * {@code orient.blobUpdated == sql.blobCreated}
   *
   * @see #getAssetCreatedTime(Repository, String)
   * @see #getBlobUpdatedTime(Repository, String)
   *
   * @deprecated Do not implement this method
   */
  @Deprecated
  default DateTime getBlobCreatedTime(final Repository repository, final String path) {
    throw new UnsupportedOperationException("Do not implement me");
  }

  class AssetNotFoundException
      extends RuntimeException
  {
    AssetNotFoundException(final Repository repository, final String path) {
      super("Missing asset: " + path + " from repository: " + repository.getName());
    }
  }

  class BlobNotFoundException
      extends RuntimeException
  {
    BlobNotFoundException(final Repository repository, final String path) {
      super("Missing blob for the asset: " + path + " from repository: " + repository.getName());
    }
  }

  class ComponentNotFoundException
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
