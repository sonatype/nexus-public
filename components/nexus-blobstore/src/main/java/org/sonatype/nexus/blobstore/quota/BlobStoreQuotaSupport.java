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
package org.sonatype.nexus.blobstore.quota;

import java.util.Locale;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.String.format;

/**
 * A base class for {@link BlobStoreQuota} that holds constants which map to config values in {@link
 * BlobStoreConfiguration}
 *
 * @since 3.14
 */
public abstract class BlobStoreQuotaSupport
    extends ComponentSupport
    implements BlobStoreQuota
{
  //Must be in ascending order of exponent
  private enum SIPrefix
  {
    BYTE("B", pow(10, 0)),
    KILO("KB", pow(10, 3)),
    MEGA("MB", pow(10, 6)),
    GIGA("GB", pow(10, 9)),
    TERA("TB", pow(10, 12)),
    PETA("PB", pow(10, 15)),
    EXA("EB", pow(10, 18));

    final String name;

    final double value;

    SIPrefix(final String name, final double value) {
      this.name = name;
      this.value = value;
    }
  }

  public static final String ROOT_KEY = "blobStoreQuotaConfig";

  public static final String TYPE_KEY = "quotaType";

  public static final String LIMIT_KEY = "quotaLimitBytes";

  public static String convertBytesToSI(final long bytes) {
    SIPrefix prefix;

    if (bytes == 0) {
      prefix = SIPrefix.BYTE;
    }
    else {
      double exponent = floor(log10(abs(bytes)));
      prefix = SIPrefix.values()[(int) exponent / 3];
    }

    return format(Locale.ENGLISH, "%.2f %s", bytes / prefix.value, prefix.name);
  }

  public static Runnable createQuotaCheckJob(final BlobStore blobStore,
                                             final BlobStoreQuotaService quotaService,
                                             final Logger logger)
  {
    return () -> quotaCheckJob(blobStore, quotaService, logger);
  }

  @VisibleForTesting
  static void quotaCheckJob(final BlobStore blobStore, final BlobStoreQuotaService quotaService, final Logger logger) {
    try {
      BlobStoreQuotaResult result = quotaService.checkQuota(blobStore);
      if (result != null && result.isViolation()) {
        logger.warn(result.getMessage());
      }
    }
    catch (Exception e) {
      // Don't propagate, as this stops subsequent executions
      logger.error("Quota check exception for {}", blobStore.getBlobStoreConfiguration().getName(), e);
    }
  }

  /**
   * Gets the blob store's quota limit from the blob store's configuration.
   *
   * @return the quota's limit
   * @since 3.15
   */
  public static long getLimit(final BlobStoreConfiguration config) {
    Number limitObj = config.attributes(ROOT_KEY).get(LIMIT_KEY, Number.class);
    checkArgument(limitObj != null, "Limit not found in configuration");
    return limitObj.longValue();
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}
