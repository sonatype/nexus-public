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

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class S3PropertiesFileTest
{
  private static final String TEST_PROPERTIES = "propertyName = value\n";

  @Mock
  private EncryptingS3Client s3;

  @Captor
  ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;

  @Captor
  ArgumentCaptor<RequestBody> requestBodyArgumentCaptor;

  @Test
  public void testLoadIngestsPropertiesFromS3Object() throws Exception {
    S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, "mybucket", "mykey");
    ResponseInputStream<GetObjectResponse> responseStream =
        new ResponseInputStream<>(GetObjectResponse.builder().build(),
            new ByteArrayInputStream(TEST_PROPERTIES.getBytes()));

    when(s3.getObject("mybucket", "mykey")).thenReturn(responseStream);

    propertiesFile.load();

    assertThat(propertiesFile.getProperty("propertyName"), is("value"));
  }

  @Test
  public void testStoreWritesPropertiesToS3Object() throws Exception {
    S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, "mybucket", "mykey");

    propertiesFile.setProperty("testProperty", "newValue");
    propertiesFile.store();

    verify(s3).putObject(putObjectRequestCaptor.capture(), requestBodyArgumentCaptor.capture());

    assertThat(putObjectRequestCaptor.getValue().bucket(), is("mybucket"));
    assertThat(putObjectRequestCaptor.getValue().key(), is("mykey"));

    String text = new String(requestBodyArgumentCaptor.getValue().contentStreamProvider().newStream().readAllBytes());

    assertThat(text, containsString("testProperty=newValue\n"));
    assertThat(requestBodyArgumentCaptor.getValue().optionalContentLength().orElse(null), is((long) text.length()));
    assertThat(putObjectRequestCaptor.getValue().metadata(), not(hasKey(TEMPORARY_BLOB_HEADER)));
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

    verify(s3).putObject(putObjectRequestCaptor.capture(), requestBodyArgumentCaptor.capture());

    PutObjectRequest putObjectRequest = putObjectRequestCaptor.getValue();
    RequestBody requestBody = requestBodyArgumentCaptor.getValue();
    String text = new String(requestBody.contentStreamProvider().newStream().readAllBytes());

    assertThat(text, containsString("BlobStore.temporary-blob=true\n"));
    assertThat(requestBody.contentLength(), is((long) text.length()));
    assertThat(putObjectRequest.metadata(), hasEntry(TEMPORARY_BLOB_HEADER, "true"));
  }
}
