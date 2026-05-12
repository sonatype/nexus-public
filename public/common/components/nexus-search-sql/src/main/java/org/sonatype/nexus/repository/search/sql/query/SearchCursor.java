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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keyset-based pagination cursor for efficient search result pagination.
 * 
 * Encodes the last seen record's sort key values (format, componentId) to enable
 * efficient "WHERE (format, component_id) > (lastFormat, lastComponentId)" queries
 * instead of expensive OFFSET-based pagination.
 * 
 * Benefits:
 * - O(log n) seek vs O(n) scan for deep pagination
 * - Consistent results even when data changes between pages
 * - Uses existing indexes on (format, component_id)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchCursor
{
  private static final Logger log = LoggerFactory.getLogger(SearchCursor.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Format of the last seen component (e.g., "maven2", "npm").
   * Used as the primary sort key.
   */
  private final String lastFormat;

  /**
   * Component ID of the last seen component.
   * Used as the secondary sort key (provides uniqueness).
   */
  private final int lastComponentId;

  @JsonCreator
  public SearchCursor(
      @JsonProperty("f") final String lastFormat,
      @JsonProperty("c") final int lastComponentId)
  {
    this.lastFormat = lastFormat;
    this.lastComponentId = lastComponentId;
  }

  @JsonProperty("f")
  public String getLastFormat() {
    return lastFormat;
  }

  @JsonProperty("c")
  public int getLastComponentId() {
    return lastComponentId;
  }

  /**
   * Encodes this cursor to a URL-safe Base64 string for use as a continuation token.
   * 
   * @return Base64-encoded cursor string
   */
  public String encode() {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(this);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
    catch (JsonProcessingException e) {
      log.warn("Failed to encode search cursor", e);
      return null;
    }
  }

  /**
   * Decodes a continuation token into a SearchCursor.
   * Returns empty if the token is null, empty, a legacy offset-based token (numeric),
   * or invalid.
   * 
   * @param token the continuation token to decode
   * @return Optional containing the decoded cursor, or empty if invalid/legacy
   */
  public static Optional<SearchCursor> decode(@Nullable final String token) {
    if (token == null || token.isEmpty()) {
      return Optional.empty();
    }

    // Check for legacy offset-based token (pure numeric string)
    if (token.matches("^\\d+$")) {
      log.debug("Legacy offset-based token detected: {}", token);
      return Optional.empty();
    }

    try {
      byte[] decoded = Base64.getUrlDecoder().decode(token);
      String json = new String(decoded);
      SearchCursor cursor = OBJECT_MAPPER.readValue(json, SearchCursor.class);
      return Optional.of(cursor);
    }
    catch (Exception e) {
      log.debug("Failed to decode search cursor: {}", token, e);
      return Optional.empty();
    }
  }

  /**
   * Creates a cursor from the last result in a search page.
   * 
   * @param format the format of the last component
   * @param componentId the component ID of the last component
   * @return a new SearchCursor
   */
  public static SearchCursor from(final String format, final int componentId) {
    return new SearchCursor(format, componentId);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchCursor that = (SearchCursor) o;
    return lastComponentId == that.lastComponentId && Objects.equals(lastFormat, that.lastFormat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastFormat, lastComponentId);
  }

  @Override
  public String toString() {
    return "SearchCursor{" +
        "lastFormat='" + lastFormat + '\'' +
        ", lastComponentId=" + lastComponentId +
        '}';
  }
}
