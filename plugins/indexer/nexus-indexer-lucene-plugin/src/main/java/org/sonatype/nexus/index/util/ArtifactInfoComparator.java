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
package org.sonatype.nexus.index.util;

import java.util.Comparator;

import org.apache.maven.index.ArtifactInfo;

public class ArtifactInfoComparator
    implements Comparator<ArtifactInfo>
{
  private Comparator<String> stringComparator;

  public ArtifactInfoComparator() {
    this(String.CASE_INSENSITIVE_ORDER);
  }

  public ArtifactInfoComparator(Comparator<String> nameComparator) {
    this.stringComparator = nameComparator;
  }

  public int compare(ArtifactInfo f1, ArtifactInfo f2) {
    int n = stringComparator.compare(f1.groupId, f2.groupId);
    if (n != 0) {
      return n;
    }

    n = stringComparator.compare(f1.artifactId, f2.artifactId);
    if (n != 0) {
      return n;
    }

    n = stringComparator.compare(f1.version, f2.version);
    if (n != 0) {
      return n;
    }

    String c1 = f1.classifier;
    String c2 = f2.classifier;
    if (c1 == null) {
      return c2 == null ? 0 : -1;
    }
    else {
      return c2 == null ? 1 : stringComparator.compare(c1, c2);
    }
  }

}
