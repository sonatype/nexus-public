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
package org.sonatype.nexus.repository.search.sql.store;

import org.sonatype.nexus.repository.search.sql.query.SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchDB;

/**
 * H2 runner for the component-versions queries, alongside
 * {@code PostgresComponentVersionsSearchTableDAOTest}. H2 is the default store for self-hosted
 * deployments, and these queries use constructs whose portability is not self-evident —
 * STRING_AGG with an ordered DISTINCT aggregate, LIKE ... ESCAPE, and COUNT(DISTINCT ...) — so
 * both engines need to execute them. Mirrors the {@link SearchTableDAOTest} /
 * {@code PostgresSearchTableDAOTest} pairing.
 */
public class ComponentVersionsSearchTableDAOTest
    extends ComponentVersionsSearchTableDAOTestSupport
{
  @Override
  protected SearchConditionFactory createSearchConditionFactory() {
    return new H2SearchConditionFactory(new H2SearchDB());
  }
}
