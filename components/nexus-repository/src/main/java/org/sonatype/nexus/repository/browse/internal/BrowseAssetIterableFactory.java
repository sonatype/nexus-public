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
package org.sonatype.nexus.repository.browse.internal;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.selector.internal.ContentAuthHelper;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory for creating an {@code asset} browsing {@link Iterable<ODocument>} using the injected
 * {@link ContentAuthHelper} and {@code pageSize}.
 *
 * @since 3.next
 */
@Named
@Singleton
public class BrowseAssetIterableFactory
    extends ComponentSupport
{
  private final ContentAuthHelper contentAuthHelper;

  private final int pageSize;

  @Inject
  public BrowseAssetIterableFactory(final ContentAuthHelper contentAuthHelper,
                                    @Named("${nexus.asset.browse.pageSize:-5000}") final int pageSize)
  {
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
    this.pageSize = pageSize;
  }

  public Iterable<ODocument> create(final ODatabaseDocumentTx db,
                                    @Nullable final String rid,
                                    final String repositoryName,
                                    final List<String> bucketIds,
                                    final int limit)
  {
    return () -> new BrowseAssetIterator(contentAuthHelper, db, rid, repositoryName, bucketIds, limit, pageSize);
  }
}
