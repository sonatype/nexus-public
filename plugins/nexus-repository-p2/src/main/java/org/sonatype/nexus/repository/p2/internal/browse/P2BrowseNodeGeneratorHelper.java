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
package org.sonatype.nexus.repository.p2.internal.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Splitter;

import static com.google.common.collect.ImmutableSet.of;

/**
 * @since 3.next
 */
public class P2BrowseNodeGeneratorHelper
{
  private static final Set<String> knownSubDirectories = of("binary", "features", "plugins");

  private P2BrowseNodeGeneratorHelper() {}

  public static List<String> computeComponentPath(final List<String> assetPath, final Optional<String> nameOptional, final Optional<String> versionOptional) {
    List<String> pathParts = new ArrayList<>();
    if (assetPath.size() > 1) {
      pathParts.add(assetPath.get(0));
    }

    nameOptional.ifPresent(name -> {
      String version = versionOptional.orElseThrow(() -> new IllegalStateException("Supplied component name, but not version"));
      if (!knownSubDirectories.contains(assetPath.get(0))) {
        pathParts.add(assetPath.get(1));
      }

      pathParts.addAll(Splitter.on('.').omitEmptyStrings().splitToList(name));
      pathParts.add(version);
    });

    return pathParts;
  }

  public static List<String> splitPath(final String path) {
    return Splitter.on('/').omitEmptyStrings().splitToList(path);
  }
}
