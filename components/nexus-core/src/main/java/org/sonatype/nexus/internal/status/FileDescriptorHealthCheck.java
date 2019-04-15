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
package org.sonatype.nexus.internal.status;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.system.FileDescriptorService;

import static java.lang.String.format;

/**
 * Health check that indicates if the file descriptor limit is below the recommended threshold
 *
 * @since 3.16
 */
@Named("File Descriptors")
@Singleton
public class FileDescriptorHealthCheck
    extends HealthCheckComponentSupport
{
  private FileDescriptorService fileDescriptorService;

  @Inject
  public FileDescriptorHealthCheck(final FileDescriptorService fileDescriptorService) {
    this.fileDescriptorService = fileDescriptorService;
  }

  @Override
  protected Result check() {
    return fileDescriptorService.isFileDescriptorLimitOk() ? Result.healthy() : Result.unhealthy(reason());
  }

  private String reason() {
    return format("Recommended file descriptor limit is %d but count is %d",
        fileDescriptorService.getFileDescriptorRecommended(), fileDescriptorService.getFileDescriptorCount());
  }

}
