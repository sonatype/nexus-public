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
package org.sonatype.nexus.index;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;

@Named("sha1")
@Singleton
public class Sha1Searcher
    implements Searcher
{
  public static final String TERM_SHA1 = "sha1";

  private final IndexerManager indexerManager;

  @Inject
  public Sha1Searcher(final IndexerManager indexerManager) {
    this.indexerManager = indexerManager;
  }

  public boolean canHandle(Map<String, String> terms) {
    return (terms.containsKey(TERM_SHA1) && !StringUtils.isEmpty(terms.get(TERM_SHA1)));
  }

  public SearchType getDefaultSearchType() {
    return SearchType.EXACT;
  }

  public IteratorSearchResponse flatIteratorSearch(Map<String, String> terms, String repositoryId, Integer from,
                                                   Integer count, Integer hitLimit, boolean uniqueRGA,
                                                   SearchType searchType,
                                                   List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    if (!canHandle(terms)) {
      return IteratorSearchResponse.empty(null);
    }

    return indexerManager.searchArtifactSha1ChecksumIterator(terms.get(TERM_SHA1), repositoryId, from, count,
        hitLimit, filters);
  }
}
