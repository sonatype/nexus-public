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
package org.sonatype.plugin.nexus.testenvironment.filter;

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;

public class TestScopeFilter
    extends AbstractArtifactsFilter
{

  @Override
  public boolean isArtifactIncluded(Artifact artifact) {
    if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
      return false;
    }
    return true;
  }

  @SuppressWarnings("rawtypes")
  public Set filter(Set artifacts)
      throws ArtifactFilterException
  {
    for (Iterator iterator = artifacts.iterator(); iterator.hasNext(); ) {
      Artifact artifact = (Artifact) iterator.next();
      if (!isArtifactIncluded(artifact)) {
        iterator.remove();
      }
    }
    return artifacts;
  }
}
