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
package org.sonatype.nexus.repository.search;

import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.elasticsearch.search.SearchHit;

import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.FORMAT;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;

/**
 * @since 3.14
 */
@Singleton
@Named
public class DefaultSearchResultComponentGenerator
    implements SearchResultComponentGenerator
{

  public static final String DEFAULT_SEARCH_RESULT_COMPONENT_GENERATOR_KEY = "default";

  @Override
  public SearchResultComponent from(final SearchHit hit, final Set<String> componentIdSet) {
    SearchResultComponent component = new SearchResultComponent();
    final Map<String, Object> source = hit.getSource();

    component.setId(hit.getId());
    component.setRepositoryName((String) source.get(REPOSITORY_NAME));
    component.setGroup((String) source.get(GROUP));
    component.setName((String) source.get(NAME));
    component.setVersion((String) source.get(VERSION));
    component.setFormat((String) source.get(FORMAT));

    return component;
  }
}
