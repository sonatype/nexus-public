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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFilter;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.selector.DatastoreContentAuthHelper;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorManager;

/**
 * Test datastore browse node store implementation.
 *
 * @since 3.24
 */
@Named
public class TestDatastoreBrowseNodeStoreImpl
    extends DatastoreBrowseNodeStoreImpl<TestBrowseNodeDAO>
{
  @Inject
  public TestDatastoreBrowseNodeStoreImpl(
      final DataSessionSupplier sessionSupplier,
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final BrowseNodeConfiguration configuration,
      final RepositoryManager repositoryManager,
      final DatastoreContentAuthHelper contentAuthHelper,
      final Map<String, ContentRepositoryStore<? extends ContentRepositoryDAO>> contentRepositoryStores,
      final Map<String, BrowseNodeFilter> browseNodeFilters,
      final Map<String, BrowseNodeComparator> browseNodeComparators)
  {
    super(sessionSupplier, securityHelper, selectorManager, configuration, repositoryManager, contentAuthHelper, contentRepositoryStores,
        browseNodeFilters, browseNodeComparators);
  }
}
