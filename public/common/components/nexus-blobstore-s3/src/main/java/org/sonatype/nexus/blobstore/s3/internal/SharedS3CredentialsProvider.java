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

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * A singleton {@link AwsCredentialsProvider} shared across all S3 clients and presigners.
 *
 * <p>
 * This class wraps a single {@link DefaultCredentialsProvider} instance so that all blobstores
 * in a deployment share one credential chain rather than each creating their own independent IMDS
 * client. Without sharing, concurrent blobstore startup and operation bursts IMDS requests
 * simultaneously (e.g. 2500+), exhausting the instance metadata endpoint.
 *
 * <p>
 * The {@link #close()} method is intentionally a no-op. AWS SDK bug #4386 causes closing an
 * {@link software.amazon.awssdk.services.s3.presigner.S3Presigner} to call {@code close()} on
 * its credentials provider, which would destroy a shared STS client used by other live presigners
 * and S3 clients. By making {@code close()} a no-op on this wrapper, presigner lifecycle events
 * cannot destroy the underlying provider. The real {@link DefaultCredentialsProvider} is only
 * closed when this singleton bean is destroyed at application shutdown via {@link #destroy()}.
 */
@Component("sharedS3CredentialsProvider")
public class SharedS3CredentialsProvider
    implements AwsCredentialsProvider, AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(SharedS3CredentialsProvider.class);

  private final AwsCredentialsProvider delegate;

  public SharedS3CredentialsProvider() {
    this.delegate = DefaultCredentialsProvider.create();
  }

  @VisibleForTesting
  SharedS3CredentialsProvider(final AwsCredentialsProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public AwsCredentials resolveCredentials() {
    return delegate.resolveCredentials();
  }

  /**
   * Intentional no-op. See class-level JavaDoc for the rationale.
   */
  @Override
  public void close() {
    // deliberately empty — closing this wrapper must not close the shared delegate
  }

  /**
   * Closes the underlying delegate. Called by the Spring container at application shutdown.
   */
  @PreDestroy
  public void destroy() {
    if (delegate instanceof AutoCloseable closeable) {
      try {
        closeable.close();
      }
      catch (Exception e) {
        log.warn("Error closing shared credentials provider delegate", e);
      }
    }
  }
}
