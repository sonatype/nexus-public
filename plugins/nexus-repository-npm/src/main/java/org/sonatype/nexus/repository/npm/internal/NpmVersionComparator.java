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
package org.sonatype.nexus.repository.npm.internal;

import java.util.function.BiFunction;

import org.sonatype.nexus.common.app.VersionComparator;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Utility class for Npm version comparison
 *
 * @since 3.14
 */
public final class NpmVersionComparator
{
  public static final VersionComparator versionComparator = new VersionComparator();

  public static final BiFunction<String, String, String> extractPackageRootVersionUnlessEmpty = (packageRootVersion, packageVersion) ->
      isEmpty(packageRootVersion) ? packageVersion : packageRootVersion;

  public static final BiFunction<String, String, String> extractAlwaysPackageVersion = (packageRootVersion, packageVersion) -> packageVersion;

  public static final BiFunction<String, String, String> extractNewestVersion = (packageRootVersion, packageVersion) ->
      versionComparator.compare(packageVersion, packageRootVersion) > 0
          ? packageVersion : packageRootVersion;

  private NpmVersionComparator() {
    //NOSONAR
  }
}
