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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.function.Supplier;

import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.DefaultBlobStoreProvider;
import org.sonatype.nexus.blobstore.file.FileBlobStoreConfigurationBuilder;

import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;

/**
 * Provider for creating the default browse node configuration
 *
 * @since 3.37
 */
@Named
public class DefaultFileBlobStoreProvider
    implements DefaultBlobStoreProvider
{
  @Override
  public BlobStoreConfiguration get(final Supplier<BlobStoreConfiguration> configurationSupplier) {
    return new FileBlobStoreConfigurationBuilder(DEFAULT_BLOBSTORE_NAME, configurationSupplier).build();
  }
}
