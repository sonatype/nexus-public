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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.file.FileBlobMetadata;
import org.sonatype.nexus.blobstore.file.FileBlobState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.mapdb.Serializer;

import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.blobstore.file.internal.ExternalizationHelper.readNullableLong;
import static org.sonatype.nexus.blobstore.file.internal.ExternalizationHelper.readNullableString;
import static org.sonatype.nexus.blobstore.file.internal.ExternalizationHelper.writeNullableLong;
import static org.sonatype.nexus.blobstore.file.internal.ExternalizationHelper.writeNullableString;

/**
 * Metadata record for internal storage in MapDB.
 *
 * @since 3.0
 */
class MetadataRecord
{
  FileBlobState state;

  Map<String, String> headers;

  boolean metrics;

  DateTime created;

  String sha1;

  Long size;

  /**
   * CTOR for deserialization.
   */
  @VisibleForTesting
  MetadataRecord() {
    // empty
  }

  public MetadataRecord(final FileBlobMetadata source) {
    this.state = source.getBlobState();
    this.headers = Maps.newHashMap(source.getHeaders());
    BlobMetrics metrics = source.getMetrics();
    if (metrics != null) {
      this.metrics = true;
      this.created = metrics.getCreationTime();
      this.sha1 = metrics.getSHA1Hash();
      this.size = metrics.getContentSize();
    }
    else {
      this.metrics = false;
      this.created = null;
      this.sha1 = null;
      this.size = null;
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MetadataRecord that = (MetadataRecord) o;

    if (!Objects.equals(state, that.state)) {
      return false;
    }
    if (!Objects.equals(headers, that.headers)) {
      return false;
    }
    if (metrics != that.metrics) {
      return false;
    }
    if (!Objects.equals(created, that.created)) {
      return false;
    }
    if (!Objects.equals(sha1, that.sha1)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, headers, metrics, created, sha1, size);
  }


  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "state=" + state +
        ", headers=" + headers +
        ", metrics=" + metrics +
        ", created=" + created +
        ", sha1='" + sha1 + '\'' +
        ", size=" + size +
        '}';
  }

  //
  // SerializerImpl
  //

  static class SerializerImpl
      implements Serializer<MetadataRecord>, Serializable
  {
    final static int FORMAT_VERSION = 1;

    @Override
    public void serialize(final DataOutput out, final MetadataRecord value) throws IOException {
      out.writeInt(FORMAT_VERSION);

      out.writeInt(value.state.ordinal());

      out.writeInt(value.headers.size());
      for (Map.Entry<String, String> header : value.headers.entrySet()) {
        writeNullableString(out, header.getKey());
        writeNullableString(out, header.getValue());
      }

      out.writeBoolean(value.metrics);

      if (value.metrics) {
        // writeObject preserves nulls
        writeNullableLong(out, value.created == null ? null : value.created.getMillis());
        writeNullableString(out, value.sha1);
        writeNullableLong(out, value.size);
      }
    }

    @Override
    public MetadataRecord deserialize(final DataInput in, final int available) throws IOException {
      MetadataRecord value = new MetadataRecord();

      final int version = in.readInt();
      checkState(version == FORMAT_VERSION, "Version must be %s", FORMAT_VERSION);

      value.state = FileBlobState.values()[in.readInt()];

      value.headers = Maps.newHashMap();
      final int numberOfHeaders = in.readInt();
      for (int i = 0; i < numberOfHeaders; i++) {
        value.headers.put(readNullableString(in), readNullableString(in));
      }

      value.metrics = in.readBoolean();
      if (value.metrics) {
        final Long createdMillis = readNullableLong(in);
        if (createdMillis != null) {
          value.created = new DateTime(createdMillis);
        }
        value.sha1 = readNullableString(in);
        value.size = readNullableLong(in);
      }
      return value;
    }

    @Override
    public int fixedSize() {
      return -1;
    }
  }
}
