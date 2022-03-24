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
package org.sonatype.nexus.repository.search.upgrade.orient;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.search.index.SearchUpdateService;
import org.sonatype.nexus.repository.types.GroupType;

/**
 * @since 3.37
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientSearchUpdateService
    implements SearchUpdateService
{
  @Override
  public boolean needsReindex(final Repository repository) {
    if (GroupType.NAME.equals(repository.getType().getValue())) {
      return false;
    }
    AttributesFacet attributesFacet = repository.facet(AttributesFacet.class);
    Object indexOutdated =
        attributesFacet.getAttributes().get(SEARCH_INDEX_OUTDATED);
    if (indexOutdated instanceof Boolean) {
      return (Boolean) indexOutdated;
    }
    return false;
  }

  @Override
  public void doneReindexing(final Repository repository) {
    repository.facet(AttributesFacet.class)
        .modifyAttributes((final NestedAttributesMap attributes) -> attributes.remove(SEARCH_INDEX_OUTDATED));
  }
}
