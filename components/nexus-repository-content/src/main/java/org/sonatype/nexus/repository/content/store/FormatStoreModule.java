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
package org.sonatype.nexus.repository.content.store;

/**
 * Extend this module if your format uses the standard content store APIs.
 * Declare your DAOs and annotate the module with the name of your format:
 *
 * <code><pre>
 * &#64;Named("example")
 * public class ExampleStoreModule
 *     extends FormatStoreModule&lt;ExampleContentRepositoryDAO,
 *                               ExampleComponentDAO,
 *                               ExampleAssetDAO,
 *                               ExampleAssetBlobDAO&gt;
 * {
 *   // nothing to add...
 * }
 * </pre></code>
 *
 * @since 3.24
 */
public abstract class FormatStoreModule<CONTENT_REPOSITORY_DAO extends ContentRepositoryDAO,
                                        COMPONENT_DAO extends ComponentDAO,
                                        ASSET_DAO extends AssetDAO,
                                        ASSET_BLOB_DAO extends AssetBlobDAO>

    extends BespokeFormatStoreModule<ContentRepositoryStore<CONTENT_REPOSITORY_DAO>,
                                     ComponentStore<COMPONENT_DAO>,
                                     AssetStore<ASSET_DAO>,
                                     AssetBlobStore<ASSET_BLOB_DAO>>
{
  // nothing to add...
}
