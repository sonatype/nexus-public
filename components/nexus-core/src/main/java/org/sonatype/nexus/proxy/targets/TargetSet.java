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
package org.sonatype.nexus.proxy.targets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple helper Set implementation.
 *
 * @author cstamas
 */
public class TargetSet
{
  private final Set<TargetMatch> matches = new HashSet<TargetMatch>();

  private final Set<String> matchedRepositoryIds = new HashSet<String>();

  public Set<TargetMatch> getMatches() {
    return Collections.unmodifiableSet(matches);
  }

  public Set<String> getMatchedRepositoryIds() {
    return Collections.unmodifiableSet(matchedRepositoryIds);
  }

  public void addTargetMatch(TargetMatch tm) {
    // TODO: a very crude solution!
    for (TargetMatch t : matches) {
      if (t.getTarget().equals(tm.getTarget())
          && t.getRepository().getId().equals(tm.getRepository().getId())) {
        return;
      }

    }

    matches.add(tm);

    matchedRepositoryIds.add(tm.getRepository().getId());
  }

  public void addTargetSet(TargetSet ts) {
    if (ts == null) {
      return;
    }

    for (TargetMatch tm : ts.getMatches()) {
      addTargetMatch(tm);
    }
  }

}
