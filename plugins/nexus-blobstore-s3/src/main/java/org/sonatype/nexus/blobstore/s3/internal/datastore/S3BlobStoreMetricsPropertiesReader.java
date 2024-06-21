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
package org.sonatype.nexus.blobstore.s3.internal.datastore;

import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.BlobStoreMetricsPropertiesReaderSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;
import org.sonatype.nexus.blobstore.s3.internal.S3PropertiesFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Read {@link BlobStore} metrics from external file (-metrics.properties) saved in S3 bucket.
 */
@Named("S3")
@Singleton
public class S3BlobStoreMetricsPropertiesReader
    extends BlobStoreMetricsPropertiesReaderSupport<S3PropertiesFile>
{
  private static final Map<String, Long> AVAILABLE_SPACE_BY_FILE_STORE = ImmutableMap.of("s3", Long.MAX_VALUE);

  private S3Config s3Configuration;

  @Override
  public void initWithBlobStore(final BlobStore blobStore) throws Exception {
    if (!(blobStore instanceof S3BlobStore)) {
      throw new IllegalArgumentException("BlobStore must be of type S3BlobStore");
    }

    S3BlobStore s3BlobStore = (S3BlobStore) blobStore;
    s3BlobStore.useAmazonS3Config((s3Config -> s3Configuration = s3Config));
  }

  @Override
  protected S3PropertiesFile getProperties() throws Exception {
    String bucket = s3Configuration.getBucket();
    Optional<String> key = s3Configuration.getS3().listObjects(bucket).getObjectSummaries()
        .stream()
        .filter(summary -> summary.getKey().endsWith(metricsFilename()))
        .findFirst()
        .map(S3ObjectSummary::getKey);

    return key.map(metricsKey -> new S3PropertiesFile(s3Configuration.getS3(), bucket, metricsKey))
        .orElse(null);
  }

  @Override
  protected Map<String, Long> getAvailableSpace() throws Exception {
    return AVAILABLE_SPACE_BY_FILE_STORE;
  }

  public static final class S3Config {
    private final AmazonS3 s3;

    private final String bucket;

    public S3Config(final AmazonS3 s3, final String bucket) {
      this.s3 = checkNotNull(s3);
      this.bucket = checkNotNull(bucket);
    }

    public AmazonS3 getS3() {
      return s3;
    }

    public String getBucket() {
      return bucket;
    }
  }
}
