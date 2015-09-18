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
package org.sonatype.nexus.repository.storage;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;

/**
 * Storage {@link Facet}, providing component and asset storage for a repository.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface StorageFacet
    extends Facet
{
  /**
   * Key of {@link Component} and {@link Asset} attributes nested map.
   */
  String P_ATTRIBUTES = "attributes";

  /**
   * Key of {@link Asset} blob ref attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  String P_BLOB_REF = "blob_ref";

  /**
   * Key of {@link Component} and {@link Asset} bucket reference attribute.
   */
  String P_BUCKET = "bucket";

  /**
   * Key of {@link Asset} nested map of blob content hashes (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  String P_CHECKSUM = "checksum";

  /**
   * Key of {@link Asset} component reference attribute (if asset belongs to a component).
   */
  String P_COMPONENT = "component";

  /**
   * Key of {@link Asset} for content type attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  String P_CONTENT_TYPE = "content_type";

  /**
   * Key of {@link Component} and {@link Asset} attribute for format reference.
   */
  String P_FORMAT = "format";

  /**
   * Key of {@link Component} group coordinate.
   */
  String P_GROUP = "group";

  /**
   * Key of {@link Component} and {@link Asset} attribute denoting when the record was last updated. This denotes a
   * timestamp when CMA last modified any attribute of the record, and has nothing to do with content change, it's age
   * or it's last modified attributes. This property is present always on {@link Component} and {@link Asset}.
   *
   * @see MetadataNodeEntityAdapter#writeFields(com.orientechnologies.orient.core.record.impl.ODocument, org.sonatype.nexus.repository.storage.MetadataNode)
   */
  String P_LAST_UPDATED = "last_updated";

  /**
   * Key of {@link Asset} attribute denoting when it was last accessed.
   */
  String P_LAST_ACCESSED = "last_accessed";

  /**
   * Key of {@link Component} name coordinate.
   */
  String P_NAME = "name";

  /**
   * Key of {@link Asset} optional path attribute.
   */
  String P_PATH = "path";

  /**
   * Key of {@link Bucket} repository name attribute.
   */
  String P_REPOSITORY_NAME = "repository_name";

  /**
   * Key of {@link Asset} size attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  String P_SIZE = "size";

  /**
   * Key of {@link Component} version coordinate.
   */
  String P_VERSION = "version";

  /**
   * Registers format specific selector for {@link WritePolicy}. If not set, the {@link
   * WritePolicySelector#DEFAULT} is used which returns the configured write policy.
   */
  void registerWritePolicySelector(WritePolicySelector writePolicySelector);

  /**
   * Supplies transactions for use in {@link UnitOfWork}.
   */
  Supplier<StorageTx> txSupplier();
}
