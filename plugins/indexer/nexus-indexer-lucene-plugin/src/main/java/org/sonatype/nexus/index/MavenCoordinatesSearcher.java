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
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.UniqueArtifactFilterPostprocessor;

/**
 * Searches Lucene index for artifacts based on maven artifact coordinates.
 *
 * @author Alin Dreghiciu
 */
@Named("mavenCoordinates")
@Singleton
public class MavenCoordinatesSearcher
    implements Searcher
{

  /**
   * The key for group term.
   */
  public static final String TERM_GROUP = "g";

  /**
   * The key for artifact term.
   */
  public static final String TERM_ARTIFACT = "a";

  /**
   * The key for version term.
   */
  public static final String TERM_VERSION = "v";

  /**
   * The key for packaging term.
   */
  public static final String TERM_PACKAGING = "p";

  /**
   * The key for classifier term.
   */
  public static final String TERM_CLASSIFIER = "c";

  private final IndexerManager m_lucene;

  @Inject
  public MavenCoordinatesSearcher(final IndexerManager m_lucene) {
    this.m_lucene = m_lucene;
  }

  /**
   * Map should contain a term with key {@link #TERM_GROUP} or {@link #TERM_ARTIFACT} or {@link #TERM_VERSION} or
   * {@link #TERM_PACKAGING} or {@link #TERM_CLASSIFIER} which has a non null value. {@inheritDoc}
   */
  public boolean canHandle(final Map<String, String> terms) {
    return (terms.containsKey(TERM_GROUP) && !StringUtils.isEmpty(terms.get(TERM_GROUP)))
        || (terms.containsKey(TERM_ARTIFACT) && !StringUtils.isEmpty(terms.get(TERM_ARTIFACT)))
        || (terms.containsKey(TERM_VERSION) && !StringUtils.isEmpty(terms.get(TERM_VERSION)))
        || (terms.containsKey(TERM_PACKAGING) && !StringUtils.isEmpty(terms.get(TERM_PACKAGING)))
        || (terms.containsKey(TERM_CLASSIFIER) && !StringUtils.isEmpty(terms.get(TERM_CLASSIFIER)));
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

    // if the user is querying against these fields, we want to return them properly
    if (filters != null) {
      for (ArtifactInfoFilter filter : filters) {
        if (filter instanceof UniqueArtifactFilterPostprocessor) {
          UniqueArtifactFilterPostprocessor uFilter = (UniqueArtifactFilterPostprocessor) filter;

          if (terms.containsKey(MavenCoordinatesSearcher.TERM_VERSION)) {
            uFilter.addField(MAVEN.VERSION);
          }
          if (terms.containsKey(MavenCoordinatesSearcher.TERM_PACKAGING)) {
            uFilter.addField(MAVEN.PACKAGING);
          }
          if (terms.containsKey(MavenCoordinatesSearcher.TERM_CLASSIFIER)) {
            uFilter.addField(MAVEN.CLASSIFIER);
          }

          // in GAV search, we _always_ expand repository
          uFilter.addField(MAVEN.REPOSITORY_ID);

          break;
        }
      }
    }

    return m_lucene.searchArtifactIterator(terms.get(TERM_GROUP), terms.get(TERM_ARTIFACT),
        terms.get(TERM_VERSION), terms.get(TERM_PACKAGING), terms.get(TERM_CLASSIFIER), repositoryId, from,
        count, hitLimit, false, searchType, filters);
  }

}