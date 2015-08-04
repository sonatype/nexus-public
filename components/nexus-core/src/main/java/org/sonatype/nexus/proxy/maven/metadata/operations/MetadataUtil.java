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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import java.util.List;

import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.codehaus.plexus.util.StringUtils;

public class MetadataUtil
{

  public static SnapshotVersion searchForEquivalent(SnapshotVersion source, List<SnapshotVersion> list) {
    for (SnapshotVersion equivalent : list) {
      if (StringUtils.equals(source.getExtension(), equivalent.getExtension())
          && ((StringUtils.isEmpty(source.getClassifier()) && StringUtils.isEmpty(equivalent.getClassifier())) ||
          StringUtils.equals(
              source.getClassifier(), equivalent.getClassifier()))) {
        return equivalent;
      }
    }
    return null;
  }

  public static boolean isPluginEquals(Plugin p1, Plugin p2) {
    if (p1.getName() == null) {
      p1.setName("");
    }

    if (p2.getName() == null) {
      p2.setName("");
    }

    return StringUtils.equals(p1.getArtifactId(), p2.getArtifactId())
        && StringUtils.equals(p1.getPrefix(), p2.getPrefix()) && StringUtils.equals(p1.getName(), p2.getName());
  }

  public static boolean isPluginPrefixAndArtifactIdEquals(Plugin p1, Plugin p2) {
    return StringUtils.equals(p1.getArtifactId(), p2.getArtifactId())
        && StringUtils.equals(p1.getPrefix(), p2.getPrefix());
  }

}
