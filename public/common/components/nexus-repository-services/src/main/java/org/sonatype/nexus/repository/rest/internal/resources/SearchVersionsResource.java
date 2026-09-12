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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;

import org.sonatype.nexus.repository.rest.api.ComponentVersionXO;
import org.sonatype.nexus.repository.rest.api.ComponentVersionsPageXO;
import org.sonatype.nexus.repository.rest.api.SearchVersionsResourceDoc;
import org.sonatype.nexus.repository.search.ComponentVersion;
import org.sonatype.nexus.repository.search.ComponentVersionPage;
import org.sonatype.nexus.repository.search.ComponentVersionQuery;
import org.sonatype.nexus.repository.search.ComponentVersionSearch;
import org.sonatype.nexus.repository.search.ComponentVersionSortKeys;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * Browses the distinct versions of a single component, one page at a time.
 *
 * <p>
 * Unlike {@code /v1/search}, this endpoint returns one row per version rather than one row per
 * component/repository pair, and pages with explicit {@code page}/{@code size} parameters plus a
 * {@code total} rather than a continuation token.
 */
@Component
@Path(SearchVersionsResource.RESOURCE_URI)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SearchVersionsResource
    implements Resource, SearchVersionsResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/search/versions";

  static final int MAX_SIZE = 250;

  /**
   * Typed as a String, unlike {@link #MAX_SIZE}, purely so {@code @DefaultValue} can take it:
   * the annotation accepts only a constant String expression. Its one consumer is the
   * {@code size} parameter below — declaring it and leaving a bare "20" in the annotation is
   * what invites the two to drift.
   */
  static final String DEFAULT_SIZE = "20";

  private final ComponentVersionSearch componentVersionSearch;

  public SearchVersionsResource(final ComponentVersionSearch componentVersionSearch) {
    this.componentVersionSearch = checkNotNull(componentVersionSearch);
  }

  @RequiresUser
  @GET
  @Override
  public ComponentVersionsPageXO getVersions(
      @QueryParam("format") final String format,
      @QueryParam("group") final String group,
      @QueryParam("name") final String name,
      @QueryParam("version") final String version,
      @QueryParam("page") @DefaultValue("0") final int page,
      @QueryParam("size") @DefaultValue(DEFAULT_SIZE) final int size,
      @QueryParam("sort") @DefaultValue("version") final String sort,
      @QueryParam("direction") @DefaultValue("desc") final String direction)
  {
    if (isBlank(format)) {
      throw new WebApplicationMessageException(BAD_REQUEST, "format is required");
    }
    if (isBlank(name)) {
      throw new WebApplicationMessageException(BAD_REQUEST, "name is required");
    }
    if (page < 0) {
      throw new WebApplicationMessageException(BAD_REQUEST, "page must not be negative");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new WebApplicationMessageException(BAD_REQUEST, "size must be between 1 and " + MAX_SIZE);
    }
    // Checked in long arithmetic, and before the query is built: downstream the offset is
    // page * size in a 32-bit int (SqlComponentVersionSearch), so an unbounded page silently
    // overflows to a negative OFFSET that PostgreSQL rejects with a raw SQL error. The bound is
    // expressed as the largest representable offset rather than a fixed page cap so it does not
    // depend on, or drift from, the value of MAX_SIZE.
    if ((long) page * size > Integer.MAX_VALUE) {
      throw new WebApplicationMessageException(BAD_REQUEST,
          "page is too large for the requested size: page * size must not exceed " + Integer.MAX_VALUE);
    }
    if (!ComponentVersionSortKeys.ALL.contains(sort)) {
      throw new WebApplicationMessageException(BAD_REQUEST, "sort must be one of " + ComponentVersionSortKeys.ALL);
    }

    ComponentVersionPage result = componentVersionSearch.browseVersions(new ComponentVersionQuery(
        format, group, name, version, page, size, sort, parseDirection(direction)));

    return toXO(result);
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }

  private static SortDirection parseDirection(final String direction) {
    if ("asc".equalsIgnoreCase(direction)) {
      return SortDirection.ASC;
    }
    if ("desc".equalsIgnoreCase(direction)) {
      return SortDirection.DESC;
    }
    throw new WebApplicationMessageException(BAD_REQUEST, "direction must be asc or desc");
  }

  private static ComponentVersionsPageXO toXO(final ComponentVersionPage page) {
    List<ComponentVersionXO> items = page.items().stream().map(SearchVersionsResource::toXO).toList();
    ComponentVersionsPageXO xo = new ComponentVersionsPageXO();
    xo.setItems(items);
    xo.setTotal(page.total());
    xo.setPage(page.page());
    xo.setSize(page.size());
    return xo;
  }

  private static ComponentVersionXO toXO(final ComponentVersion version) {
    ComponentVersionXO xo = new ComponentVersionXO();
    xo.setVersion(version.version());
    xo.setLastUpdated(version.lastUpdated());
    xo.setRepositories(version.repositories());
    return xo;
  }
}
