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
package org.sonatype.nexus.repository.maven.internal.group;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.base.Predicate;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;

import static org.sonatype.nexus.common.app.VersionComparator.version;

/**
 * Maven 2 archetype catalog merger.
 *
 * @since 3.0
 */
public class ArchetypeCatalogMerger
    extends ComponentSupport
{
  /**
   * Merges the contents of passed in catalogs
   */
  public void merge(final OutputStream outputStream,
                    final MavenPath mavenPath,
                    final Map<Repository, Content> contents)
  {
    log.debug("Merge archetype catalog for {}", mavenPath.getPath());
    ArchetypeCatalog mergedCatalog = new ArchetypeCatalog();
    UniqueFilter uniqueFilter = new UniqueFilter();

    try {
      for (Map.Entry<Repository, Content> entry : contents.entrySet()) {
        String origin = entry.getKey().getName() + " @ " + mavenPath.getPath();
        ArchetypeCatalog catalog = MavenModels.readArchetypeCatalog(entry.getValue().openInputStream());
        if (catalog == null) {
          log.debug("Corrupted archetype catalog: {}", origin);
          continue;
        }
        for (Archetype archetype : catalog.getArchetypes()) {
          if (uniqueFilter.apply(archetype)) {
            archetype.setRepository(null);
            mergedCatalog.addArchetype(archetype);
          }
        }
      }
      // sort the archetypes
      sortArchetypes(mergedCatalog);
      MavenModels.writeArchetypeCatalog(outputStream, mergedCatalog);
    }
    catch (IOException e) {
      log.error("Unable to merge {}", mavenPath, e);
    }
  }

  private void sortArchetypes(final ArchetypeCatalog mergedCatalog) {
    Collections.sort(mergedCatalog.getArchetypes(),
        (Archetype o1, Archetype o2) ->
        {
          int gc = o1.getGroupId().compareTo(o2.getGroupId());
          if (gc != 0) {
            return gc;
          }
          int ac = o1.getArtifactId().compareTo(o2.getArtifactId());
          if (ac != 0) {
            return ac;
          }
          return version(o1.getVersion()).compareTo(version(o2.getVersion()));
        });
  }

  /**
   * Memory conservative "uniqueness filter" that filters archetypes by keys (GAV), allowing one GAV at the time.
   */
  private static class UniqueFilter
      implements Predicate<Archetype>
  {
    /**
     * G->A->Set(V), just to check for uniqueness.
     */
    private Map<String, Map<String, Set<String>>> gav = new HashMap<>();

    @Override
    public boolean apply(final Archetype input) {
      String g = input.getGroupId();
      String a = input.getArtifactId();
      String v = input.getVersion();
      // G
      Map<String, Set<String>> aMap = gav.get(g);
      if (aMap == null) {
        aMap = new HashMap<>();
        gav.put(g, aMap);
      }
      // A
      Set<String> vSet = aMap.get(a);
      if (vSet == null) {
        vSet = new HashSet<>();
        aMap.put(a, vSet);
      }
      // V
      return vSet.add(v);
    }
  }

}
