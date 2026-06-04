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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SharedS3CredentialsProviderTest
{
  @Mock(extraInterfaces = AutoCloseable.class)
  private AwsCredentialsProvider mockDelegate;

  @Test
  void resolveCredentials_delegatesToUnderlying() {
    AwsCredentials expected = AwsBasicCredentials.create("access", "secret");
    when(mockDelegate.resolveCredentials()).thenReturn(expected);

    SharedS3CredentialsProvider provider = new SharedS3CredentialsProvider(mockDelegate);

    AwsCredentials result = provider.resolveCredentials();

    assertSame(expected, result);
    verify(mockDelegate).resolveCredentials();
  }

  @Test
  void close_isNoOp_doesNotCloseDelegate() throws Exception {
    SharedS3CredentialsProvider provider = new SharedS3CredentialsProvider(mockDelegate);

    provider.close();

    // The mock also implements AutoCloseable via Mockito; verify close() was never called on it
    verify((AutoCloseable) mockDelegate, never()).close();
  }

  @Test
  void destroy_closesDelegate() throws Exception {
    SharedS3CredentialsProvider provider = new SharedS3CredentialsProvider(mockDelegate);

    provider.destroy();

    verify((AutoCloseable) mockDelegate).close();
  }
}
