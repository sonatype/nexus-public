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
package org.sonatype.nexus.repository.content.search.table;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.ComponentSearchField;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.index.SearchConstants;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substring;

/**
 * Utility class for forming a sort expression based on a search field's column mapping
 *
 * @see org.sonatype.nexus.repository.rest.SearchFieldSupport
 */
public class SqlSearchSortUtil
    extends ComponentSupport
{
  public static final String JSON_PATH_FORMAT = "%s #> '{%s}'";

  public static final String MAVEN_2 = "maven2";

  public static final String DOCKER_LAYER_ANCESTRY = "layerAncestry";

  public static final String DOCKER_CONTENT_DIGEST = "content_digest";

  private final Map<String, SearchFieldSupport> aliasToColumn;

  @Inject
  public SqlSearchSortUtil(final List<SearchMappings> searchMappings) {
    this.aliasToColumn = checkNotNull(searchMappings).stream()
        .map(SearchMappings::get)
        .flatMap(iterable -> stream(iterable.spliterator(), false))
        .collect(Collectors.toMap(mapping -> getAlias(mapping.getAlias()), SearchMapping::getField));
  }

  public Optional<String> getSortExpression(@Nullable final String sortAttribute) {
    Optional<String> sortAlias = Optional.ofNullable(sortAttribute)
        .flatMap(this::isAttributesField);

    Optional<String> sortColumn = sortAlias.map(aliasToColumn::get)
        .map(SearchFieldSupport::getSortColumnName);

    return sortColumn.filter(ComponentSearchField.ATTRIBUTES::equals)
        .map(sortExpressionForAttributesColumn(sortAlias))
        .orElse(sortColumn);
  }

  public Optional<SortDirection> getSortDirection(@Nullable final String sortAttribute) {
    Optional<String> sortAlias = Optional.ofNullable(sortAttribute)
        .flatMap(this::isAttributesField);

    return sortAlias.map(aliasToColumn::get)
        .map(SearchFieldSupport::getSortDirection);
  }

  private Function<String, Optional<String>> sortExpressionForAttributesColumn(final Optional<String> sortAlias) {
    return column ->
        sortAlias.map(alias -> alias.replace('.', ','))
            .map(alias -> String.format(JSON_PATH_FORMAT, column, alias));
  }

  private Optional<String> isAttributesField(final String value) {
    return startsWith(value, "assets.attributes.") ||
        startsWith(value, "attributes.") ? extractSuffix(value) : of(value);
  }

  private Optional<String> extractSuffix(@Nullable final String sortAttribute) {
    if (sortAttribute == null) {
      return empty();
    }

    int prefixIndex = indexOf(sortAttribute, SearchConstants.ATTRIBUTES);
    if (prefixIndex >= 0) {
      int beginIndex = prefixIndex + "attributes.".length();
      return of(substring(sortAttribute, beginIndex));
    }
    return empty();
  }

  protected static String getAlias(final String alias) {
    if (startsWith(alias, "maven.")) {
      return replace(alias, "maven", MAVEN_2);
    }

    if (StringUtils.equals(alias, "docker.layerId")) {
      return replace(alias, "layerId", DOCKER_LAYER_ANCESTRY);
    }

    if (StringUtils.equals(alias, "docker.contentDigest")) {
      return replace(alias, "contentDigest", DOCKER_CONTENT_DIGEST);
    }
    return alias;
  }
}
