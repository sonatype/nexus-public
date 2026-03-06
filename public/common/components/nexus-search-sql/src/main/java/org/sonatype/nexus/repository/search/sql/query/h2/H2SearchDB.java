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
package org.sonatype.nexus.repository.search.sql.query.h2;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.query.SearchDatabase;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchColumn.Type;

import jakarta.inject.Singleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.rest.sql.SearchField.*;

/**
 * An H2 implementation of {@link SearchDatabase}
 *
 * H2 doesn't support PostgreSQL's TSVECTOR columns, so all searches use simple VARCHAR columns with LIKE queries.
 * Instantiated by SearchDatabaseFactory when H2 is detected at runtime.
 */
@Lazy
@Qualifier("h2")
@Component
@Singleton
public class H2SearchDB
    implements SearchDatabase
{
  private final Map<SearchField, H2SearchColumn> columns;

  public H2SearchDB() {
    this.columns = Collections.unmodifiableMap(columns());
  }

  public Optional<H2SearchColumn> getColumn(final SearchField field) {
    return Optional.ofNullable(columns.get(field));
  }

  @Override
  public Optional<String> getSortColumn(final SearchField field) {
    return Optional.ofNullable(field)
        .map(columns::get)
        .flatMap(H2SearchColumn::sortColumnName);
  }

  @Override
  public String formatJsonPath(final String columnName, final String sortAlias) {
    // H2 doesn't support JSON functions or operators like (JSON_VALUE, #>) it may be added in future H2 releases
    return null;
  }

  private static Map<SearchField, H2SearchColumn> columns() {
    EnumMap<SearchField, H2SearchColumn> columns = new EnumMap<>(SearchField.class);

    // Component table columns (with cs. prefix)
    columns.put(FORMAT, new H2SearchColumn(addComponentPrefix("format")));
    columns.put(REPOSITORY_NAME, new H2SearchColumn(addComponentPrefix("search_repository_name")));
    columns.put(COMPONENT_ID, new H2SearchColumn(addComponentPrefix("component_id")));
    columns.put(COMPONENT_KIND, new H2SearchColumn(addComponentPrefix("component_kind")));
    columns.put(LAST_MODIFIED, new H2SearchColumn(addComponentPrefix("last_modified")));
    columns.put(NAMESPACE, new H2SearchColumn(addComponentPrefix("namespace")));
    columns.put(NAME, new H2SearchColumn(addComponentPrefix("search_component_name")));
    columns.put(VERSION, new H2SearchColumn(addComponentPrefix("version"), addComponentPrefix("normalised_version")));
    columns.put(PRERELEASE, new H2SearchColumn(addComponentPrefix("prerelease"), Type.BOOLEAN));

    // Text search columns - H2 uses VARCHAR instead of TSVECTOR
    // Only mark columns as tokenized where we're certain they always contain tokenized data
    columns.put(KEYWORDS,
        new H2SearchColumn(addComponentPrefix("keywords"), addComponentPrefix("search_component_name"), true));
    // FORMAT_FIELD columns can't use attributes JSON column for sorting because H2 doesn't support JSON functions or
    // operators
    columns.put(FORMAT_FIELD_1, new H2SearchColumn(addComponentPrefix("format_field_values_1"), true));
    columns.put(FORMAT_FIELD_2, new H2SearchColumn(addComponentPrefix("format_field_values_2"), true));
    columns.put(FORMAT_FIELD_3, new H2SearchColumn(addComponentPrefix("format_field_values_3"), true));
    columns.put(FORMAT_FIELD_4, new H2SearchColumn(addComponentPrefix("format_field_values_4"), true));
    columns.put(FORMAT_FIELD_5, new H2SearchColumn(addComponentPrefix("format_field_values_5"), true));
    columns.put(FORMAT_FIELD_6, new H2SearchColumn(addComponentPrefix("format_field_values_6"), true));
    columns.put(FORMAT_FIELD_7, new H2SearchColumn(addComponentPrefix("format_field_values_7"), true));

    columns.put(MD5, new H2SearchColumn(addComponentPrefix("md5"), true));
    columns.put(SHA1, new H2SearchColumn(addComponentPrefix("sha1"), true));
    columns.put(SHA256, new H2SearchColumn(addComponentPrefix("sha256"), true));
    columns.put(SHA512, new H2SearchColumn(addComponentPrefix("sha512"), true));

    columns.put(TAGS, new H2SearchColumn(addComponentPrefix("tags"), true, Type.JSON));
    columns.put(PATHS, new H2SearchColumn(addComponentPrefix("paths"), true, Type.JSON));
    columns.put(UPLOADERS, new H2SearchColumn(addComponentPrefix("uploaders"), true));
    columns.put(UPLOADER_IPS, new H2SearchColumn(addComponentPrefix("uploader_ips"), true));

    // Asset table columns (with ap. prefix)
    columns.put(ASSET_FORMAT_VALUE_1, new H2SearchColumn(addAssetPrefix("asset_format_value_1")));
    columns.put(ASSET_FORMAT_VALUE_2, new H2SearchColumn(addAssetPrefix("asset_format_value_2")));
    columns.put(ASSET_FORMAT_VALUE_3, new H2SearchColumn(addAssetPrefix("asset_format_value_3")));
    columns.put(ASSET_FORMAT_VALUE_4, new H2SearchColumn(addAssetPrefix("asset_format_value_4")));
    columns.put(ASSET_FORMAT_VALUE_5, new H2SearchColumn(addAssetPrefix("asset_format_value_5")));
    columns.put(ASSET_FORMAT_VALUE_6, new H2SearchColumn(addAssetPrefix("asset_format_value_6")));
    columns.put(ASSET_FORMAT_VALUE_7, new H2SearchColumn(addAssetPrefix("asset_format_value_7")));
    columns.put(ASSET_FORMAT_VALUE_8, new H2SearchColumn(addAssetPrefix("asset_format_value_8")));
    columns.put(ASSET_FORMAT_VALUE_9, new H2SearchColumn(addAssetPrefix("asset_format_value_9")));
    columns.put(ASSET_FORMAT_VALUE_10, new H2SearchColumn(addAssetPrefix("asset_format_value_10")));
    columns.put(ASSET_FORMAT_VALUE_11, new H2SearchColumn(addAssetPrefix("asset_format_value_11")));
    columns.put(ASSET_FORMAT_VALUE_12, new H2SearchColumn(addAssetPrefix("asset_format_value_12")));
    columns.put(ASSET_FORMAT_VALUE_13, new H2SearchColumn(addAssetPrefix("asset_format_value_13")));
    columns.put(ASSET_FORMAT_VALUE_14, new H2SearchColumn(addAssetPrefix("asset_format_value_14")));
    columns.put(ASSET_FORMAT_VALUE_15, new H2SearchColumn(addAssetPrefix("asset_format_value_15")));
    columns.put(ASSET_FORMAT_VALUE_16, new H2SearchColumn(addAssetPrefix("asset_format_value_16")));
    columns.put(ASSET_FORMAT_VALUE_17, new H2SearchColumn(addAssetPrefix("asset_format_value_17")));
    columns.put(ASSET_FORMAT_VALUE_18, new H2SearchColumn(addAssetPrefix("asset_format_value_18")));
    columns.put(ASSET_FORMAT_VALUE_19, new H2SearchColumn(addAssetPrefix("asset_format_value_19")));
    columns.put(ASSET_FORMAT_VALUE_20, new H2SearchColumn(addAssetPrefix("asset_format_value_20")));

    return columns;
  }

  private static String addComponentPrefix(final String columnName) {
    return "cs." + columnName;
  }

  private static String addAssetPrefix(final String columnName) {
    return "ap." + columnName;
  }
}
