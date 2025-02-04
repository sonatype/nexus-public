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
package org.sonatype.nexus.internal.system;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.system.FileDescriptorProvider;

import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.internal.system.FileDescriptorServiceImpl.MINIMUM_FILE_DESCRIPTOR_COUNT;
import static org.sonatype.nexus.internal.system.FileDescriptorServiceImpl.NOT_SUPPORTED;
import static org.sonatype.nexus.internal.system.FileDescriptorServiceImpl.WARNING_HEADER;
import static org.sonatype.nexus.internal.system.FileDescriptorServiceImpl.WARNING_URL;
import static org.sonatype.nexus.internal.system.FileDescriptorServiceImpl.WARNING_VIOLATION;

public class FileDescriptorServiceImplTest
    extends TestSupport
{
  @Mock
  private Logger mockLogger;

  private TestFileDescriptorProvider fileDescriptorProvider = new TestFileDescriptorProvider();

  @Test
  public void getFileDescriptorCount() {
    assertFileDescriptorService(NOT_SUPPORTED, true);
    assertFileDescriptorService(1, false);
    assertFileDescriptorService(MINIMUM_FILE_DESCRIPTOR_COUNT - 1, false);
    assertFileDescriptorService(MINIMUM_FILE_DESCRIPTOR_COUNT, true);
    assertFileDescriptorService(MINIMUM_FILE_DESCRIPTOR_COUNT + 1, true);
  }

  @Test
  public void logOnBadFileDescriptors() {
    FileDescriptorServiceImpl fileDescriptorService = new TestFileDescriptorService(fileDescriptorProvider.with(10L));
    fileDescriptorService.doStart();
    fileDescriptorService.isFileDescriptorLimitOk();
    verify(mockLogger, times(2)).warn(eq(WARNING_HEADER));
    verify(mockLogger).warn(eq(WARNING_URL));
    verify(mockLogger).warn(eq(WARNING_VIOLATION), eq(10L), eq(MINIMUM_FILE_DESCRIPTOR_COUNT));
  }

  @Test
  public void doNotLogOnGoodFileDescriptors() {
    FileDescriptorServiceImpl fileDescriptorService =
        new TestFileDescriptorService(fileDescriptorProvider.with(MINIMUM_FILE_DESCRIPTOR_COUNT));
    fileDescriptorService.doStart();
    fileDescriptorService.isFileDescriptorLimitOk();
    verify(mockLogger, never()).warn(eq(WARNING_HEADER));
    verify(mockLogger, never()).warn(eq(WARNING_URL));
    verify(mockLogger, never()).warn(eq(WARNING_VIOLATION), eq(10L), eq(MINIMUM_FILE_DESCRIPTOR_COUNT));
  }

  private void assertFileDescriptorService(final long count, final boolean isOk) {
    FileDescriptorServiceImpl fileDescriptorService = new TestFileDescriptorService(fileDescriptorProvider.with(count));
    assertThat(fileDescriptorService.getFileDescriptorCount(), equalTo(count));
    assertThat(fileDescriptorService.getFileDescriptorRecommended(), equalTo(MINIMUM_FILE_DESCRIPTOR_COUNT));
    assertThat(fileDescriptorService.isFileDescriptorLimitOk(), equalTo(isOk));
  }

  private class TestFileDescriptorProvider
      implements FileDescriptorProvider
  {
    private long override = NOT_SUPPORTED;

    @Override
    public long getFileDescriptorCount() {
      return override;
    }

    FileDescriptorProvider with(long override) {
      this.override = override;
      return this;
    }
  }

  private class TestFileDescriptorService
      extends FileDescriptorServiceImpl
  {
    TestFileDescriptorService(FileDescriptorProvider fileDescriptorProvider) {
      super(fileDescriptorProvider);
    }

    @Override
    protected Logger createLogger() {
      return mockLogger;
    }
  }
}
