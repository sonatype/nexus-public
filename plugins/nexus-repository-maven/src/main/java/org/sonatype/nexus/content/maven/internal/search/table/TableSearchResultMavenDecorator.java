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
package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.table.TableSearchResultDecorator;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.ComponentSearchResult;

import static java.util.Collections.emptyMap;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;

/**
 * An {@link TableSearchResultDecorator} which annotates {@link ComponentSearchResult} with the maven baseVersion.
 */
@Singleton
@Named(Maven2Format.NAME)
public class TableSearchResultMavenDecorator
    implements TableSearchResultDecorator
{
  @Override
  public void updateComponent(final ComponentSearchResult component, final SearchResult searchResult) {
    if (Maven2Format.NAME.equals(component.getFormat())) {
      Object formatAttributes = searchResult.attributes().get(Maven2Format.NAME);

      @SuppressWarnings("unchecked")
      Map<String, String> attributes =
          formatAttributes instanceof Map ? (Map<String, String>) formatAttributes : emptyMap();

      component.addAnnotation(P_BASE_VERSION, attributes.get(P_BASE_VERSION));
    }
  }
}
