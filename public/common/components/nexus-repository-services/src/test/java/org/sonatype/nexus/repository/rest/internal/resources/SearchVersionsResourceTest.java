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

import java.time.OffsetDateTime;
import java.util.List;

import org.sonatype.nexus.repository.rest.api.ComponentVersionsPageXO;
import org.sonatype.nexus.repository.search.ComponentVersion;
import org.sonatype.nexus.repository.search.ComponentVersionPage;
import org.sonatype.nexus.repository.search.ComponentVersionQuery;
import org.sonatype.nexus.repository.search.ComponentVersionSearch;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Note on the missing 401 case: every test here runs {@code @WithUser(isAuthenticated = false)}
 * and still reaches the method body, because {@code @RequiresUser} on
 * {@link SearchVersionsResource#getVersions} is enforced by a Shiro AOP interceptor that only
 * exists once a {@code SecurityManager} is wired into the request path. Calling the resource
 * directly, as these tests do, bypasses it. No unit test can therefore cover the 401 path, and
 * none would fail if the annotation were deleted — that guarantee lives in the interceptor's own
 * tests and in the integration suite.
 */
@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
class SearchVersionsResourceTest
{
  @Mock
  private ComponentVersionSearch componentVersionSearch;

  private SearchVersionsResource underTest;

  @BeforeEach
  void setUp() {
    underTest = new SearchVersionsResource(componentVersionSearch);
  }

  @WithUser(isAuthenticated = false)
  @Test
  void returnsThePageFromTheService() {
    when(componentVersionSearch.browseVersions(any())).thenReturn(new ComponentVersionPage(
        List.of(new ComponentVersion("1.0.10", OffsetDateTime.parse("2026-02-01T00:00:00Z"),
            List.of("releases", "snapshots"))),
        4213L, 0, 20));

    ComponentVersionsPageXO result =
        underTest.getVersions("maven2", "org.test", "artifact", null, 0, 20, "version", "desc");

    assertThat(result.getTotal()).isEqualTo(4213L);
    assertThat(result.getPage()).isZero();
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getVersion()).isEqualTo("1.0.10");
    assertThat(result.getItems().get(0).getRepositories()).containsExactly("releases", "snapshots");
  }

  @WithUser(isAuthenticated = false)
  @Test
  void acceptsSizeAtTheCap() {
    when(componentVersionSearch.browseVersions(any()))
        .thenReturn(new ComponentVersionPage(List.of(), 0L, 0, 250));

    ComponentVersionsPageXO result =
        underTest.getVersions("maven2", "org.test", "artifact", null, 0, 250, "version", "desc");

    assertThat(result.getSize()).isEqualTo(250);
  }

  @WithUser(isAuthenticated = false)
  @Test
  void rejectsASizeAboveTheCap() {
    assertThatThrownBy(
        () -> underTest.getVersions("maven2", "org.test", "artifact", null, 0, 251, "version", "desc"))
            .isInstanceOf(WebApplicationMessageException.class)
            .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  /**
   * The lower bound matters as much as the cap: {@code size=0} would otherwise reach the DAO as
   * {@code LIMIT 0}, which returns an empty page that looks like "no versions" rather than a bad
   * request.
   */
  @WithUser(isAuthenticated = false)
  @Test
  void rejectsASizeBelowOne() {
    assertThatThrownBy(
        () -> underTest.getVersions("maven2", "org.test", "artifact", null, 0, 0, "version", "desc"))
            .isInstanceOf(WebApplicationMessageException.class)
            .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void rejectsAnUnknownSortKey() {
    assertThatThrownBy(
        () -> underTest.getVersions("maven2", "org.test", "artifact", null, 0, 20, "status", "desc"))
            .isInstanceOf(WebApplicationMessageException.class)
            .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void rejectsANegativePage() {
    assertThatThrownBy(
        () -> underTest.getVersions("maven2", "org.test", "artifact", null, -1, 20, "version", "desc"))
            .isInstanceOf(WebApplicationMessageException.class)
            .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  /**
   * A page that is individually valid but whose {@code page * size} exceeds {@link Integer#MAX_VALUE}
   * must be refused here. Downstream the offset is computed in 32-bit arithmetic, so letting this
   * through produces a negative OFFSET and a raw SQL error from PostgreSQL instead of a 400 —
   * hence the assertion that the search service is never reached.
   */
  @WithUser(isAuthenticated = false)
  @Test
  void rejectsAPageThatWouldOverflowTheOffset() {
    assertThatThrownBy(
        () -> underTest.getVersions("maven2", "org.test", "artifact", null, 10_000_000, 250, "version", "desc"))
            .isInstanceOf(WebApplicationMessageException.class)
            .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));

    verifyNoInteractions(componentVersionSearch);
  }

  /**
   * The bound is the largest representable offset, not an arbitrary page cap, so the last page
   * that still fits must be accepted.
   */
  @WithUser(isAuthenticated = false)
  @Test
  void acceptsTheLargestPageThatFitsTheOffset() {
    int page = Integer.MAX_VALUE / 250;
    when(componentVersionSearch.browseVersions(any()))
        .thenReturn(new ComponentVersionPage(List.of(), 0L, page, 250));

    ComponentVersionsPageXO result =
        underTest.getVersions("maven2", "org.test", "artifact", null, page, 250, "version", "desc");

    assertThat(result.getPage()).isEqualTo(page);
  }

  @WithUser(isAuthenticated = false)
  @Test
  void rejectsAMissingFormat() {
    assertThatThrownBy(() -> underTest.getVersions(" ", "org.test", "artifact", null, 0, 20, "version", "desc"))
        .isInstanceOf(WebApplicationMessageException.class)
        .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void rejectsAMissingName() {
    assertThatThrownBy(() -> underTest.getVersions("maven2", "org.test", " ", null, 0, 20, "version", "desc"))
        .isInstanceOf(WebApplicationMessageException.class)
        .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void rejectsAnInvalidDirection() {
    assertThatThrownBy(
        () -> underTest.getVersions("maven2", "org.test", "artifact", null, 0, 20, "version", "invalid"))
            .isInstanceOf(WebApplicationMessageException.class)
            .satisfies(e -> assertThat(((WebApplicationMessageException) e).getResponse().getStatus()).isEqualTo(400));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void passesAllQueryParametersThrough() {
    when(componentVersionSearch.browseVersions(any()))
        .thenReturn(new ComponentVersionPage(List.of(), 0L, 2, 50));

    underTest.getVersions("npm", null, "lodash", "4.17", 2, 50, "lastUpdated", "asc");

    ArgumentCaptor<ComponentVersionQuery> captor = ArgumentCaptor.forClass(ComponentVersionQuery.class);
    verify(componentVersionSearch).browseVersions(captor.capture());
    ComponentVersionQuery query = captor.getValue();
    assertThat(query.format()).isEqualTo("npm");
    assertThat(query.namespace()).isNull();
    assertThat(query.name()).isEqualTo("lodash");
    assertThat(query.versionFilter()).isEqualTo("4.17");
    assertThat(query.page()).isEqualTo(2);
    assertThat(query.size()).isEqualTo(50);
    assertThat(query.sort()).isEqualTo("lastUpdated");
    assertThat(query.direction().name()).isEqualTo("ASC");
  }
}
