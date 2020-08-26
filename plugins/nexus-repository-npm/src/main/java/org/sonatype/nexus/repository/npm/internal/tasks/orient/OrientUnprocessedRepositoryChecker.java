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
package org.sonatype.nexus.repository.npm.internal.tasks.orient;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.ImmutableNestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.npm.internal.tasks.ReindexNpmRepositoryManager.UnprocessedRepositoryChecker;

import static java.lang.Boolean.TRUE;
import static org.sonatype.nexus.repository.npm.internal.tasks.orient.OrientReindexNpmRepositoryTask.NPM_V1_SEARCH_UNSUPPORTED;

/**
 * @since 3.27
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientUnprocessedRepositoryChecker
    implements UnprocessedRepositoryChecker
{
  @Override
  public boolean isUnprocessedNpmRepository(final Repository repository) {
    AttributesFacet attributesFacet = repository.facet(AttributesFacet.class);
    ImmutableNestedAttributesMap attributes = attributesFacet.getAttributes();
    return TRUE.equals(attributes.get(NPM_V1_SEARCH_UNSUPPORTED));
  }
}
