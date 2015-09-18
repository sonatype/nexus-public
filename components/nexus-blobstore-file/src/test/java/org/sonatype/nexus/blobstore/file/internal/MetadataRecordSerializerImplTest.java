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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.file.FileBlobMetadata;
import org.sonatype.nexus.blobstore.file.FileBlobState;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link MetadataRecord.SerializerImpl}.
 */
public class MetadataRecordSerializerImplTest
{
  private MetadataRecord.SerializerImpl underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new MetadataRecord.SerializerImpl();
  }

  @Test
  public void roundTrip() throws Exception {
    FileBlobMetadata blobMetadata = new FileBlobMetadata(FileBlobState.CREATING, ImmutableMap.of("Hi", "mom"));
    blobMetadata.setMetrics(new BlobMetrics(new DateTime(), "pretend hash", 33434));

    roundTrip(blobMetadata);
  }

  @Test
  public void roundTripWithEmptyObject() throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(null, null);

    FileBlobMetadata blobMetadata = new FileBlobMetadata(FileBlobState.CREATING, headers);
    blobMetadata.setMetrics(new BlobMetrics(null, null, 0));

    roundTrip(blobMetadata);
  }

  private void roundTrip(final FileBlobMetadata blobMetadata) throws IOException {
    MetadataRecord metadata = new MetadataRecord(blobMetadata);

    byte[] bytes = serialize(metadata);
    MetadataRecord roundTripMetadata = deserialize(bytes);

    assertThat(roundTripMetadata, Matchers.is(equalTo(metadata)));
  }

  private byte[] serialize(MetadataRecord metadata) throws IOException {
    ByteArrayOutputStream buff = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(buff)) {
      underTest.serialize(out, metadata);
      out.flush();
      return buff.toByteArray();
    }
  }

  private MetadataRecord deserialize(final byte[] bytes) throws IOException {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      return underTest.deserialize(in, in.available());
    }
  }
}
