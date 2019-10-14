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
package org.sonatype.nexus.proxy.storage;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Simple Path-based whitelist to validate allowed storage locations for repositories. Locations are whitelisted
 * by adding them as comma-separated values to the {@code nexus.override.local.storage.whitelist} property in
 * {@code nexus.properties}.
 *
 * A whitelisted path has its subdirectories included. For example, whitelisting the directory {@code /foo/bar} would
 * allow repositories to set their storage to {@code /foo/bar/baz}.
 *
 * @since 2.14.15
 */
@Named
@Singleton
public class StorageWhitelist
{
  public static final String PROP_NAME = "nexus.override.local.storage.whitelist";

  private static final String INJECTED_PROP = "${" + PROP_NAME + ":-}";

  private final Set<Path> whitelistPaths;

  @Inject
  public StorageWhitelist(@Named(INJECTED_PROP) final String whitelistPaths) {
    this.whitelistPaths = isBlank(whitelistPaths) ? new HashSet<>() :
        stream(whitelistPaths.split(",")).map(this::parse).map(Path::normalize).collect(toSet());
  }

  void addWhitelistPath(final String whitelistPath) {
    this.whitelistPaths.add(parse(whitelistPath).normalize());
  }

  public boolean isApproved(final String path) {
    Path targetPath = parse(path).normalize();
    return whitelistPaths.stream().anyMatch(targetPath::startsWith);
  }

  /**
   * Attempts to generate a {@link Path} from a string first assuming the string is formatted as a {@link URI} and
   * subsequently as a direct path if not
   *
   * @param pathString A path string
   * @return the {@link Path} resulting from {@code pathString}
   */
  private Path parse(final String pathString) {
    try {
      return Paths.get(URI.create(pathString));
    }
    catch (IllegalArgumentException e) {
      return Paths.get(pathString);
    }
  }
}
