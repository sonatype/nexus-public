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
package org.sonatype.nexus.content.maven.internal.index;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategy;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategyProvider;

import org.apache.maven.index.reader.Record;

/**
 * Group implementation of {@link MavenIndexFacet}.
 *
 * @since 3.26
 */
@Named
public class MavenContentGroupIndexFacet
    extends MavenContentIndexFacetSupport
{
  private final DuplicateDetectionStrategyProvider duplicateDetectionStrategyProvider;

  @Inject
  public MavenContentGroupIndexFacet(
      final MavenIndexPublisher mavenIndexPublisher,
      final DuplicateDetectionStrategyProvider duplicateDetectionStrategyProvider) {
    super(mavenIndexPublisher);
    this.duplicateDetectionStrategyProvider = duplicateDetectionStrategyProvider;
  }

  @Override
  public void publishIndex() throws IOException {
    try (DuplicateDetectionStrategy<Record> strategy = duplicateDetectionStrategyProvider.get()) {
      mavenIndexPublisher.publishGroupIndex(getRepository(), facet(GroupFacet.class).leafMembers(), strategy);
    }
  }
}
