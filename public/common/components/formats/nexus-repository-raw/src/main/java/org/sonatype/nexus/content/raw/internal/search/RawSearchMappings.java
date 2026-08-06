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

import java.util.List;

import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.rest.sql.SearchField.NAME;

/**
 * Raw format search mappings.
 *
 * <p>
 * Registers an internal {@code raw.name} alias that performs a lenient (non-exact) search against the component name
 * field. This alias is <strong>not</strong> exposed in the UI; the user-facing criterion remains {@code name.raw}
 * across all UIs. {@link NameRawSqlSearchQueryContribution} rewrites {@code name.raw} filters to this {@code raw.name}
 * routing target when the request is scoped to {@code format=raw}.
 *
 * <p>
 * Raw stores the full asset path as the component name (e.g. {@code /foo/bar/test.txt}), but
 * {@link RawSearchCustomFieldContributor} also registers the basename as an alias in the fulltext index.
 * Using {@code exactMatch=false} causes the search to go through the fulltext (tsvector) path, where the
 * basename alias is available, so that searching by filename (e.g. {@code oci-image-spec.pdf}) works correctly.
 */
@Component
@Qualifier(RawFormat.NAME)
public class RawSearchMappings
    implements SearchMappings
{
  public static final String RAW_NAME = "raw.name";

  /**
   * The attribute path registered with {@link org.sonatype.nexus.repository.rest.SearchMapping} for the
   * {@link #RAW_NAME} alias. This is a routing key used by {@code SearchMappingService} to dispatch REST search
   * parameters — no actual {@code raw.name} attribute is stored on component/asset records during ingestion.
   * Basename matching works via the fulltext alias registered by {@link RawSearchCustomFieldContributor}, not
   * through a stored attribute value.
   */
  public static final String RAW_NAME_ATTRIBUTE = "attributes.raw.name";

  private static final List<SearchMapping> MAPPINGS = ImmutableList.of(
      new SearchMapping(RAW_NAME, RAW_NAME_ATTRIBUTE, "Raw asset filename (basename)", NAME, false));

  @Override
  public Iterable<SearchMapping> get() {
    return MAPPINGS;
  }
}
