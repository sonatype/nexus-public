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
package org.sonatype.nexus.blobstore;

import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobId;

/**
 * Interface providing operations to resolve {@link BlobId}s to locations and vice versa.
 *
 * @since 3.8
 */
public interface BlobIdLocationResolver
{
  /**
   * Resolves a {@link BlobId} to path-like {@link String}.
   */
  String getLocation(BlobId blobId);

  /**
   * Special use case to resolve a temporary location, even for non-temporary {@link BlobId}s.
   * Prefer {@link #getLocation(BlobId)}.
   */
  String getTemporaryLocation(BlobId id);

  /**
   * Safely constructs a {@link BlobId} from the {@link Map} argument to
   * {@link org.sonatype.nexus.blobstore.api.BlobStore#create(InputStream, Map)}.
   */
  BlobId fromHeaders(Map<String, String> headers);
}
