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
package org.sonatype.nexus.bootstrap.osgi;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class NexusEditionFactory
{
  private NexusEditionFactory() {
    throw new IllegalStateException("NexusEditionFactory is a Utility class");
  }

  private static final List<NexusEdition> editions =
      ImmutableList.of(new ProNexusEdition(), new CommunityNexusEdition());

  public static void selectActiveEdition(final Properties properties, final Path workDirPath) {
    NexusEdition nexusEdition = findActiveEdition(editions, properties, workDirPath);
    nexusEdition.apply(properties, workDirPath);
  }

  @VisibleForTesting
  static NexusEdition findActiveEdition(
      final List<NexusEdition> editions,
      final Properties properties,
      final Path workDirPath)
  {
    return editions.stream()
        .filter(edition -> edition.applies(properties, workDirPath))
        .findFirst()
        .orElse(new OpenCoreNexusEdition());
  }
}
