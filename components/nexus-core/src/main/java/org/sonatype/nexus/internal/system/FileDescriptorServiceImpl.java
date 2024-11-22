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

import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.system.FileDescriptorProvider;
import org.sonatype.nexus.common.system.FileDescriptorService;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;

/**
 * @since 3.5
 */
@Named
@ManagedLifecycle(phase = KERNEL)
public class FileDescriptorServiceImpl
    extends StateGuardLifecycleSupport
    implements FileDescriptorService
{
  static final long MINIMUM_FILE_DESCRIPTOR_COUNT = 65536;

  static final String WARNING_HEADER =
      "WARNING: ****************************************************************************";

  static final String WARNING_VIOLATION =
      "WARNING: The open file descriptor limit is {} which is below the minimum recommended value of {}.";

  static final String WARNING_URL =
      "WARNING: Please see: http://links.sonatype.com/products/nexus/system-reqs#filehandles";

  static final long NOT_SUPPORTED = -1; // e.g. Windows does not have the concept of file descriptors

  private final long fileDescriptorCount;

  @Inject
  public FileDescriptorServiceImpl(@Nullable final FileDescriptorProvider fileDescriptorProvider) {
    this.fileDescriptorCount = Optional.ofNullable(fileDescriptorProvider)
        .orElse(new ProcessProbeFileDescriptorProvider())
        .getFileDescriptorCount();
  }

  @Override
  public void doStart() {
    if (!isFileDescriptorLimitOk()) {
      log.warn(WARNING_HEADER);
      log.warn(WARNING_VIOLATION, fileDescriptorCount, MINIMUM_FILE_DESCRIPTOR_COUNT);
      log.warn(WARNING_URL);
      log.warn(WARNING_HEADER);
    }
  }

  @Override
  public boolean isFileDescriptorLimitOk() {
    return fileDescriptorCount >= MINIMUM_FILE_DESCRIPTOR_COUNT || fileDescriptorCount == NOT_SUPPORTED;
  }

  @Override
  public long getFileDescriptorCount() {
    return fileDescriptorCount;
  }

  @Override
  public long getFileDescriptorRecommended() {
    return MINIMUM_FILE_DESCRIPTOR_COUNT;
  }
}
