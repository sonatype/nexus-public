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
package org.sonatype.nexus.repository.content.fluent;

import java.time.OffsetDateTime;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * Fluent API for a particular asset.
 *
 * @since 3.21
 */
public interface FluentAsset
    extends Asset, FluentAttributes<FluentAsset>, FluentAssetBlobAttach
{
  /**
   * The repository containing this asset.
   *
   * @since 3.24
   */
  Repository repository();

   /**
   * Downloads this asset.
   */
  Content download();

  /**
   * Mark this asset as recently downloaded by a user action.
   */
  FluentAsset markAsDownloaded();

  /**
   * Mark this proxied asset as recently cached from the given content.
   *
   * @since 3.25
   */
  FluentAsset markAsCached(Payload content);

  /**
   * Mark this generated asset as recently cached.
   *
   * @since 3.24
   */
  FluentAsset markAsCached(CacheInfo cacheInfo);

  /**
   * Mark this generated/proxied asset as stale.
   *
   * @since 3.24
   */
  FluentAsset markAsStale();

  /**
   * Is this generated/proxied asset stale?
   *
   * @since 3.24
   */
  boolean isStale(CacheController cacheController);

  /**
   * Update this asset to have the given kind.
   *
   * @since 3.25
   */
  FluentAsset kind(String kind);

  /**
   * Deletes this asset.
   */
  boolean delete();

  /**
   * Generally it is recommended that this method not be called and let stores manage this value.
   *
   * @since 3.29
   */
  void blobCreated(OffsetDateTime blobCreated);

  /**
   * Sets added to repository on the asset blob.
   */
  void blobAddedToRepository(final OffsetDateTime addedToRepository);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the created time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void created(OffsetDateTime created);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the last download time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void lastDownloaded(OffsetDateTime lastDownloaded);

  /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the last updated time of the asset associated with the ID to the specified time.
   *
   * @since 3.29
   */
  void lastUpdated(OffsetDateTime lastUpdated);

  /**
   * Applies the given change to the current attributes.
   */
  FluentAsset attributes(AttributeChangeSet changeset);

   /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the name of user who uploaded the asset.
   */
  void createdBy(String createdBy);

   /**
   * Generally it is recommended that this method not be called and let stores manage this value automatically.
   *
   * Sets the IP addres of user who uploaded the asset.
   */
  void createdByIP(String createdByIP);
}
