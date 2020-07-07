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
 * Extend this module if your format adds methods to its content store APIs.
 * Declare your stores and annotate the module with the name of your format.
 * You can mix custom stores with standard stores in the same bespoke module:
 *
 * <code><pre>
 * &#64;Named("example")
 * public class ExampleStoreModule
 *     extends BespokeFormatStoreModule&lt;ContentRepositoryStore&lt;ExampleContentRepositoryDAO&gt;,
 *                                      ExampleComponentStore,
 *                                      ExampleAssetStore,
 *                                      AssetBlobStore&lt;ExampleAssetBlobDAO&gt;&gt;
 * {
 *   // nothing to add...
 * }
 * </pre></code>
 *
 * In this scenario {@code ExampleContentStore} and {@code ExampleAssetStore}
 * have added methods that query supplementary data held in columns attached
 * to the format's component and asset tables.
 *
 * Most formats don't need this flexibility and should use {@link FormatStoreModule}.
 *
 * @since 3.24
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public abstract class BespokeFormatStoreModule<CONTENT_REPOSITORY_STORE extends ContentRepositoryStore<?>,
                                               COMPONENT_STORE extends ComponentStore<?>,
                                               ASSET_STORE extends AssetStore<?>,
                                               ASSET_BLOB_STORE extends AssetBlobStore<?>>
    extends ContentStoreModule
{
  protected BespokeFormatStoreModule() {
    super(BespokeFormatStoreModule.class);
  }

  @Override
  protected void configure() {
    super.configure();

    bind(FormatStoreManager.class).annotatedWith(format).toInstance(new FormatStoreManager(formatClassPrefix));
  }
}
