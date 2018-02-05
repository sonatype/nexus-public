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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.internal.MavenIndexFacetSupport;
import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * Group implementation of {@link MavenIndexFacet}.
 *
 * @since 3.0
 */
@Named
public class IndexGroupFacet
    extends MavenIndexFacetSupport
{
  @Override
  public void publishIndex() throws IOException {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      List<Repository> leafMembers = facet(GroupFacet.class).leafMembers();
      ArrayList<String> withoutIndex = new ArrayList<>();
      for (Iterator<Repository> ri = leafMembers.iterator(); ri.hasNext(); ) {
        Repository repository = ri.next();
        if (repository.facet(MavenIndexFacet.class).lastPublished() == null) {
          withoutIndex.add(repository.getName());
          ri.remove();
        }
      }
      if (!withoutIndex.isEmpty()) {
        log.info("Following members of group {} have no index, will not participate in merged index: {}",
            getRepository().getName(),
            withoutIndex
        );
      }
      MavenIndexPublisher.publishMergedIndex(getRepository(), leafMembers);
    }
    finally {
      UnitOfWork.end();
    }
  }
}
