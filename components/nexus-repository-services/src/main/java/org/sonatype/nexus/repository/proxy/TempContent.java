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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Temporary reusable {@link Content}; caches the original remote content in-memory.
 *
 * @since 3.4
 */
class TempContent
    extends Content
{
  public TempContent(final Content remote) throws IOException {
    super(cachePayload(remote), remote.getAttributes());
  }

  private static Payload cachePayload(final Content remote) throws IOException {
    try (InputStream in = remote.openInputStream()) {
      return new BytesPayload(toByteArray(in), remote.getContentType());
    }
  }
}
