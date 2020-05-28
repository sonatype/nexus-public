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
package org.sonatype.nexus.repository.content.facet;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.content.Asset;

/**
 * Something that determines and validates the Content-Type of asset-blobs.
 *
 * @since 3.24
 */
public interface AssetBlobValidator
{
  /**
   * Determines the Content-Type and optionally validates it against the declaration in the blob headers.
   *
   * @return the validated Content-Type
   */
  String determineContentType(Asset asset, Blob blob, boolean strictValidation);
}
