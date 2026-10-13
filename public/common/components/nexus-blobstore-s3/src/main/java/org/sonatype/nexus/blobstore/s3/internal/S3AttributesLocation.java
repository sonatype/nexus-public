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

import org.sonatype.nexus.blobstore.AttributesLocation;

import software.amazon.awssdk.services.s3.model.S3Object;

import static java.util.Objects.requireNonNull;

/**
 * Location for S3BlobStore attributes files
 *
 * @since 3.15
 */
public class S3AttributesLocation
    implements AttributesLocation
{
  private final String key;

  public S3AttributesLocation(final S3Object s3Object) {
    requireNonNull(s3Object);
    this.key = s3Object.key();
  }

  @Override
  public String getFileName() {
    return key.substring(key.lastIndexOf('/') + 1);
  }

  @Override
  public String getFullPath() {
    return key;
  }
}
