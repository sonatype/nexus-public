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
package org.sonatype.nexus.content.raw.internal.search;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.search.sql.SearchCustomFieldContributor;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.search.sql.SearchRecord;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Populates raw format custom search fields.
 *
 * <p>
 * Raw stores the full asset path as the component name (e.g. {@code /foo/bar/test.txt}). Searches against the existing
 * {@code name} alias therefore only match the full path, which means typing the bare filename (e.g. {@code test.txt})
 * in the UI's "Name" field returns no results. This contributor registers the basename (the portion after the final
 * {@code /}) as an additional component-name alias so the existing {@code name} search matches by filename for the raw
 * format, without introducing a new search criterion.
 */
@Component
@Qualifier(RawFormat.NAME)
public class RawSearchCustomFieldContributor
    implements SearchCustomFieldContributor
{
  @Override
  public void populateSearchCustomFields(final SearchRecord searchRecord, final Asset asset) {
    String basename = extractBasename(asset.path());
    if (basename != null) {
      searchRecord.addAliasComponentName(basename);
    }
  }

  private static String extractBasename(final String path) {
    if (path == null || path.isEmpty()) {
      return null;
    }
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash < 0) {
      return path;
    }
    if (lastSlash == path.length() - 1) {
      return null;
    }
    return path.substring(lastSlash + 1);
  }
}
