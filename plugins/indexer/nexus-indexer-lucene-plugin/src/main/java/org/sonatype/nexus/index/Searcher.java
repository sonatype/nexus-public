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

import org.sonatype.nexus.proxy.NoSuchRepositoryException;

import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;

/**
 * A searcher is able to perform artifact info searches based on key/value search terms. Note that this is an
 * intermediate step towards future Nexus pluggable indexing and should not be considered public api.
 *
 * @author Alin Dreghiciu
 */
public interface Searcher
{

  /**
   * Answers the question: can this searcher be used to search for the available terms?
   *
   * @param terms available terms
   * @return true if searcher can be used to search for the available terms, false oterwise
   */
  boolean canHandle(Map<String, String> terms);

  /**
   * Returns the default "search type", that this Searcher wants. Naturally, this is overridable, see
   * flatIteratorSearch() method.
   */
  SearchType getDefaultSearchType();

  /**
   * Searches for artifacts based on available terms.
   *
   * @param terms        search terms
   * @param repositoryId repository id of the repository to be searched ir null if the search should be performed on
   *                     all repositories that suports indexing
   * @param from         offset of first search result
   * @param count        number of search results to be retrieved
   * @return search results
   * @throws NoSuchRepositoryException - If there is no repository with specified repository id
   */
  IteratorSearchResponse flatIteratorSearch(Map<String, String> terms, String repositoryId, Integer from,
                                            Integer count, Integer hitLimit, boolean uniqueRGA, SearchType searchType,
                                            List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException;
}
