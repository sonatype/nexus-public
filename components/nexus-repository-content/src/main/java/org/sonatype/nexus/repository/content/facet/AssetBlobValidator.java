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

import javax.annotation.Nullable;

import org.sonatype.nexus.common.io.InputStreamSupplier;

/**
 * Something that determines and validates the Content-Type of asset-blobs.
 *
 * @since 3.24
 */
public interface AssetBlobValidator
{
  /**
   * Determines the Content-Type and optionally validates it against the declared Content-Type.
   *
   * @param strictValidation    whether the check should be strict or not.
   * @param contentSupplier     the supplier of the content to determine or confirm content type.
   * @param assetPath           blob name, usually a file path or file name or just extension (file extension is used to
   *                            determine content type along with "magic" detection where actual content bits are used,
   *                            like file headers or magic bytes). Is optional, but be aware that if present it improves
   *                            content type detection reliability.
   * @param declaredContentType if non-null, the declared content type will be confirmed, if null, this method will
   *                            attempt to determine the content type.
   * @return the validated Content-Type.
   */
  String determineContentType(
      boolean strictValidation,
      InputStreamSupplier contentSupplier,
      @Nullable String assetPath,
      @Nullable String declaredContentType);
}
