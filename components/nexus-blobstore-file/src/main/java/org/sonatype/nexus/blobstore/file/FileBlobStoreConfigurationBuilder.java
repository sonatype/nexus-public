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
package org.sonatype.nexus.blobstore.file;

import java.util.function.Supplier;

import org.sonatype.nexus.blobstore.BlobStoreConfigurationBuilder;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder to configure a file blob store.
 *
 * @since 3.1
 */
public class FileBlobStoreConfigurationBuilder
    extends BlobStoreConfigurationBuilder
{
  private String path;

  /**
   * Creates a new builder using the specified name for the resulting blob store. Unless customized, the name is also
   * used as the path for the blob store.
   */
  public FileBlobStoreConfigurationBuilder(
      final String name,
      final Supplier<BlobStoreConfiguration> configurationSupplier)
  {
    super(name, configurationSupplier);
    type(FileBlobStore.TYPE);
    this.path = name;
  }

  /**
   * Sets the local path where the blobs are persisted.
   */
  public FileBlobStoreConfigurationBuilder path(final String path) {
    this.path = checkNotNull(path);
    return this;
  }

  /**
   * Creates the configuration for the desired file blob store.
   */
  @Override
  public BlobStoreConfiguration build() {
    final BlobStoreConfiguration configuration = super.build();
    configuration.attributes(FileBlobStore.CONFIG_KEY).set(FileBlobStore.PATH_KEY, path);
    return configuration;
  }
}
