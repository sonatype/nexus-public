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
import java.util.function.Consumer;

import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ACCESS_KEY_ID_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SECRET_ACCESS_KEY_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SESSION_TOKEN_KEY;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * {@link AmazonS3Factory} tests.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AmazonS3FactoryTest
{
  @Mock
  S3ServiceClientConfiguration s3Serviceconfiguration;

  MockedStatic<S3Client> s3ClientMockedStatic;

  MockedStatic<CloudWatchMetricPublisher> cloudWatchMetricPublisher;

  @Mock
  S3ClientBuilder s3ClientBuilder;

  @Mock
  S3Client client;

  @Mock
  CloudWatchMetricPublisher.Builder cloudWatchMetricsPublisherBuilder;

  @Mock
  CloudWatchMetricPublisher cloudWatchMetricPublisherInstance;

  private SecretsFactory secretsFactory = mock(SecretsFactory.class);

  @Mock
  private SharedS3CredentialsProvider defaultSharedProvider;

  private AmazonS3Factory amazonS3Factory;

  private MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

  @Before
  public void setup() {
    amazonS3Factory = new AmazonS3Factory(-1, null, false, "", secretsFactory, defaultSharedProvider);
    s3ClientMockedStatic = mockStatic(S3Client.class);
    s3ClientMockedStatic.when(S3Client::builder).thenReturn(s3ClientBuilder);

    cloudWatchMetricPublisher = mockStatic(CloudWatchMetricPublisher.class);
    cloudWatchMetricPublisher.when(CloudWatchMetricPublisher::builder).thenReturn(cloudWatchMetricsPublisherBuilder);
    when(cloudWatchMetricsPublisherBuilder.build()).thenReturn(cloudWatchMetricPublisherInstance);

    when(s3ClientBuilder.httpClient(any(SdkHttpClient.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.serviceConfiguration(any(S3Configuration.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.credentialsProvider(any(AwsCredentialsProvider.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.forcePathStyle(anyBoolean())).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.endpointOverride(any(URI.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.region(any(Region.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.requestChecksumCalculation(any(RequestChecksumCalculation.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.responseChecksumValidation(any(ResponseChecksumValidation.class))).thenReturn(s3ClientBuilder);
    when(s3ClientBuilder.build()).thenReturn(client);

    when(client.serviceClientConfiguration()).thenReturn(s3Serviceconfiguration);
    when(s3Serviceconfiguration.region()).thenReturn(Region.US_EAST_1);

    Map<String, Object> s3Map = new HashMap<>();
    s3Map.put("bucket", "mybucket");
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", s3Map);
    config.setAttributes(attributes);
  }

  @After
  public void tearDown() {
    s3ClientMockedStatic.close();
    cloudWatchMetricPublisher.close();
  }

  @Test
  public void endpointIsSetWhenProvidedInConfig() throws Exception {
    config.getAttributes().get("s3").put("endpoint", "http://localhost/");
    config.getAttributes().get("s3").put("region", "us-west-2");

    amazonS3Factory.create(config);
    verify(s3ClientBuilder).endpointOverride(URI.create("http://localhost/"));
    verify(s3ClientBuilder).region(Region.US_WEST_2);
  }

  @Test
  public void endpointIsSetWhenProvidedInConfigWithDefaultRegion() {
    config.getAttributes().get("s3").put("endpoint", "http://localhost/");

    amazonS3Factory.create(config);
    verify(s3ClientBuilder).endpointOverride(URI.create("http://localhost/"));
  }

  @Test
  public void regionIsSetWhenProvidedInConfig() {
    config.getAttributes().get("s3").put("region", "us-west-2");

    amazonS3Factory.create(config);
    verify(s3ClientBuilder).region(Region.US_WEST_2);
  }

  @Test
  public void cloudWatchMetricsAreEnabledWhenSet() {
    final String givenNamespace = "some-namepace";
    amazonS3Factory = new AmazonS3Factory(-1, null, true, givenNamespace, secretsFactory, defaultSharedProvider);

    amazonS3Factory.create(config);

    verify(cloudWatchMetricsPublisherBuilder).namespace(givenNamespace);
    verify(s3ClientBuilder).overrideConfiguration(any(Consumer.class));
  }

  @Test
  public void pathStyleAccessIsSetWhenProvidedInConfig() {
    config.getAttributes().get("s3").put("region", "us-west-2");
    config.getAttributes().get("s3").put("forcepathstyle", "true");

    amazonS3Factory.create(config);

    verify(s3ClientBuilder).region(Region.US_WEST_2);
    verify(s3ClientBuilder).forcePathStyle(true);
  }

  @Test
  public void checksumConfigurationIsAppliedToBuilder() {
    amazonS3Factory.create(config);

    verify(s3ClientBuilder).requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
    verify(s3ClientBuilder).responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);

    verify(s3ClientBuilder).build();
  }

  @Test
  public void sharedCredentialsProviderIsUsedForS3ClientWhenNoExplicitKeys() {
    AwsCredentialsProvider sharedProvider = mock(AwsCredentialsProvider.class);
    amazonS3Factory = new AmazonS3Factory(-1, null, false, "", secretsFactory, sharedProvider);
    when(s3ClientBuilder.credentialsProvider(sharedProvider)).thenReturn(s3ClientBuilder);

    amazonS3Factory.create(config);

    verify(s3ClientBuilder).credentialsProvider(sharedProvider);
  }

  @Test
  public void sharedCredentialsProviderIsNotUsedWhenExplicitKeysConfigured() {
    AwsCredentialsProvider sharedProvider = mock(AwsCredentialsProvider.class);
    amazonS3Factory = new AmazonS3Factory(-1, null, false, "", secretsFactory, sharedProvider);

    Secret accessKeyMock = mock(Secret.class);
    when(secretsFactory.from("_1")).thenReturn(accessKeyMock);
    when(accessKeyMock.decrypt()).thenReturn("secretAccessKey".toCharArray());
    config.getAttributes().get("s3").put(ACCESS_KEY_ID_KEY, "accessKeyId");
    config.getAttributes().get("s3").put(SECRET_ACCESS_KEY_KEY, "_1");

    amazonS3Factory.create(config);

    verify(s3ClientBuilder, never()).credentialsProvider(sharedProvider);
    verify(s3ClientBuilder).credentialsProvider(any(StaticCredentialsProvider.class));
    // Verify the shared IMDS provider is never resolved — the presigner receives a
    // SharedS3CredentialsProvider wrapper around the explicit StaticCredentialsProvider,
    // not the shared IMDS singleton. Both resolve credentials through the explicit key chain.
    verify(sharedProvider, never()).resolveCredentials();
  }

  @Test
  public void itShouldDecryptTheSecretAccessKeyAndSessionToken() {
    Secret accessKeyMock = mock(Secret.class);
    Secret sessionTokenMock = mock(Secret.class);
    when(secretsFactory.from("_1")).thenReturn(accessKeyMock);
    when(secretsFactory.from("_2")).thenReturn(sessionTokenMock);
    when(accessKeyMock.decrypt()).thenReturn("secretAccessKey".toCharArray());
    when(sessionTokenMock.decrypt()).thenReturn("sessionToken".toCharArray());

    config.getAttributes().get("s3").put(ACCESS_KEY_ID_KEY, "accessKeyId");
    config.getAttributes().get("s3").put(SECRET_ACCESS_KEY_KEY, "_1");
    config.getAttributes().get("s3").put(SESSION_TOKEN_KEY, "_2");
    config.getAttributes().get("s3").put("region", "us-west-2");

    amazonS3Factory.create(config);

    verify(secretsFactory).from("_1");
    verify(secretsFactory).from("_2");
    verify(accessKeyMock).decrypt();
    verify(sessionTokenMock).decrypt();
  }
}
