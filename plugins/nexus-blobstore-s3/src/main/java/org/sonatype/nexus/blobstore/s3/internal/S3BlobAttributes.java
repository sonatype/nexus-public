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
package org.sonatype.nexus.blobstore.s3.internal;

import java.io.IOException;
import java.util.Map;

import org.sonatype.nexus.blobstore.BlobAttributesSupport;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobMetrics;

import com.amazonaws.services.s3.AmazonS3;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link BlobAttributes} backed by {@link S3PropertiesFile}
 *
 * @since 3.6.1
 */
public class S3BlobAttributes
    extends BlobAttributesSupport<S3PropertiesFile>
{
  public S3BlobAttributes(final AmazonS3 s3, final String bucket, final String key) {
    super(new S3PropertiesFile(s3, bucket, key), null, null);
  }

  public S3BlobAttributes(final AmazonS3 s3, final String bucket, final String key, final Map<String, String> headers,
                          final BlobMetrics metrics) {
    super(new S3PropertiesFile(s3, bucket, key), checkNotNull(headers), checkNotNull(metrics));
  }

  public boolean load() throws IOException {
    if (!propertiesFile.exists()) {
      return false;
    }
    propertiesFile.load();
    readFrom(propertiesFile);
    return true;
  }

  @Override
  public void store() throws IOException {
    writeTo(propertiesFile);
    propertiesFile.store();
  }

  @Override
  public void writeProperties(){
    writeTo(propertiesFile);
  }
}
