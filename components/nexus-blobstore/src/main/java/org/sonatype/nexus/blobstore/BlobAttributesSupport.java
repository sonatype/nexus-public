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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobMetrics;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.CONTENT_SIZE_ATTRIBUTE;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.CREATION_TIME_ATTRIBUTE;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.DELETED_ATTRIBUTE;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.DELETED_REASON_ATTRIBUTE;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.DELETED_DATETIME_ATTRIBUTE;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.SHA1_HASH_ATTRIBUTE;

/**
 * A data holder for the content of each blob's .attribs.
 *
 * @since 3.12
 */
public abstract class BlobAttributesSupport<T extends Properties>
    implements BlobAttributes
{
  private Map<String, String> headers;

  private BlobMetrics metrics;

  private boolean deleted = false;

  private String deletedReason;

  private DateTime deletedDateTime;

  protected final T propertiesFile;

  protected BlobAttributesSupport(final T propertiesFile,
                                  @Nullable final Map<String, String> headers,
                                  @Nullable final BlobMetrics metrics) {
    this.propertiesFile = checkNotNull(propertiesFile);
    this.headers = headers;
    this.metrics = metrics;
  }

  @Override
  public Map<String, String> getHeaders() {
    return headers;
  }

  @Override
  public BlobMetrics getMetrics() {
    return metrics;
  }

  @Override
  public boolean isDeleted() {
    return deleted;
  }

  @Override
  public void setDeleted(final boolean deleted) {
    this.deleted = deleted;
  }

  @Override
  public void setDeletedReason(final String deletedReason) {
    this.deletedReason = deletedReason;
  }

  @Override
  public String getDeletedReason() {
    return deletedReason != null ? deletedReason : "No reason supplied";
  }

  @Override
  public DateTime getDeletedDateTime() {
    return deletedDateTime;
  }

  @Override
  public void setDeletedDateTime(final DateTime deletedDateTime) {
    this.deletedDateTime = deletedDateTime;
  }

  @Override
  public Properties getProperties() {
    return new Properties(propertiesFile);
  }

  @Override
  public void updateFrom(final BlobAttributes blobAttributes) {
    headers = blobAttributes.getHeaders();
    metrics = blobAttributes.getMetrics();
    deleted = blobAttributes.isDeleted();
    deletedReason = blobAttributes.getDeletedReason();
    deletedDateTime = blobAttributes.getDeletedDateTime();
  }

  protected void readFrom(Properties properties) {
    headers = new HashMap<>();
    for (Entry<Object, Object> property : properties.entrySet()) {
      String key = (String) property.getKey();
      if (key.startsWith(HEADER_PREFIX)) {
        headers.put(key.substring(HEADER_PREFIX.length()), String.valueOf(property.getValue()));
      }
    }

    metrics = new BlobMetrics(
        new DateTime(Long.parseLong(properties.getProperty(CREATION_TIME_ATTRIBUTE))),
        properties.getProperty(SHA1_HASH_ATTRIBUTE),
        Long.parseLong(properties.getProperty(CONTENT_SIZE_ATTRIBUTE)));

    deleted = properties.containsKey(DELETED_ATTRIBUTE);
    deletedReason = properties.getProperty(DELETED_REASON_ATTRIBUTE);
    deletedDateTime = Optional.ofNullable(properties.getProperty(DELETED_DATETIME_ATTRIBUTE))
        .map(p -> new DateTime(Long.parseLong(p)))
        .orElse(null);
  }

  protected void writeTo(final Properties properties) {
    for (Entry<String, String> header : getHeaders().entrySet()) {
      properties.put(HEADER_PREFIX + header.getKey(), header.getValue());
    }
    BlobMetrics blobMetrics = getMetrics();
    properties.setProperty(SHA1_HASH_ATTRIBUTE, blobMetrics.getSha1Hash());
    properties.setProperty(CONTENT_SIZE_ATTRIBUTE, Long.toString(blobMetrics.getContentSize()));
    properties.setProperty(CREATION_TIME_ATTRIBUTE, Long.toString(blobMetrics.getCreationTime().getMillis()));

    if (deleted) {
      properties.put(DELETED_ATTRIBUTE, Boolean.toString(deleted));
      properties.put(DELETED_REASON_ATTRIBUTE, getDeletedReason());
      if (deletedDateTime != null) {
        properties.put(DELETED_DATETIME_ATTRIBUTE, Long.toString(deletedDateTime.getMillis()));
      }
    }
    else {
      properties.remove(DELETED_ATTRIBUTE);
      properties.remove(DELETED_REASON_ATTRIBUTE);
      properties.remove(DELETED_DATETIME_ATTRIBUTE);
    }
  }

  public String toString() {
    return propertiesFile.toString();
  }
}
