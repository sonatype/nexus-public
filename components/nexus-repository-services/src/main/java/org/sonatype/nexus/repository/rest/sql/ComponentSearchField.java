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
package org.sonatype.nexus.repository.rest.sql;

import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.search.SortDirection;

import static org.sonatype.nexus.repository.rest.sql.TextualQueryType.DEFAULT_TEXT_QUERY;
import static org.sonatype.nexus.repository.rest.sql.TextualQueryType.FULL_TEXT_SEARCH_QUERY;

/**
 * A field on the component table.
 *
 * @since 3.38
 */
public class ComponentSearchField
    extends SearchFieldSupport
{
  public static final SearchFieldSupport FORMAT =
      new ComponentSearchField("tsvector_format", "format", FULL_TEXT_SEARCH_QUERY);

  private static final String TABLE = "${format}_component";

  public static final ComponentSearchField NAMESPACE =
      new ComponentSearchField("tsvector_namespace", "namespace", FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport NAME = new ComponentSearchField(
      "tsvector_search_component_name", "tsvector_search_component_name", FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport VERSION =
      new ComponentSearchField("tsvector_version", "normalised_version", SortDirection.DESC, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport KEYWORD = new ComponentSearchField("keywords", FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport PRERELEASE = new ComponentSearchField("prerelease",
      DEFAULT_TEXT_QUERY);

  public static final SearchFieldSupport MD5 = new ComponentSearchField("md5", FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport SHA1 = new ComponentSearchField("sha1", FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport SHA256 = new ComponentSearchField("sha256", FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport SHA512 = new ComponentSearchField("sha512", FULL_TEXT_SEARCH_QUERY);

  public static final String ATTRIBUTES = "attributes";

  public static final SearchFieldSupport FORMAT_FIELD_1 = new ComponentSearchField("format_field_values_1",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport FORMAT_FIELD_2 = new ComponentSearchField("format_field_values_2",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport FORMAT_FIELD_3 = new ComponentSearchField("format_field_values_3",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport FORMAT_FIELD_4 = new ComponentSearchField("format_field_values_4",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport FORMAT_FIELD_5 = new ComponentSearchField("format_field_values_5",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport FORMAT_FIELD_6 = new ComponentSearchField("format_field_values_6",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public static final SearchFieldSupport FORMAT_FIELD_7 = new ComponentSearchField("format_field_values_7",
      ATTRIBUTES, FULL_TEXT_SEARCH_QUERY);

  public ComponentSearchField(final String searchedColumnName) {
    this(searchedColumnName, TextualQueryType.DEFAULT_TEXT_QUERY);
  }

  public ComponentSearchField(final String searchedColumnName, final TextualQueryType columnType) {
    this(searchedColumnName, searchedColumnName, columnType);
  }

  public ComponentSearchField(
      final String searchedColumnName,
      final String sortColumnName,
      final TextualQueryType columnType)
  {
    super(TABLE, searchedColumnName, sortColumnName, SortDirection.ASC, columnType);
  }

  public ComponentSearchField(
      final String searchedColumnName,
      final String sortColumnName,
      final SortDirection sortDirection,
      final TextualQueryType columnType)
  {
    super(TABLE, searchedColumnName, sortColumnName, sortDirection, columnType);
  }
}
