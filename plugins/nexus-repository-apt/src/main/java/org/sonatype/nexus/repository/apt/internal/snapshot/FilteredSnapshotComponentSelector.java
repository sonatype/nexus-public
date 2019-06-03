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
package org.sonatype.nexus.repository.apt.internal.snapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.Release;

/**
 * @since 3.next
 */
public class FilteredSnapshotComponentSelector
    implements SnapshotComponentSelector
{

  private final ControlFile settings;

  public FilteredSnapshotComponentSelector(ControlFile settings) {
    this.settings = settings;
  }

  @Override
  public List<String> getArchitectures(Release release) {
    Optional<Set<String>> settingsArchitectures = settings.getField("Architectures")
        .map(s -> s.listValue())
        .map(l -> new HashSet<>(l));
    if (settingsArchitectures.isPresent()) {
      Set<String> releaseArchitectures = new HashSet<>(release.getArchitectures());
      releaseArchitectures.retainAll(settingsArchitectures.get());
      return new ArrayList<>(releaseArchitectures);
    }
    else {
      return release.getArchitectures();
    }
  }

  @Override
  public List<String> getComponents(Release release) {
    Optional<Set<String>> settingsComponents = settings.getField("Components").map(s -> s.listValue())
        .map(l -> new HashSet<>(l));
    if (settingsComponents.isPresent()) {
      Set<String> releaseComponents = new HashSet<>(release.getComponents());
      releaseComponents.retainAll(settingsComponents.get());
      return new ArrayList<>(releaseComponents);
    }
    else {
      return release.getComponents();
    }
  }

}
