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
package org.sonatype.nexus.repository.npm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.npm.internal.NpmPackageId;

/**
 * Since 3.16
 */
public class NpmCoordinateUtil
{
  private static final Pattern NPM_VERSION_PATTERN = Pattern
      .compile("-(\\d+\\.\\d+\\.\\d+[A-Za-z\\d\\-.+]*)\\.(?:tar\\.gz|tgz)");


  public static String extractVersion(final String npmPath) {
    Matcher matcher = NPM_VERSION_PATTERN.matcher(npmPath);

    return matcher.find() ? matcher.group(1) : "";
  }

  @Nullable
  public static String getPackageIdScope(final String npmPath) {
    return NpmPackageId.parse(npmPath).scope();
  }

  public static String getPackageIdName(final String npmPath) {
    return NpmPackageId.parse(npmPath).name();
  }

  private NpmCoordinateUtil() {
    // no op
  }
}
