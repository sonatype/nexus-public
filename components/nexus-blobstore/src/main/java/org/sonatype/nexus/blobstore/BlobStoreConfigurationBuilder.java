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
package org.sonatype.nexus.blobstore;

import java.util.function.Supplier;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for blob store configurations.
 *
 * @since 3.36
 */
public class BlobStoreConfigurationBuilder
{
  private final Supplier<BlobStoreConfiguration> configurationSupplier;

  private final String name;

  private String type;

  private String quotaType;

  private long quotaLimit;

  /**
   * Creates a new builder using the specified name for the resulting blob store.
   */
  public BlobStoreConfigurationBuilder(
      final String name,
      final Supplier<BlobStoreConfiguration> configurationSupplier)
  {
    this.name = checkNotNull(name);
    this.configurationSupplier = checkNotNull(configurationSupplier);
  }

  /**
   * Sets the type of blob store.
   */
  public BlobStoreConfigurationBuilder type(final String type) {
    this.type = checkNotNull(type);
    return this;
  }

  /**
   * Sets the blob store quota configuration.
   */
  public BlobStoreConfigurationBuilder quotaConfig(final String quotaType, final long limit) {
    this.quotaType = quotaType;
    this.quotaLimit = limit;
    return this;
  }

  /**
   * Creates the configuration for the desired blob store.
   */
  public BlobStoreConfiguration build() {
    final BlobStoreConfiguration configuration = configurationSupplier.get();
    configuration.setName(name);
    configuration.setType(type);
    if (quotaType != null) {
      configuration.attributes(BlobStoreQuotaSupport.ROOT_KEY).set(BlobStoreQuotaSupport.TYPE_KEY, quotaType);
      configuration.attributes(BlobStoreQuotaSupport.ROOT_KEY).set(BlobStoreQuotaSupport.LIMIT_KEY, quotaLimit);
    }
    return configuration;
  }
}
