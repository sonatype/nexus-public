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
package org.sonatype.nexus.repository.content.search.upgrade;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.search.index.SearchUpdateService;
import org.sonatype.nexus.repository.types.GroupType;

@Named
@Singleton
public class SearchUpdateServiceImpl
    implements SearchUpdateService
{
  @Override
  public boolean needsReindex(final Repository repository) {
    if (GroupType.NAME.equals(repository.getType().getValue())) {
      return false;
    }
    ContentFacet contentFacet = repository.facet(ContentFacet.class);
    Object indexOutdated = contentFacet.attributes().get(SEARCH_INDEX_OUTDATED);
    if (indexOutdated instanceof Boolean) {
      return (Boolean) indexOutdated;
    }
    return false;
  }

  @Override
  public void doneReindexing(final Repository repository) {
    ContentFacet contentFacet = repository.facet(ContentFacet.class);
    contentFacet.withoutAttribute(SEARCH_INDEX_OUTDATED);
  }
}
