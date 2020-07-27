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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategy;
import org.sonatype.nexus.repository.maven.internal.filter.DuplicateDetectionStrategyProvider;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.maven.index.reader.Record;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Group implementation of {@link MavenIndexFacet}.
 *
 * @since 3.0
 */
@Named
@Priority(Integer.MAX_VALUE)
public class OrientMavenGroupIndexFacet
    extends OrientMavenIndexFacetSupport
{
  private final DuplicateDetectionStrategyProvider duplicateDetectionStrategyProvider;

  @Inject
  public OrientMavenGroupIndexFacet(
      final DuplicateDetectionStrategyProvider duplicateDetectionStrategyProvider,
      final MavenIndexPublisher mavenIndexPublisher)
  {
    super(mavenIndexPublisher);
    this.duplicateDetectionStrategyProvider = checkNotNull(duplicateDetectionStrategyProvider);
  }

  @Override
  public void publishIndex() throws IOException {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try (DuplicateDetectionStrategy<Record> strategy = duplicateDetectionStrategyProvider.get()) {
      mavenIndexPublisher.publishGroupIndex(getRepository(), facet(GroupFacet.class).leafMembers(), strategy);
    }
    finally {
      UnitOfWork.end();
    }
  }
}
