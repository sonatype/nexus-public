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
package org.sonatype.nexus.repository.maven.internal.filter;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.maven.index.reader.Record;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides the configured record duplicate detection strategy. Defaults to
 * {@link BloomFilterDuplicateDetectionStrategy}
 */
@Singleton
@Component
public class DuplicateDetectionStrategyProvider
    implements FactoryBean<DuplicateDetectionStrategy<Record>>
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final String strategy;

  private final ApplicationDirectories applicationDirectories;

  private final int maxHeapGb;

  private final int maxDiskSizeGb;

  @Inject
  public DuplicateDetectionStrategyProvider(
      final ApplicationDirectories applicationDirectories,
      @Value("${nexus.maven.duplicate.detection.strategy:BLOOM}") final String strategy,
      @Value("${nexus.maven.duplicate.detection.heap.max.gb:1}") final int maxHeapGb,
      @Value("${nexus.maven.duplicate.detection.disk.max.gb:10}") final int maxDiskSizeGb)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.strategy = checkNotNull(strategy);
    this.maxHeapGb = maxHeapGb;
    this.maxDiskSizeGb = maxDiskSizeGb;
  }

  @Override
  public DuplicateDetectionStrategy<Record> getObject() {
    return switch (getStrategy()) {
      case HASH -> new HashBasedDuplicateDetectionStrategy();
      case DISK -> new DiskBackedDuplicateDetectionStrategy(applicationDirectories, maxHeapGb, maxDiskSizeGb);
      default -> new BloomFilterDuplicateDetectionStrategy();
    };
  }

  private Strategy getStrategy() {
    Strategy duplicateStrategy = null;
    try {
      duplicateStrategy = Strategy.valueOf(this.strategy.toUpperCase());
    }
    catch (IllegalArgumentException e) { // NOSONAR
      log.warn("Unsupported record duplicate detection strategy {}. Falling back to bloom. ", duplicateStrategy);
      duplicateStrategy = Strategy.BLOOM;
    }
    return duplicateStrategy;
  }

  @Override
  public Class<?> getObjectType() {
    return DuplicateDetectionStrategy.class;
  }

  private enum Strategy
  {
    HASH,
    BLOOM,
    DISK
  }
}
