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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;

public class S3PropertiesFileTest
    extends TestSupport
{
  private static final String TEST_PROPERTIES = "propertyName = value\n";

  @Mock
  private AmazonS3 s3;

  @Captor
  private ArgumentCaptor<ByteArrayInputStream> byteArrayCaptor;

  @Captor
  private ArgumentCaptor<ObjectMetadata> metadataCaptor;

  @Test
  public void testLoadIngestsPropertiesFromS3Object() throws Exception {
    S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, "mybucket", "mykey");
    S3Object s3Object = mock(S3Object.class);

    when(s3.getObject("mybucket", "mykey")).thenReturn(s3Object);
    when(s3Object.getObjectContent())
        .thenReturn(new S3ObjectInputStream(new ByteArrayInputStream(TEST_PROPERTIES.getBytes()), null));

    propertiesFile.load();

    assertThat(propertiesFile.getProperty("propertyName"), is("value"));
  }

  @Test
  public void testStoreWritesPropertiesToS3Object() throws Exception {
    S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, "mybucket", "mykey");

    propertiesFile.setProperty("testProperty", "newValue");
    propertiesFile.store();

    verify(s3).putObject(eq("mybucket"), eq("mykey"), byteArrayCaptor.capture(), metadataCaptor.capture());

    String text = new String(byteArrayCaptor.getValue().readAllBytes());
    ObjectMetadata metadata = metadataCaptor.getValue();

    assertThat(text, containsString("testProperty=newValue\n"));
    assertThat(metadata.getContentLength(), is((long) text.length()));
    assertThat(metadata.getUserMetadata(), not(hasKey(TEMPORARY_BLOB_HEADER)));
  }

  @Test
  public void testToStringIsFormattedProperly() {
    S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, "mybucket", "mykey/with/nesting/");

    propertiesFile.setProperty("testProperty", "newValue");
    propertiesFile.setProperty("otherKey", "otherValue");

    assertThat(propertiesFile.toString(),
        is("s3://mybucket/mykey/with/nesting/ {testProperty=newValue, otherKey=otherValue}"));
  }

  @Test
  public void testAddsBlobStoreTemporaryBlobUserMetadataToObjectMetadataWhenBlobStoreTemporaryBlobIsInHeaders() throws Exception {
    S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, "mybucket", "mykey");

    propertiesFile.setProperty(HEADER_PREFIX + TEMPORARY_BLOB_HEADER, "true");
    propertiesFile.store();

    verify(s3).putObject(eq("mybucket"), eq("mykey"), byteArrayCaptor.capture(), metadataCaptor.capture());

    String text = new String(byteArrayCaptor.getValue().readAllBytes());
    ObjectMetadata metadata = metadataCaptor.getValue();

    assertThat(text, containsString("BlobStore.temporary-blob=true\n"));
    assertThat(metadata.getContentLength(), is((long) text.length()));
    assertThat(metadata.getUserMetadata(), hasEntry(TEMPORARY_BLOB_HEADER, "true"));
  }
}
