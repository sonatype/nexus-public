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
package org.sonatype.nexus.repository.metadata;

/**
 * An interface defining raw resource IO contract. This transport is suitable to move payloads that are small enough to
 * not be streamed. Just like repository metadata.
 *
 * @author Eugene Kuleshov
 * @author cstamas
 */
public interface RawTransport
{
  /**
   * Retrieve the raw content of the path from repository. If the path is not found, null should be returned.
   *
   * @return the raw data from path, or null if not found.
   */
  byte[] readRawData(String path)
      throws Exception;

  /**
   * Write the raw content to the path in repository.
   *
   * @return the raw data from path, or null if not found.
   */
  void writeRawData(String path, byte[] data)
      throws Exception;
}
