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
package org.sonatype.nexus.yum.client;

import org.sonatype.nexus.yum.client.internal.CompressionType;

import static org.sonatype.nexus.yum.client.internal.CompressionType.BZIP2;
import static org.sonatype.nexus.yum.client.internal.CompressionType.GZIP;

/**
 * @since yum 3.0
 */
public enum MetadataType
{
  PRIMARY_XML("primary", GZIP),

  PRIMARY_SQLITE("primary_db", BZIP2),

  FILELISTs_XML("filelists", GZIP),

  FILELISTS_SQLITE("filelists_db", BZIP2),

  OTHER_XML("other", GZIP),

  OTHER_SQLITE("other_db", BZIP2);

  private final String type;

  private final CompressionType compression;

  private MetadataType(String type, CompressionType compression) {
    this.type = type;
    this.compression = compression;
  }

  public CompressionType getCompression() {
    return compression;
  }

  public String getType() {
    return type;
  }

}
