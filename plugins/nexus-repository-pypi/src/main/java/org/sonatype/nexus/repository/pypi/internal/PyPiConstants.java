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
package org.sonatype.nexus.repository.pypi.internal;

/**
 * Various constants used by the PyPI implementation.
 *
 * @since 3.1
 */
public final class PyPiConstants
{
  /**
   * The file upload action.
   */
  public static final String ACTION_FILE_UPLOAD = "file_upload";

  /**
   * The POST field that contains the action (only file_upload is supported).
   */
  public static final String FIELD_ACTION = ":action";

  /**
   * The POST field that contains an upload.
   */
  public static final String FIELD_CONTENT = "content";

  /**
   * The egg-info filename (found in eggs).
   */
  public static final String EGG_INFO_FILENAME = "EGG-INFO";

  /**
   * The pkg-info filename (found in eggs and source distributions).
   */
  public static final String PKG_INFO_FILENAME = "PKG-INFO";

  /**
   * The METADATA filename (backward-compatible metadata file found in wheels).
   */
  public static final String METADATA_FILENAME = "METADATA";

  /**
   * The suffix for egg-info directories in some circumstances.
   */
  public static final String EGG_INFO_SUFFIX = ".egg-info";

  /**
   * The suffix for dist-info directories containing metadata (in wheels).
   */
  public static final String DIST_INFO_SUFFIX = ".dist-info";

  private PyPiConstants() {
    // empty
  }
}
