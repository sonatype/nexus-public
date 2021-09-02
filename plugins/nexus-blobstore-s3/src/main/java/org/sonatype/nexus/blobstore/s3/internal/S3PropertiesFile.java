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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.blobstore.CloudBlobPropertiesSupport;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;

/**
 * Persistent properties file stored in AWS S3.
 *
 * @since 3.6.1
 */
public class S3PropertiesFile
    extends CloudBlobPropertiesSupport<ObjectMetadata>
{
  private static final Logger log = LoggerFactory.getLogger(S3PropertiesFile.class);

  private final AmazonS3 s3;

  private final String bucket;

  private final String key;

  public S3PropertiesFile(final AmazonS3 s3, final String bucket, final String key) {
    this.s3 = checkNotNull(s3);
    this.bucket = checkNotNull(bucket);
    this.key = checkNotNull(key);
  }

  public void load() throws IOException {
    log.debug("Loading: {}/{}", bucket, key);

    try (S3Object object = s3.getObject(bucket, key)) {
      try (InputStream inputStream = object.getObjectContent()) {
        load(inputStream);
      }
    }
  }

  @Override
  public ObjectMetadata getMetadata() {
    ObjectMetadata metadata = new ObjectMetadata();
    maybePutTempBlobUserMetadata(metadata);
    return metadata;
  }

  @Override
  public ByteArrayOutputStream getData() throws IOException {
    ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    store(bufferStream, null);
    return bufferStream;
  }

  @Override
  protected void write(final ByteArrayOutputStream data, final ObjectMetadata metadata) {
    log.debug("Storing: {}/{}", bucket, key);
    byte[] buffer = data.toByteArray();
    metadata.setContentLength(buffer.length);
    s3.putObject(bucket, key, new ByteArrayInputStream(buffer), metadata);
  }

  private void maybePutTempBlobUserMetadata(final ObjectMetadata metadata) {
    if (containsKey(HEADER_PREFIX + TEMPORARY_BLOB_HEADER)) {
      metadata.addUserMetadata(TEMPORARY_BLOB_HEADER, "true");
    }
  }

  public boolean exists() throws IOException {
    return s3.doesObjectExist(bucket, key);
  }

  public void remove() throws IOException {
    s3.deleteObject(bucket, key);
  }

  public String toString() {
    return format("s3://%s/%s %s", bucket, key, super.toString());
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof S3PropertiesFile) {
      S3PropertiesFile other = (S3PropertiesFile) object;
      return
          s3.equals(other.s3) &&
              bucket.equals(other.bucket) &&
              key.equals(other.key) &&
              super.equals(object);
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return
        s3.hashCode() +
            bucket.hashCode() +
            key.hashCode() +
            super.hashCode();
  }
}
