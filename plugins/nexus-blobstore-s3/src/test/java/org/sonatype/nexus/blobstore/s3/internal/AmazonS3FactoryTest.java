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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ACCESS_KEY_ID_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SECRET_ACCESS_KEY_KEY;

/**
 * {@link AmazonS3Factory} tests.
 */
public class AmazonS3FactoryTest
{
  private SecretsFactory secretsFactory = mock(SecretsFactory.class);

  private AmazonS3Factory amazonS3Factory = new AmazonS3Factory(-1, null, false, "", secretsFactory);

  private MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

  @Before
  public void setup() {
    Map<String, Object> s3Map = new HashMap<>();
    s3Map.put("bucket", "mybucket");
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", s3Map);
    config.setAttributes(attributes);
  }

  @Test
  public void endpointIsSetWhenProvidedInConfig() throws Exception {
    config.getAttributes().get("s3").put("endpoint", "http://localhost/");
    config.getAttributes().get("s3").put("region", "us-west-2");

    AmazonS3 s3 = amazonS3Factory.create(config);
    URI endpoint = (URI) MethodUtils.invokeMethod(s3, true, "getEndpoint");
    assertEquals(new URI("http://localhost/"), endpoint);
  }

  @Test
  public void endpointIsSetWhenProvidedInConfigWithDefaultRegion() throws Exception {
    config.getAttributes().get("s3").put("endpoint", "http://localhost/");

    AmazonS3 s3 = amazonS3Factory.create(config);
    URI endpoint = (URI) MethodUtils.invokeMethod(s3, true, "getEndpoint");
    assertEquals(new URI("http://localhost/"), endpoint);
  }

  @Test
  public void signingAlgorithmIsSetWhenProvidedInConfig() throws Exception {
    config.getAttributes().get("s3").put("signertype", "AWSS3V4SignerType");
    config.getAttributes().get("s3").put("region", "us-west-2");

    AmazonS3 s3 = amazonS3Factory.create(config);
    assertEquals("AWSS3V4SignerType", getSignerOverride(s3));
  }

  @Test
  public void nullSignerDoesNotOverrideConfigValue() throws Exception {
    testSignerOverrideWith(null);
  }

  @Test
  public void emptySignerDoesNotOverrideConfigValue() throws Exception {
    testSignerOverrideWith("");
  }

  @Test
  public void defaultSignerDoesNotOverrideConfigValue() throws Exception {
    testSignerOverrideWith("DEFAULT");
  }

  private void testSignerOverrideWith(String signer) throws Exception {
    config.getAttributes().get("s3").put("region", "us-west-2");
    config.getAttributes().get("s3").put("signertype", signer);

    AmazonS3 s3 = amazonS3Factory.create(config);
    assertNull(getSignerOverride(s3));
  }

  @Test
  public void pathStyleAccessIsSetWhenProvidedInConfig() {
    config.getAttributes().get("s3").put("region", "us-west-2");
    config.getAttributes().get("s3").put("forcepathstyle", "true");

    AmazonS3 s3 = amazonS3Factory.create(config);
    assertEquals("/bucket/key", s3.getUrl("bucket", "key").getPath());
  }

  @Test
  public void itShouldDecryptTheSecretAccessKey() {
    Secret secretMock = mock(Secret.class);
    when(secretsFactory.from("_1")).thenReturn(secretMock);
    when(secretMock.decrypt()).thenReturn("secretAccessKey".toCharArray());
    config.getAttributes().get("s3").put(SECRET_ACCESS_KEY_KEY, "_1");
    config.getAttributes().get("s3").put(ACCESS_KEY_ID_KEY, "accessKeyId");

    amazonS3Factory.create(config);

    verify(secretsFactory).from("_1");
    verify(secretMock).decrypt();
  }

  private String getSignerOverride(AmazonS3 s3) throws Exception {
    Object clientConfiguration = MethodUtils.invokeMethod(s3, true, "getClientConfiguration");
    return (String) MethodUtils.invokeMethod(clientConfiguration, true, "getSignerOverride");
  }
}
