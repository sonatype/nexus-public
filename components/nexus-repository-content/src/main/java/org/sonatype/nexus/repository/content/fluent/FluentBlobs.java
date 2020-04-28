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
package org.sonatype.nexus.repository.content.fluent;

import java.io.InputStream;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

/**
 * Fluent API for ingesting blobs.
 *
 * @since 3.21
 */
public interface FluentBlobs
{
  /**
   * Ingests the given stream as a temporary blob with the requested hashing.
   */
  TempBlob ingest(InputStream in, @Nullable String contentType, Iterable<HashAlgorithm> hashing);

  /**
   * Ingests the given payload as a temporary blob with the requested hashing.
   */
  TempBlob ingest(Payload payload, Iterable<HashAlgorithm> hashing);
}
