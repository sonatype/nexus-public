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
package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.utils.SearchComponentPathFilter;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import static org.sonatype.nexus.content.maven.internal.search.table.MavenSearchComponentPathFilter.MavenType.getMavenTypes;

@Named(Maven2Format.NAME)
@Singleton
public class MavenSearchComponentPathFilter
    implements SearchComponentPathFilter
{
  enum MavenType
  {
    POM(".pom"),
    WAR(".war"),
    JAR(".jar"),
    EAR(".ear"),
    AAR(".aar"),
    ZIP(".zip"),
    TARGZ(".tar.gz");

    private final String mavenType;

    MavenType(final String mavenType) {
      this.mavenType = mavenType;
    }

    private String getMavenType() {
      return mavenType;
    }

    static List<String> getMavenTypes() {
      return Arrays.stream(MavenType.values())
          .map(MavenType::getMavenType)
          .collect(Collectors.toList());
    }
  }

  @Override
  public boolean shouldFilterPathExtension(final String path) {
    return getMavenTypes().stream().noneMatch(path::endsWith);
  }
}
