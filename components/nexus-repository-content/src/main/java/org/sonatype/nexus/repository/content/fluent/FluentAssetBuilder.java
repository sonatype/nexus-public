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
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;

/**
 * Fluent API to create/find an asset; at this point we already know the asset path.
 *
 * @since 3.21
 */
public interface FluentAssetBuilder
{
  /**
   * Continue building the asset using the given kind.
   *
   * @since 3.24
   */
  FluentAssetBuilder kind(String kind);

  /**
   * Continue building the asset using the given owning component.
   */
  FluentAssetBuilder component(Component component);

  /**
   * Continue building this asset by converting a temporary blob into a permanent blob and attaching it to this asset.
   *
   * @since 3.30
   */
  FluentAssetBuilder blob(TempBlob blob);

  /**
   * Continue building this asset by attaching an existing blob to this asset.
   *
   * @since 3.30
   */
  FluentAssetBuilder blob(Blob blob, Map<HashAlgorithm, HashCode> checksums);

  /**
   * Continue building the asset using the given attributes.
   *
   * @since 3.31
   */
  FluentAssetBuilder attributes(String key, Object value);

  /**
   * Save the asset using details built so far. If the asset doesn't exist it is created otherwise the blob reference
   * is updated.
   *
   * @since 3.30
   */
  FluentAsset save();

  /**
   * Find by asset path if an asset exists using the details built so far.
   * Fields such as attributes, kind will be ignored.
   */
  Optional<FluentAsset> find();
}
