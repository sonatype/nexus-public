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
package org.sonatype.nexus.content.maven.internal.search;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.search.ComponentFinder;
import org.sonatype.nexus.repository.content.search.DefaultComponentFinder;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.search.Maven2SearchResultComponentGenerator;

import static java.util.Comparator.reverseOrder;

/**
 * Maven {@link ComponentFinder} that also includes all snapshots with the same base version.
 *
 * @since 3.26
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2ComponentFinder
    extends DefaultComponentFinder
{
  public static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile( "^(.*-)?([0-9]{8}\\.[0-9]{6}-[0-9]+)$" );

  @Override
  public Stream<FluentComponent> findComponentsByModel(
      final Repository repository,
      final String searchComponentId,
      final String namespace,
      final String name,
      final String version)
  {
    // we take a special approach if the search id is a snapshot coordinate
    if (Maven2SearchResultComponentGenerator.isSnapshotId(searchComponentId)) {
      FluentComponents components = contentFacet(repository).components();
      FluentComponentBuilder builder = components.name(name).namespace(namespace);

      String versionPrefix = version.replace("SNAPSHOT", "");

      // find timestamped versions that match the base version and fetch their components
      return components.versions(namespace, name).stream()
          .filter(v -> v.startsWith(versionPrefix))
          .filter(v -> SNAPSHOT_TIMESTAMP.matcher(v).matches())
          .sorted(reverseOrder())
          .map(v -> builder.version(v).find())
          .filter(Optional::isPresent)
          .map(Optional::get);
    }

    return super.findComponentsByModel(repository, searchComponentId, namespace, name, version);
  }
}
