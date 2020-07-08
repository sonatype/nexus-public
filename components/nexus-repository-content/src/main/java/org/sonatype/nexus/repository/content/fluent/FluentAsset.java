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

import java.util.Map;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;

/**
 * Fluent API for a particular asset.
 *
 * @since 3.21
 */
public interface FluentAsset
    extends Asset, FluentAttributes<FluentAsset>
{
  /**
   * The repository containing this asset.
   *
   * @since 3.24
   */
  Repository repository();

  /**
   * Converts a temporary blob into a permanent blob and attaches it to this asset.
   */
  FluentAsset attach(TempBlob blob);

  /**
   * Attaches an existing blob to this asset.
   */
  FluentAsset attach(Blob blob, Map<HashAlgorithm, HashCode> checksums);

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
}
