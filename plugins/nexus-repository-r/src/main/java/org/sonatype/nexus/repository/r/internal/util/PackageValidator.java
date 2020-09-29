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
package org.sonatype.nexus.repository.r.internal.util;

import org.sonatype.nexus.rest.ValidationErrorsException;

import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.isValidArchiveExtension;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.isValidRepoPath;

/**
 * Validator for repository packages
 *
 * @since 3.28
 */
public final class PackageValidator
{
  public static final String NOT_VALID_PATH_ERROR_MESSAGE =
      "Provided path is not valid and is expecting src/contrib or bin/<os>/contrib/<R_version>";

  public static final String NOT_VALID_EXTENSION_ERROR_MESSAGE = "Provided extension is not .zip, .tar.gz or .tgz";

  private PackageValidator() {
    // Empty
  }

  /**
   * Validates upload path, filename and extension.
   * <p>
   * Throws {@link org.sonatype.nexus.rest.ValidationErrorsException} if validation failed
   */
  public static void validateArchiveUploadPath(final String fullPath) {
    if (!isValidRepoPath(fullPath)) {
      throw new ValidationErrorsException(NOT_VALID_PATH_ERROR_MESSAGE);
    }
    if (!isValidArchiveExtension(fullPath)) {
      throw new ValidationErrorsException(NOT_VALID_EXTENSION_ERROR_MESSAGE);
    }
  }
}
