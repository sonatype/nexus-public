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
package org.sonatype.nexus.repository.search.sql.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.normalize.VersionNumberExpander;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.query.SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.testdb.DatabaseTest;

import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAME;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAMESPACE;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.EQ;

/**
 * Tests for the distinct-version browsing queries ({@code browseComponentVersions} /
 * {@code countComponentVersions}) on {@link SearchTableDAO}, split out of
 * {@link SearchTableDAOTestSupport} into its own test class (NEXUS-54219) so it gets its own
 * Postgres testcontainer rather than sharing {@code PostgresSearchTableDAOTest}'s connection
 * budget. That class's {@code max_connections=110} Postgres container is shared by every
 * {@code @DatabaseTest} method declared in it; adding these tests there pushed the class's
 * cumulative connection churn over that ceiling before the run finished, failing whichever
 * tests (old or new) happened to run afterward.
 */
public abstract class ComponentVersionsSearchTableDAOTestSupport
    extends ExampleContentTestSupport
{
  private ContentRepositoryData repository;

  protected SearchConditionFactory conditionBuilder;

  private static int componentIdCounter = 1000;

  @BeforeEach
  public void setupContent() {
    sessionRule.register(SearchTableDAO.class);

    conditionBuilder = createSearchConditionFactory();

    generateNamespaces(5);
    generateNames(5);
    generateVersions(10);
    generatePaths(10);
    generateConfiguration();

    ConfigurationData configuration = generatedConfigurations().get(0);
    generateSingleRepository(UUID.fromString(configuration.getRepositoryId().getValue()));
    repository = generatedRepositories().get(0);
  }

  @DatabaseTest(postgresql = true)
  void browseComponentVersionsGroupsByVersionAndAggregatesRepositories() {
    // one component, version 1.0.0 in two repositories, plus 1.0.9 and 1.0.10
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "releases");
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "snapshots");
    insertSearchRow("test", "org.test", "artifact", "1.0.9", "releases");
    insertSearchRow("test", "org.test", "artifact", "1.0.10", "releases");

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.format = #{filterParams.format} AND cs.namespace = #{filterParams.ns} "
            + "AND cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("format", "test", "ns", "org.test", "name", "artifact"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC")
        .limit(10)
        .offset(0)
        .build();

    List<ComponentVersionData> versions;
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      versions = session.access(SearchTableDAO.class).browseComponentVersions(request);
    }

    assertThat(versions.stream().map(ComponentVersionData::getVersion).toList(),
        contains("1.0.10", "1.0.9", "1.0.0"));

    ComponentVersionData oneZeroZero = versions.get(2);
    assertThat(Arrays.asList(oneZeroZero.getRepositories().split(",")),
        containsInAnyOrder("releases", "snapshots"));
  }

  /**
   * Pins the repository order the aggregate's own ORDER BY asks for, with the rows inserted in
   * reverse so the expectation is not the insertion order.
   * <p>
   * This does not prove the ORDER BY is load-bearing, and cannot: dropping it still passes here,
   * because PostgreSQL is merely unconstrained in what order it returns — for an input this small
   * it happens to come back sorted. The guarantee is by construction in the SQL. What this test
   * does catch is the order being changed to something other than ascending by name, which is
   * what the CSV export and the repository badge are written against.
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsReturnsAggregatedRepositoriesSortedByName() {
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "zulu");
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "mike");
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "alpha");

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.format = #{filterParams.format} AND cs.namespace = #{filterParams.ns} "
            + "AND cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("format", "test", "ns", "org.test", "name", "artifact"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC")
        .limit(10)
        .offset(0)
        .build();

    List<ComponentVersionData> versions;
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      versions = session.access(SearchTableDAO.class).browseComponentVersions(request);
    }

    assertThat(versions, hasSize(1));
    assertThat(versions.get(0).getRepositories(), is("alpha,mike,zulu"));
  }

  /**
   * AT-014: the version filter is a substring match evaluated server-side, so typing "10"
   * must find 10.0.0 and 100.0.0 as well as 1.0.10 — including matches that live on pages the
   * client has not loaded. An exact-match filter silently returns nothing here.
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsFiltersVersionBySubstring() {
    insertSearchRow("test", "org.test", "artifact", "1.0.9", "releases");
    insertSearchRow("test", "org.test", "artifact", "1.0.10", "releases");
    insertSearchRow("test", "org.test", "artifact", "10.0.0", "releases");
    insertSearchRow("test", "org.test", "artifact", "100.0.0", "releases");
    insertSearchRow("test", "org.test", "artifact", "2.0.0", "releases");

    ComponentVersionSearchRequest request = componentVersionRequestBuilder()
        .versionLikePattern("%10%")
        .build();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchTableDAO dao = session.access(SearchTableDAO.class);

      assertThat(dao.browseComponentVersions(request).stream().map(ComponentVersionData::getVersion).toList(),
          contains("100.0.0", "10.0.0", "1.0.10"));
      // the count query must apply the same predicate, or paging totals contradict the rows
      assertThat(dao.countComponentVersions(request), is(3L));
    }
  }

  /**
   * A literal % or _ typed into the filter box must be matched literally rather than acting as
   * a wildcard, otherwise typing "%" appears to match every version.
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsEscapesWildcardsInVersionFilter() {
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "releases");
    insertSearchRow("test", "org.test", "artifact", "1%0", "releases");
    insertSearchRow("test", "org.test", "artifact", "1_0", "releases");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchTableDAO dao = session.access(SearchTableDAO.class);

      // "%" escaped to "\%" must match only the version literally containing a percent sign
      ComponentVersionSearchRequest percent = componentVersionRequestBuilder()
          .versionLikePattern("%1\\%0%")
          .build();
      assertThat(dao.browseComponentVersions(percent).stream().map(ComponentVersionData::getVersion).toList(),
          contains("1%0"));

      // "_" escaped to "\_" must not behave as the single-character wildcard
      ComponentVersionSearchRequest underscore = componentVersionRequestBuilder()
          .versionLikePattern("%1\\_0%")
          .build();
      assertThat(dao.browseComponentVersions(underscore).stream().map(ComponentVersionData::getVersion).toList(),
          contains("1_0"));
    }
  }

  /**
   * Guards the assumption that {@code idx_search_components_format_ns_name_version} depends on:
   * that format / namespace / name resolve to plain-column equality rather than a tsvector match.
   *
   * <p>
   * These three are declared in {@code DefaultSearchMappings} with the four-argument
   * {@code SearchMapping} constructor, which defaults {@code exactMatch} to true, producing
   * {@link ExactTerm} and therefore the non-tsvector columns. If any of them is ever switched to
   * lenient matching the predicate becomes {@code tsvector_* @@ to_tsquery(...)}, the composite
   * index silently stops being usable, and the paged version browse regresses to a bitmap scan
   * over every row of the component. That failure is invisible without this assertion.
   */
  @DatabaseTest(postgresql = true)
  void componentVersionFilterUsesPlainColumnsNotTsvector() {
    SqlSearchQueryCondition condition = conditionBuilder.build(new ExpressionGroup(
        SqlClause.create(Operand.AND,
            new SqlPredicate(EQ, SearchField.FORMAT, new ExactTerm("test")),
            new SqlPredicate(EQ, NAMESPACE, new ExactTerm("org.test")),
            new SqlPredicate(EQ, NAME, new ExactTerm("artifact"))),
        null)).getComponentCondition();

    String sql = condition.getSqlFilter().toLowerCase(Locale.ROOT);

    assertThat("format/namespace/name must compare plain columns for the composite index to apply",
        sql, not(containsString("tsvector")));
    assertThat(sql, not(containsString("to_tsquery")));
    assertThat(sql, containsString("format"));
    assertThat(sql, containsString("namespace"));
    assertThat(sql, containsString("search_component_name"));
  }

  private ComponentVersionSearchRequest.Builder componentVersionRequestBuilder() {
    return ComponentVersionSearchRequest.builder()
        .filter("cs.format = #{filterParams.format} AND cs.namespace = #{filterParams.ns} "
            + "AND cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("format", "test", "ns", "org.test", "name", "artifact"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC")
        .limit(10)
        .offset(0);
  }

  @DatabaseTest(postgresql = true)
  void countComponentVersionsCountsDistinctVersionsNotRepositoryRows() {
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "releases");
    insertSearchRow("test", "org.test", "artifact", "1.0.0", "snapshots");
    insertSearchRow("test", "org.test", "artifact", "1.0.1", "releases");

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.format = #{filterParams.format}")
        .filterParams(Map.of("format", "test"))
        .build();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      // 3 component/repository rows, but only 2 distinct versions
      assertThat(session.access(SearchTableDAO.class).countComponentVersions(request), is(2L));
    }
  }

  /**
   * Tests ordering of qualifier and prerelease versions using the fallback normaliser
   * ({@link VersionNumberExpander}), which applies ONLY to formats that have no registered
   * {@code VersionNormalizer} bean.
   *
   * <p>
   * <strong>NOT maven2 or npm:</strong> Maven2 uses {@code MavenVersionNormalizer}, which
   * encodes releases (e.g., {@code 1.0.0}) differently from prereleases (e.g., {@code 1.0.0-beta}).
   * Under Maven's encoding, releases sort ABOVE their prereleases, producing the order:
   * {@code 1.0.10, 1.0.9, 1.0.0, 1.0.0-rc1, 1.0.0-beta}.
   *
   * <p>
   * This test validates the fallback path only, where the expander places prereleases above
   * their release counterpart: {@code 1.0.10, 1.0.9, 1.0.0-rc1, 1.0.0-beta, 1.0.0}.
   * Do NOT interpret this as Maven ordering.
   *
   * @see #browseComponentVersionsOrdersMaven2ReleasesAboveTheirPrereleases for Maven-specific ordering
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsOrdersByNormalisedVersionForFormatsWithoutARegisteredNormalizer() {
    for (String version : List.of("1.0.0", "1.0.0-beta", "1.0.0-rc1", "1.0.9", "1.0.10")) {
      insertSearchRow("test", "org.test", "ordering", version, "releases");
    }

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("name", "ordering"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC")
        .limit(10)
        .offset(0)
        .build();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      assertThat(
          session.access(SearchTableDAO.class)
              .browseComponentVersions(request)
              .stream()
              .map(ComponentVersionData::getVersion)
              .toList(),
          contains("1.0.10", "1.0.9", "1.0.0-rc1", "1.0.0-beta", "1.0.0"));
    }
  }

  /**
   * Tests Maven2-specific version ordering, where releases sort ABOVE their prereleases.
   *
   * <p>
   * The {@code normalised_version} values below were captured from a real Maven2 component
   * in PostgreSQL. Maven2's {@code MavenVersionNormalizer} encodes:
   * <ul>
   * <li>Releases with {@code .c.~} suffix (e.g., {@code 2.0.10} → {@code 000000002.000000000.000000010.c.~})</li>
   * <li>Prereleases with {@code .a.<qualifier>} suffix (e.g., {@code 2.0.0-beta} →
   * {@code 000000002.000000000.000000000.a.beta})</li>
   * </ul>
   *
   * <p>
   * Since {@code 'c' > 'a'}, DESC ordering places releases ABOVE their prereleases:
   * {@code 2.0.10, 2.0.9, 2.0.0, 2.0.0-rc1, 2.0.0-beta}. This is the intended product behaviour.
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsOrdersMaven2ReleasesAboveTheirPrereleases() {
    // Maven2 normalised_version values captured from real PostgreSQL database
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "maven-artifact", "2.0.10",
        "000000002.000000000.000000010.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "maven-artifact", "2.0.9",
        "000000002.000000000.000000009.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "maven-artifact", "2.0.0",
        "000000002.000000000.000000000.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "maven-artifact", "2.0.0-rc1",
        "000000002.000000000.000000000.a.rc-000000001", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "maven-artifact", "2.0.0-beta",
        "000000002.000000000.000000000.a.beta", "releases");

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("name", "maven-artifact"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC")
        .limit(10)
        .offset(0)
        .build();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      List<String> actualVersions = session.access(SearchTableDAO.class)
          .browseComponentVersions(request)
          .stream()
          .map(ComponentVersionData::getVersion)
          .toList();

      assertThat(
          "Maven2 releases must sort ABOVE their prereleases (2.0.10, 2.0.9, 2.0.0, 2.0.0-rc1, 2.0.0-beta)",
          actualVersions,
          contains("2.0.10", "2.0.9", "2.0.0", "2.0.0-rc1", "2.0.0-beta"));
    }
  }

  @DatabaseTest(postgresql = true)
  void browseComponentVersionsAppliesLimitAndOffset() {
    for (int i = 1; i <= 5; i++) {
      insertSearchRow("test", "org.test", "paged", "1.0." + i, "releases");
    }

    ComponentVersionSearchRequest.Builder base = ComponentVersionSearchRequest.builder()
        .filter("cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("name", "paged"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchTableDAO dao = session.access(SearchTableDAO.class);
      assertThat(
          dao.browseComponentVersions(base.limit(2).offset(0).build())
              .stream()
              .map(ComponentVersionData::getVersion)
              .toList(),
          contains("1.0.5", "1.0.4"));
      assertThat(
          dao.browseComponentVersions(base.limit(2).offset(2).build())
              .stream()
              .map(ComponentVersionData::getVersion)
              .toList(),
          contains("1.0.3", "1.0.2"));
    }
  }

  /**
   * The {@code repositories} sort key is the only sort expression that is itself an aggregate
   * ({@code COUNT(DISTINCT cs.search_repository_name)}), and the only one that reaches the
   * conditional {@code cs.normalised_version DESC} tiebreaker — the default {@code version} sort
   * skips that branch because it already sorts on that column. Both were otherwise uncovered:
   * every other test here sorts on {@code cs.normalised_version}.
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsSortsByRepositoryCountThenNormalisedVersion() {
    // 2.0.0 in 3 repositories, then 1.0.9 and 1.0.10 in 2 each so their counts tie, then 0.9.0 in 1.
    // The tied pair is deliberately 1.0.9 / 1.0.10, the one case where normalised_version and the
    // raw version string disagree: normalised puts 1.0.10 first, a plain string sort puts 1.0.9
    // first. So this pins the conditional normalised_version tier specifically — with that tier
    // removed the order would fall through to the cs.version tier and invert.
    insertSearchRow("test", "org.test", "bycount", "2.0.0", "releases");
    insertSearchRow("test", "org.test", "bycount", "2.0.0", "snapshots");
    insertSearchRow("test", "org.test", "bycount", "2.0.0", "third");
    insertSearchRow("test", "org.test", "bycount", "1.0.9", "releases");
    insertSearchRow("test", "org.test", "bycount", "1.0.9", "snapshots");
    insertSearchRow("test", "org.test", "bycount", "1.0.10", "releases");
    insertSearchRow("test", "org.test", "bycount", "1.0.10", "snapshots");
    insertSearchRow("test", "org.test", "bycount", "0.9.0", "releases");

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("name", "bycount"))
        .sortExpression("COUNT(DISTINCT cs.search_repository_name)")
        .sortDirection("DESC")
        .limit(10)
        .offset(0)
        .build();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      List<String> actualVersions = session.access(SearchTableDAO.class)
          .browseComponentVersions(request)
          .stream()
          .map(ComponentVersionData::getVersion)
          .toList();

      assertThat(
          "highest repository count first, and versions tied on count fall back to normalised_version DESC",
          actualVersions,
          contains("2.0.0", "1.0.10", "1.0.9", "0.9.0"));
    }
  }

  /**
   * Distinct version strings can normalise to the same value: {@code MavenVersionNormalizer}
   * treats {@code 1}, {@code 1.0} and {@code 1.0.0} as equal, and documents {@code 2.0.a} /
   * {@code 2.0.0.a} as equivalent. Ordering by {@code normalised_version} alone therefore does not
   * define a total order, and neither engine promises a stable order for tied rows.
   *
   * <p>
   * The raw version has to break the tie, or paged browsing silently repeats and drops rows (see
   * {@link #browseComponentVersionsPagesTiedVersionsWithoutRepeatingOrDroppingRows()}).
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsBreaksNormalisationTiesDeterministically() {
    // 1.0 and 1.0.0 are different components that normalise identically, inserted lowest-first so
    // an unbroken tie comes back in scan order rather than the order the query promises.
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "tied", "1.0",
        "000000001.000000000.000000000.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "tied", "1.0.0",
        "000000001.000000000.000000000.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "tied", "0.9.0",
        "000000000.000000009.000000000.c.~", "releases");

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("name", "tied"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC")
        .limit(10)
        .offset(0)
        .build();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      List<String> actualVersions = session.access(SearchTableDAO.class)
          .browseComponentVersions(request)
          .stream()
          .map(ComponentVersionData::getVersion)
          .toList();

      assertThat(
          "versions tied on normalised_version must be ordered by the raw version, not scan order",
          actualVersions,
          contains("1.0.0", "1.0", "0.9.0"));
    }
  }

  /**
   * The pagination invariant the tiebreaker exists to protect: walking every page must yield each
   * distinct version exactly once. Without a total order a tie straddling a page boundary can be
   * returned on both pages, hiding whichever row it displaces.
   */
  @DatabaseTest(postgresql = true)
  void browseComponentVersionsPagesTiedVersionsWithoutRepeatingOrDroppingRows() {
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "tiedpaged", "1.0",
        "000000001.000000000.000000000.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "tiedpaged", "1.0.0",
        "000000001.000000000.000000000.c.~", "releases");
    insertSearchRowWithNormalisedVersion(
        "maven2", "org.test", "tiedpaged", "0.9.0",
        "000000000.000000009.000000000.c.~", "releases");

    ComponentVersionSearchRequest.Builder base = ComponentVersionSearchRequest.builder()
        .filter("cs.search_component_name = #{filterParams.name}")
        .filterParams(Map.of("name", "tiedpaged"))
        .sortExpression("cs.normalised_version")
        .sortDirection("DESC");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchTableDAO dao = session.access(SearchTableDAO.class);
      List<String> pagedVersions = new ArrayList<>();
      for (int offset = 0; offset < 3; offset++) {
        dao.browseComponentVersions(base.limit(1).offset(offset).build())
            .stream()
            .map(ComponentVersionData::getVersion)
            .forEach(pagedVersions::add);
      }

      assertThat(
          "paging one row at a time must return each distinct version exactly once",
          pagedVersions,
          containsInAnyOrder("1.0.0", "1.0", "0.9.0"));
    }
  }

  protected abstract SearchConditionFactory createSearchConditionFactory();

  /**
   * Helper to insert a single search row for the component-version tests.
   * Inserts directly into the search_components table using raw SQL, bypassing
   * the component existence check in the MyBatis save() method.
   */
  protected void insertSearchRow(
      String format,
      String namespace,
      String componentName,
      String version,
      String repositoryName)
  {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        Statement stmt = conn.createStatement()) {
      String normalisedVersion = VersionNumberExpander.expand(version);
      int componentId = generateUniqueComponentId();

      // PostgreSQL has NOT NULL constraints on tsvector columns, H2 does not
      String sql;
      if (isOnH2()) {
        sql = String.format(
            "INSERT INTO search_components (repository_id, component_id, format, namespace, "
                + "search_component_name, component_kind, version, normalised_version, search_repository_name, "
                + "last_modified, entity_version) VALUES (%d, %d, '%s', '%s', '%s', 'jar', '%s', '%s', '%s', "
                + "'2022-01-01 00:00:00', 1)",
            repository.contentRepositoryId(),
            componentId,
            format,
            namespace,
            componentName,
            version,
            normalisedVersion,
            repositoryName);
      }
      else {
        // PostgreSQL: must provide values for NOT NULL tsvector columns
        sql = String.format(
            "INSERT INTO search_components (repository_id, component_id, format, namespace, "
                + "search_component_name, component_kind, version, normalised_version, search_repository_name, "
                + "last_modified, entity_version, tsvector_format, tsvector_namespace, "
                + "tsvector_search_component_name, tsvector_version, tsvector_search_repository_name) "
                + "VALUES (%d, %d, '%s', '%s', '%s', 'jar', '%s', '%s', '%s', "
                + "'2022-01-01 00:00:00', 1, "
                + "to_tsvector('%s'), to_tsvector('%s'), to_tsvector('%s'), to_tsvector('%s'), to_tsvector('%s'))",
            repository.contentRepositoryId(),
            componentId,
            format,
            namespace,
            componentName,
            version,
            normalisedVersion,
            repositoryName,
            format,
            namespace,
            componentName,
            version,
            repositoryName);
      }
      stmt.executeUpdate(sql);
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to insert search row", e);
    }
  }

  /**
   * Helper to insert a search row with an explicitly provided {@code normalised_version}.
   *
   * <p>
   * Use this when testing format-specific normalisation strategies (e.g., Maven2's
   * {@code MavenVersionNormalizer}) that differ from the fallback {@link VersionNumberExpander}.
   *
   * @param format the format name (e.g., "maven2")
   * @param namespace the component namespace
   * @param componentName the component name
   * @param version the raw version string (e.g., "2.0.0-beta")
   * @param normalisedVersion the format-specific normalised version (e.g., "000000002.000000000.000000000.a.beta")
   * @param repositoryName the repository name
   */
  protected void insertSearchRowWithNormalisedVersion(
      String format,
      String namespace,
      String componentName,
      String version,
      String normalisedVersion,
      String repositoryName)
  {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        Statement stmt = conn.createStatement()) {
      int componentId = generateUniqueComponentId();

      String sql;
      if (isOnH2()) {
        sql = String.format(
            "INSERT INTO search_components (repository_id, component_id, format, namespace, "
                + "search_component_name, component_kind, version, normalised_version, search_repository_name, "
                + "last_modified, entity_version) VALUES (%d, %d, '%s', '%s', '%s', 'jar', '%s', '%s', '%s', "
                + "'2022-01-01 00:00:00', 1)",
            repository.contentRepositoryId(),
            componentId,
            format,
            namespace,
            componentName,
            version,
            normalisedVersion,
            repositoryName);
      }
      else {
        // PostgreSQL: must provide values for NOT NULL tsvector columns
        sql = String.format(
            "INSERT INTO search_components (repository_id, component_id, format, namespace, "
                + "search_component_name, component_kind, version, normalised_version, search_repository_name, "
                + "last_modified, entity_version, tsvector_format, tsvector_namespace, "
                + "tsvector_search_component_name, tsvector_version, tsvector_search_repository_name) "
                + "VALUES (%d, %d, '%s', '%s', '%s', 'jar', '%s', '%s', '%s', "
                + "'2022-01-01 00:00:00', 1, "
                + "to_tsvector('%s'), to_tsvector('%s'), to_tsvector('%s'), to_tsvector('%s'), to_tsvector('%s'))",
            repository.contentRepositoryId(),
            componentId,
            format,
            namespace,
            componentName,
            version,
            normalisedVersion,
            repositoryName,
            format,
            namespace,
            componentName,
            version,
            repositoryName);
      }
      stmt.executeUpdate(sql);
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to insert search row with normalised version", e);
    }
  }

  private int generateUniqueComponentId() {
    return componentIdCounter++;
  }

  protected boolean isOnH2() {
    return conditionBuilder instanceof H2SearchConditionFactory;
  }
}
