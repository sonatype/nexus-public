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
import java.util.Optional;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import com.google.common.base.Predicates;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.Duration.ofMillis;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ACCESS_KEY_ID_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ASSUME_ROLE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.ENDPOINT_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.FORCE_PATH_STYLE_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.MAX_CONNECTION_POOL_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SECRET_ACCESS_KEY_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.SESSION_TOKEN_KEY;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

/**
 * Creates configured AmazonS3 clients.
 *
 * @since 3.6.1
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AmazonS3Factory
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String DEFAULT = "DEFAULT";

  private final int defaultConnectionPoolSize;

  private final boolean cloudWatchMetricsEnabled;

  private final String cloudWatchMetricsNamespace;

  private final Time connectionTtl;

  private final SecretsFactory secretsFactory;

  @Inject
  public AmazonS3Factory(
      @Value("${nexus.s3.connection.pool:-1}") final int connectionPoolSize,
      @Nullable @Value("${nexus.s3.connection.ttl:#{null}}") final Time connectionTtl,
      @Value("${nexus.s3.cloudwatchmetrics.enabled:false}") final boolean cloudWatchMetricsEnabled,
      @Value("${nexus.s3.cloudwatchmetrics.namespace:nexus-blobstore-s3}") final String cloudWatchMetricsNamespace,
      final SecretsFactory secretsFactory)
  {
    this.defaultConnectionPoolSize = connectionPoolSize;
    this.cloudWatchMetricsEnabled = cloudWatchMetricsEnabled;
    this.cloudWatchMetricsNamespace = cloudWatchMetricsNamespace;
    this.connectionTtl = connectionTtl;
    this.secretsFactory = checkNotNull(secretsFactory);
  }

  public EncryptingS3Client create(final BlobStoreConfiguration blobStoreConfiguration) {
    S3ClientBuilder builder = S3Client.builder();

    NestedAttributesMap s3Configuration = blobStoreConfiguration.attributes(CONFIG_KEY);
    String accessKeyId = s3Configuration.get(ACCESS_KEY_ID_KEY, String.class);
    String secretAccessKey = s3Configuration.get(SECRET_ACCESS_KEY_KEY, String.class);
    String region = S3BlobStoreConfigurationHelper.getConfiguredRegion(blobStoreConfiguration);
    String forcePathStyle = s3Configuration.get(FORCE_PATH_STYLE_KEY, String.class);

    int maximumConnectionPoolSize = Optional.ofNullable(s3Configuration.get(MAX_CONNECTION_POOL_KEY, String.class))
        .filter(Predicates.not(Strings2::isBlank))
        .map(Integer::valueOf)
        .orElse(-1);

    AwsCredentialsProvider credentialsProvider = null;
    if (!isNullOrEmpty(accessKeyId) && !isNullOrEmpty(secretAccessKey)) {
      String decryptedSessionToken = getSessionToken(s3Configuration);
      String decryptedAccessKey = new String(secretsFactory.from(secretAccessKey).decrypt());
      AwsCredentials credentials = buildCredentials(accessKeyId, decryptedAccessKey, decryptedSessionToken);

      String assumeRole = s3Configuration.get(ASSUME_ROLE_KEY, String.class);
      credentialsProvider = buildCredentialsProvider(credentials, region, assumeRole);

      builder = builder.credentialsProvider(credentialsProvider);
    }

    String endpoint = s3Configuration.get(ENDPOINT_KEY, String.class);
    if (!isNullOrEmpty(endpoint)) {
      builder = builder.endpointOverride(URI.create(endpoint));
    }

    if (!isNullOrEmptyOrDefault(region)) {
      builder = builder.region(Region.of(region));
    }

    // === connection settings ===
    ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
    if (defaultConnectionPoolSize > 0 || maximumConnectionPoolSize > 0) {
      httpClientBuilder
          .maxConnections(maximumConnectionPoolSize > 0 ? maximumConnectionPoolSize : defaultConnectionPoolSize);
    }
    if (connectionTtl != null) {
      httpClientBuilder.connectionTimeToLive(ofMillis(connectionTtl.toMillis()));
    }

    builder = builder.httpClient(httpClientBuilder.build());
    // === end connection settings ===

    builder = builder.forcePathStyle(Boolean.parseBoolean(forcePathStyle));

    if (cloudWatchMetricsEnabled) {
      // Converted from AWS SDK V1 to V2 according to following this AWS blog
      // https://aws.amazon.com/blogs/developer/using-the-new-client-side-metrics-feature-in-the-aws-sdk-for-java-v2/
      CloudWatchMetricPublisher.Builder cloudWatchMetricPublisher = CloudWatchMetricPublisher.builder();

      if (nonNull(cloudWatchMetricsNamespace)) {
        cloudWatchMetricPublisher.namespace(cloudWatchMetricsNamespace);
      }

      builder.overrideConfiguration(o -> o.addMetricPublisher(cloudWatchMetricPublisher.build()));
      log.info("CloudWatch metrics enabled using namespace {}", cloudWatchMetricsNamespace);
    }

    return new EncryptingS3Client(builder.build(), blobStoreConfiguration);
  }

  private AwsCredentials buildCredentials(
      final String accessKeyId,
      final String secretAccessKey,
      final String sessionToken)
  {
    if (isNullOrEmpty(sessionToken)) {
      return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    }
    else {
      return AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken);
    }
  }

  private AwsCredentialsProvider buildCredentialsProvider(
      final AwsCredentials credentials,
      final String region,
      final String assumeRole)
  {
    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
    if (isNullOrEmpty(assumeRole)) {
      return credentialsProvider;
    }
    else {
      // STS requires a region; fall back on the SDK default if not set
      String stsRegion;
      if (isNullOrEmpty(region)) {
        stsRegion = defaultRegion();
      }
      else {
        stsRegion = region;
      }

      StsClient stsClient = StsClient.builder()
          .region(Region.of(stsRegion))
          .credentialsProvider(credentialsProvider)
          .build();

      return StsAssumeRoleCredentialsProvider.builder()
          .stsClient(stsClient)
          .refreshRequest(request -> request.roleArn(assumeRole).roleSessionName("nexus-s3-session"))
          .build();
    }
  }

  private String defaultRegion() {
    try {
      return DefaultAwsRegionProviderChain.builder().build().getRegion().id();
    }
    catch (SdkClientException e) {
      String region = Region.US_EAST_1.id();
      log.warn("Default AWS region not configured, using {}", region, e);
      return region;
    }
  }

  private String getSessionToken(final NestedAttributesMap s3Configuration) {
    if (s3Configuration.contains(SESSION_TOKEN_KEY)) {
      return new String(secretsFactory.from(s3Configuration.get(SESSION_TOKEN_KEY, String.class)).decrypt());
    }
    return null;
  }

  private boolean isNullOrEmptyOrDefault(final String value) {
    return isNullOrEmpty(value) || DEFAULT.equals(value);
  }
}
