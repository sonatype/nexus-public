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
package org.sonatype.nexus.rapture.internal.state

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.common.system.FileDescriptorService
import org.sonatype.nexus.rapture.StateContributor

import static com.google.common.collect.ImmutableMap.of

@Named
@Singleton
class FileDescriptorCheckValueContributor
    extends ComponentSupport
    implements StateContributor
{
  private static final String FILE_DESCRIPTOR_LIMIT = 'file_descriptor_limit'

  private final boolean fileDescriptorLimitOk

  @Inject
  FileDescriptorCheckValueContributor(final FileDescriptorService fileDescriptorService) {
    fileDescriptorLimitOk = fileDescriptorService.isFileDescriptorLimitOk()
  }

  @Override
  Map<String, Object> getState() {
    return of(FILE_DESCRIPTOR_LIMIT, of(FILE_DESCRIPTOR_LIMIT, fileDescriptorLimitOk))
  }
}
