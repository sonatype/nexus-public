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
package org.sonatype.nexus.security.authz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link PrincipalPermissionsCache} answers permission checks identically to plain Shiro semantics.
 *
 * <p>
 * {@link PrincipalPermissionsCache} does not scan every grant any more. After trailing {@code ":*"} parts are stripped
 * (which Shiro treats as redundant), a grant with no {@code '*'} and no comma-form action list is answered from a hash
 * set by equality or colon-prefix; only the rest keep the linear scan (NEXUS-54434). That is a change to the
 * authorization path, so these tests pin it against an independent oracle rather than against its own expectations.
 *
 * <p>
 * The oracle is {@link #shiroScan}: a plain loop calling {@link Permission#implies} over every grant. That <em>is</em>
 * Shiro's definition of the answer, so agreement with it is the property that matters. Every assertion here compares
 * the cache against the oracle rather than against a hard-coded boolean, except where a hard-coded expectation
 * documents a rule that is easy to get wrong (see {@link #builtInCrudPrivilegesUseCommaFormActions}).
 *
 * <p>
 * Permission strings are the real shapes produced in the wild, taken from the permission classes themselves:
 * {@code ApplicationPermission} is {@code nexus:<domain>:<actions>}, {@code RepositoryViewPermission} and
 * {@code RepositoryAdminPermission} are {@code nexus:<domain>:<format>:<repository>:<actions>}, and
 * {@code RepositoryContentSelectorPermission} is
 * {@code nexus:repository-content-selector:<selector>:<format>:<repository>:<actions>}.
 */
class PrincipalPermissionsCacheMatchingTest
{
  private static final String REALM = "TestRealm";

  /** Distinct principals per assertion, so one scenario's cached state never answers another's. */
  private final AtomicInteger principalSequence = new AtomicInteger();

  // ---------------------------------------------------------------------------------------------
  // Real permission shapes
  // ---------------------------------------------------------------------------------------------

  /** {@code nexus:<domain>:<actions>} - ApplicationPermission. */
  private static Permission application(final String domain, final String actions) {
    return new WildcardPermission2("nexus:" + domain + ":" + actions);
  }

  /** {@code nexus:repository-view:<format>:<repository>:<actions>} - RepositoryViewPermission. */
  private static Permission repositoryView(final String format, final String repository, final String actions) {
    return new WildcardPermission2("nexus:repository-view:" + format + ":" + repository + ":" + actions);
  }

  /** {@code nexus:repository-admin:<format>:<repository>:<actions>} - RepositoryAdminPermission. */
  private static Permission repositoryAdmin(final String format, final String repository, final String actions) {
    return new WildcardPermission2("nexus:repository-admin:" + format + ":" + repository + ":" + actions);
  }

  /** {@code nexus:repository-content-selector:<selector>:<format>:<repository>:<actions>}. */
  private static Permission contentSelector(
      final String selector,
      final String format,
      final String repository,
      final String actions)
  {
    return new WildcardPermission2(
        "nexus:repository-content-selector:" + selector + ":" + format + ":" + repository + ":" + actions);
  }

  // ---------------------------------------------------------------------------------------------
  // Real grant sets, as the built-in roles actually expand
  // ---------------------------------------------------------------------------------------------

  /** {@code nx-admin} holds the single {@code nx-all} privilege. */
  private static List<Permission> adminGrants() {
    return List.of(new WildcardPermission2("nexus:*"));
  }

  /**
   * {@code nx-anonymous} as defined by {@code NexusSecurityContributor}: nx-search-read, nx-healthcheck-read,
   * nx-repository-view-*-*-browse and nx-repository-view-*-*-read.
   */
  private static List<Permission> anonymousGrants() {
    return List.of(
        application("search", "read"),
        application("healthcheck", "read"),
        repositoryView("*", "*", "browse"),
        repositoryView("*", "*", "read"));
  }

  /** A team developer: read and browse on a handful of named repositories, plus search. */
  private static List<Permission> developerGrants() {
    return List.of(
        application("search", "read"),
        repositoryView("maven2", "maven-releases", "read"),
        repositoryView("maven2", "maven-releases", "browse"),
        repositoryView("maven2", "maven-snapshots", "read"),
        repositoryView("maven2", "maven-snapshots", "browse"),
        repositoryView("npm", "npm-proxy", "read"),
        repositoryView("npm", "npm-proxy", "browse"));
  }

  /** A release job: upload rights on one repository, expressed as a comma-form action list. */
  private static List<Permission> ciServiceAccountGrants() {
    return List.of(
        repositoryView("maven2", "maven-releases", "add,read"),
        application("component", "create,read"));
  }

  /** A format administrator: wildcarded over every repository of one format. */
  private static List<Permission> formatAdminGrants() {
    return List.of(
        repositoryAdmin("maven2", "*", "*"),
        repositoryView("maven2", "*", "*"));
  }

  /**
   * A realistic privilege catalogue: the built-in application privileges (note the comma-form CRUD actions produced by
   * {@code SecurityContributorSupport.ACTION_CREATE} and friends), the per-format wildcard privileges, and the
   * per-repository privileges created when a repository is added - measured at 11 per repository, six repository-view
   * and five repository-admin.
   */
  private static List<Permission> realisticCatalogue(final int repositories) {
    List<Permission> catalogue = new ArrayList<>();

    catalogue.add(new WildcardPermission2("nexus:*"));
    for (String domain : List.of("settings", "bundles", "search", "apikey", "healthcheck", "userschangepw")) {
      catalogue.add(application(domain, "*"));
      catalogue.add(application(domain, "read"));
    }
    // Built-in CRUD privileges pair each mutating action with read, e.g. nx-users-create is "create,read".
    for (String domain : List.of("privileges", "roles", "users")) {
      catalogue.add(application(domain, "*"));
      catalogue.add(application(domain, "read"));
      catalogue.add(application(domain, "create,read"));
      catalogue.add(application(domain, "update,read"));
      catalogue.add(application(domain, "delete,read"));
    }
    catalogue.add(application("component", "create,read"));

    List<String> formats = List.of("maven2", "npm", "docker", "raw", "pypi", "nuget");
    List<String> actions = List.of("*", "browse", "read", "edit", "add", "delete");
    for (String action : actions) {
      catalogue.add(repositoryView("*", "*", action));
      for (String format : formats) {
        catalogue.add(repositoryView(format, "*", action));
        catalogue.add(repositoryAdmin(format, "*", action));
      }
    }
    for (int i = 0; i < repositories; i++) {
      String format = formats.get(i % formats.size());
      String name = format + "-repo-" + i;
      for (String action : actions) {
        catalogue.add(repositoryView(format, name, action));
      }
    }
    return catalogue;
  }

  // ---------------------------------------------------------------------------------------------
  // Oracle and assertions
  // ---------------------------------------------------------------------------------------------

  /** Shiro's definition of the answer: does any grant imply the checked permission? */
  private static boolean shiroScan(final Collection<Permission> grants, final Permission checked) {
    for (Permission granted : grants) {
      if (granted.implies(checked)) {
        return true;
      }
    }
    return false;
  }

  private static PrincipalPermissionsCache newCache() {
    return new PrincipalPermissionsCache(true, 1000, Duration.ofMinutes(2), false, 4);
  }

  private PrincipalCollection nextPrincipal() {
    return new SimplePrincipalCollection("subject-" + principalSequence.incrementAndGet(), REALM);
  }

  /** Asserts the cache agrees with the oracle for every checked permission, and returns its answers. */
  private boolean[] assertMatchesShiro(final Collection<Permission> grants, final List<Permission> checks) {
    boolean[] answers = newCache().isPermitted(nextPrincipal(), checks, () -> grants);
    assertEquals(checks.size(), answers.length, "one answer per checked permission");
    for (int i = 0; i < checks.size(); i++) {
      assertEquals(shiroScan(grants, checks.get(i)), answers[i],
          "disagrees with Shiro for " + checks.get(i) + " given grants " + grants);
    }
    return answers;
  }

  private boolean permitted(final Collection<Permission> grants, final Permission checked) {
    return assertMatchesShiro(grants, List.of(checked))[0];
  }

  // ---------------------------------------------------------------------------------------------
  // Real-world grant sets
  // ---------------------------------------------------------------------------------------------

  @Test
  void adminHoldsEveryPrivilegeInTheCatalogue() {
    List<Permission> catalogue = realisticCatalogue(50);
    boolean[] answers = assertMatchesShiro(adminGrants(), catalogue);
    for (int i = 0; i < answers.length; i++) {
      assertTrue(answers[i], "nexus:* must imply " + catalogue.get(i));
    }
  }

  @Test
  void anonymousRoleSeesBrowseAndReadOnlyAcrossAllRepositories() {
    List<Permission> grants = anonymousGrants();

    assertTrue(permitted(grants, repositoryView("maven2", "maven-central", "read")));
    assertTrue(permitted(grants, repositoryView("npm", "npm-proxy", "browse")));
    assertTrue(permitted(grants, application("search", "read")));

    assertFalse(permitted(grants, repositoryView("maven2", "maven-central", "add")));
    assertFalse(permitted(grants, repositoryView("maven2", "maven-central", "delete")));
    assertFalse(permitted(grants, repositoryAdmin("maven2", "maven-central", "read")));
    assertFalse(permitted(grants, application("settings", "read")));
  }

  @Test
  void anonymousRoleAgainstTheWholeCatalogue() {
    assertMatchesShiro(anonymousGrants(), realisticCatalogue(60));
  }

  /**
   * The built-in CRUD privileges are comma-form: {@code SecurityContributorSupport.ACTION_CREATE} is
   * {@code "create,read"}, so {@code nx-users-create} grants {@code nexus:users:create,read}. Such a grant implies each
   * of its own actions but not the others, and a single-action grant does not imply a comma-form check.
   */
  @Test
  void builtInCrudPrivilegesUseCommaFormActions() {
    List<Permission> createGrant = List.of(application("users", "create,read"));

    assertTrue(permitted(createGrant, application("users", "create")));
    assertTrue(permitted(createGrant, application("users", "read")));
    assertTrue(permitted(createGrant, application("users", "create,read")));

    assertFalse(permitted(createGrant, application("users", "update")));
    assertFalse(permitted(createGrant, application("users", "delete")));

    // A single-action grant cannot satisfy a check demanding two actions.
    assertFalse(permitted(List.of(application("users", "read")), application("users", "read,update")));
  }

  @Test
  void perRepositoryDeveloperGrants() {
    List<Permission> grants = developerGrants();

    assertTrue(permitted(grants, repositoryView("maven2", "maven-releases", "read")));
    assertTrue(permitted(grants, repositoryView("npm", "npm-proxy", "browse")));

    // Not granted: a different repository, a different action, and the admin domain.
    assertFalse(permitted(grants, repositoryView("maven2", "maven-central", "read")));
    assertFalse(permitted(grants, repositoryView("maven2", "maven-releases", "delete")));
    assertFalse(permitted(grants, repositoryAdmin("maven2", "maven-releases", "read")));

    assertMatchesShiro(grants, realisticCatalogue(60));
  }

  @Test
  void ciServiceAccountWithCommaFormUploadGrant() {
    List<Permission> grants = ciServiceAccountGrants();

    assertTrue(permitted(grants, repositoryView("maven2", "maven-releases", "add")));
    assertTrue(permitted(grants, repositoryView("maven2", "maven-releases", "read")));
    assertTrue(permitted(grants, application("component", "create")));

    assertFalse(permitted(grants, repositoryView("maven2", "maven-releases", "delete")));
    assertFalse(permitted(grants, repositoryView("maven2", "maven-snapshots", "add")));

    assertMatchesShiro(grants, realisticCatalogue(60));
  }

  @Test
  void formatScopedWildcardGrants() {
    List<Permission> grants = formatAdminGrants();

    assertTrue(permitted(grants, repositoryView("maven2", "any-repo-at-all", "read")));
    assertTrue(permitted(grants, repositoryAdmin("maven2", "another-repo", "delete")));

    assertFalse(permitted(grants, repositoryView("npm", "npm-proxy", "read")));
    assertFalse(permitted(grants, application("settings", "read")));

    assertMatchesShiro(grants, realisticCatalogue(60));
  }

  @Test
  void contentSelectorGrants() {
    List<Permission> grants = List.of(
        contentSelector("only-com-example", "maven2", "maven-releases", "read"),
        contentSelector("only-com-example", "maven2", "maven-releases", "browse"));

    assertTrue(permitted(grants, contentSelector("only-com-example", "maven2", "maven-releases", "read")));

    assertFalse(permitted(grants, contentSelector("everything", "maven2", "maven-releases", "read")));
    assertFalse(permitted(grants, contentSelector("only-com-example", "maven2", "maven-releases", "add")));
    // A content selector grant must not leak into the plain repository-view domain.
    assertFalse(permitted(grants, repositoryView("maven2", "maven-releases", "read")));
  }

  @Test
  void mixedGrantSetSpanningEveryShape() {
    List<Permission> grants = new ArrayList<>();
    grants.addAll(developerGrants()); // wildcard-free, comma-free
    grants.addAll(ciServiceAccountGrants()); // comma-form
    grants.add(repositoryView("docker", "*", "read")); // wildcarded
    grants.add(application("privileges", "read"));

    assertMatchesShiro(grants, realisticCatalogue(80));
  }

  // ---------------------------------------------------------------------------------------------
  // Shiro rules that are easy to get wrong
  // ---------------------------------------------------------------------------------------------

  /**
   * A grant that runs out of parts implies everything below it. This is why a wildcard-free grant must be probed
   * against every colon-prefix of the checked permission, not only against the whole string.
   */
  @Test
  void shorterGrantImpliesEverythingBelowIt() {
    List<Permission> grants = List.of(new WildcardPermission2("nexus:repository-view"));

    assertTrue(permitted(grants, repositoryView("maven2", "maven-releases", "read")));
    assertTrue(permitted(grants, new WildcardPermission2("nexus:repository-view")));
    assertFalse(permitted(grants, application("settings", "read")));
  }

  /**
   * The corollary that the previous fast path missed: a wildcard-free grant can imply a <em>wildcarded</em> check,
   * because Shiro returns true as soon as the grant runs out of parts. The probe must therefore not be skipped just
   * because the checked permission contains a '*'.
   */
  @Test
  void wildcardFreeGrantImpliesWildcardedCheck() {
    List<Permission> grants = List.of(new WildcardPermission2("nexus:repository-view"));

    assertTrue(permitted(grants, new WildcardPermission2("nexus:repository-view:*:*:*")));
    assertTrue(permitted(grants, new WildcardPermission2("nexus:repository-view:maven2:*:read")));
  }

  /** A longer grant cannot imply a shorter permission, since its extra parts are not wildcards. */
  @Test
  void longerGrantDoesNotImplyShorterPermission() {
    List<Permission> grants = List.of(repositoryView("maven2", "maven-releases", "read"));

    assertFalse(permitted(grants, new WildcardPermission2("nexus:repository-view")));
    assertFalse(permitted(grants, new WildcardPermission2("nexus:repository-view:maven2")));
    assertTrue(permitted(grants, repositoryView("maven2", "maven-releases", "read")));
  }

  /**
   * A trailing {@code ":*"} is redundant in Shiro: a grant implies everything below the point where it runs out of
   * parts, so {@code "nexus:foo:*"} and {@code "nexus:foo"} imply exactly the same permissions.
   *
   * <p>
   * This is not an academic case. Every repository created gets an all-actions privilege of this shape
   * ({@code nx-repository-view-raw-releases-*}), so a user granted all actions on many repositories holds many such
   * grants.
   */
  @Test
  void trailingWildcardGrantMatchesItsTruncatedForm() {
    List<Permission> starred = List.of(repositoryView("maven2", "maven-releases", "*"));
    List<Permission> truncated = List.of(new WildcardPermission2("nexus:repository-view:maven2:maven-releases"));

    List<Permission> catalogue = realisticCatalogue(60);
    boolean[] starredAnswers = assertMatchesShiro(starred, catalogue);
    boolean[] truncatedAnswers = assertMatchesShiro(truncated, catalogue);
    assertTrue(Arrays.equals(starredAnswers, truncatedAnswers),
        "a trailing ':*' grant must answer identically to its truncated form");

    // And it really does grant every action on that repository, but nothing on another.
    for (String action : List.of("read", "browse", "add", "edit", "delete")) {
      assertTrue(permitted(starred, repositoryView("maven2", "maven-releases", action)));
      assertFalse(permitted(starred, repositoryView("maven2", "maven-snapshots", action)));
    }
    assertFalse(permitted(starred, repositoryAdmin("maven2", "maven-releases", "read")));
  }

  /** Several trailing wildcards collapse the same way: {@code "nexus:foo:*:*"} still means {@code "nexus:foo"}. */
  @Test
  void repeatedTrailingWildcardsCollapse() {
    List<Permission> grants = List.of(new WildcardPermission2("nexus:repository-view:*:*"));

    assertMatchesShiro(grants, realisticCatalogue(60));
    assertTrue(permitted(grants, repositoryView("maven2", "anything", "read")));
    assertFalse(permitted(grants, repositoryAdmin("maven2", "anything", "read")));
  }

  /** An interior wildcard is not redundant and must keep full matching. */
  @Test
  void interiorWildcardIsNotTreatedAsAPrefix() {
    List<Permission> grants = List.of(repositoryView("*", "maven-releases", "read"));

    assertTrue(permitted(grants, repositoryView("maven2", "maven-releases", "read")));
    assertTrue(permitted(grants, repositoryView("npm", "maven-releases", "read")));
    assertFalse(permitted(grants, repositoryView("maven2", "maven-snapshots", "read")));

    assertMatchesShiro(grants, realisticCatalogue(60));
  }

  /** The bare {@code "*"} grant means everything, and must not be mistaken for a prefix. */
  @Test
  void bareWildcardGrantsEverything() {
    List<Permission> grants = List.of(new WildcardPermission2("*"));
    List<Permission> catalogue = realisticCatalogue(40);
    for (boolean answer : assertMatchesShiro(grants, catalogue)) {
      assertTrue(answer, "a bare '*' grant must imply everything");
    }
  }

  /** A grant set holding nothing hash-indexable still has to answer correctly. */
  @Test
  void grantSetContainingOnlyWildcards() {
    List<Permission> grants = List.of(
        repositoryView("maven2", "*", "read"),
        repositoryView("npm", "*", "*"));

    assertMatchesShiro(grants, realisticCatalogue(60));
  }

  @Test
  void emptyGrantSetPermitsNothing() {
    List<Permission> catalogue = realisticCatalogue(20);
    boolean[] answers = assertMatchesShiro(List.of(), catalogue);
    for (boolean answer : answers) {
      assertFalse(answer, "an empty grant set must permit nothing");
    }
  }

  /** A {@link Permission} that is not a {@link org.apache.shiro.authz.permission.WildcardPermission}. */
  @Test
  void nonWildcardPermissionGrantsAreStillConsulted() {
    Permission grantsEverything = permission -> true;
    List<Permission> grants = List.of(grantsEverything);

    assertMatchesShiro(grants, realisticCatalogue(20));
    assertTrue(permitted(grants, application("settings", "read")));
  }

  // ---------------------------------------------------------------------------------------------
  // Randomised equivalence
  // ---------------------------------------------------------------------------------------------

  /**
   * Randomised differential test against the oracle. The alphabets are deliberately small so that generated grants and
   * checks collide often - collisions are where prefix and comma handling break. A fixed seed keeps failures
   * reproducible; a wider sweep (millions of cases across many seeds) was run while developing the change and is not
   * repeated on every build.
   */
  @Test
  void randomisedGrantsAndChecksAgreeWithShiro() {
    String[] segments = {"nexus", "repository-view", "repository-admin", "settings", "maven2", "releases", "a"};
    String[] actions = {"read", "browse", "edit", "add", "delete", "update"};
    Random random = new Random(20260818L);

    int grantSets = 1500;
    int checksPerSet = 30;
    int trueAnswers = 0;

    for (int i = 0; i < grantSets; i++) {
      List<Permission> grants = new ArrayList<>();
      int grantCount = 1 + random.nextInt(6);
      for (int j = 0; j < grantCount; j++) {
        grants.add(randomPermission(random, segments, actions, true));
      }
      List<Permission> checks = new ArrayList<>();
      for (int j = 0; j < checksPerSet; j++) {
        checks.add(randomPermission(random, segments, actions, random.nextInt(4) == 0));
      }
      for (boolean answer : assertMatchesShiro(grants, checks)) {
        if (answer) {
          trueAnswers++;
        }
      }
    }

    // Guard against a vacuous pass: if the generator stopped producing grants that match anything, the comparison
    // above would agree trivially on "false" everywhere and prove nothing.
    int comparisons = grantSets * checksPerSet;
    assertTrue(trueAnswers > comparisons / 100,
        "generator produced too few permitted answers (" + trueAnswers + " of " + comparisons + ") to be meaningful");
  }

  private static Permission randomPermission(
      final Random random,
      final String[] segments,
      final String[] actions,
      final boolean allowWildcards)
  {
    int parts = 1 + random.nextInt(5);
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parts; i++) {
      if (i > 0) {
        builder.append(':');
      }
      int roll = random.nextInt(10);
      if (allowWildcards && roll == 0) {
        builder.append('*');
      }
      else if (roll == 1) {
        // Comma-form action list, as the built-in CRUD privileges use.
        String first = actions[random.nextInt(actions.length)];
        String second = actions[random.nextInt(actions.length)];
        builder.append(first.equals(second) ? first : first + "," + second);
      }
      else if (roll == 2) {
        builder.append(actions[random.nextInt(actions.length)]);
      }
      else {
        builder.append(segments[random.nextInt(segments.length)]);
      }
    }
    return new WildcardPermission2(builder.toString());
  }

  /** The batch and single-permission entry points must not disagree with each other. */
  @Test
  void batchAndSingleCheckAgree() {
    List<Permission> grants = new ArrayList<>(developerGrants());
    grants.add(repositoryView("docker", "*", "read"));
    List<Permission> catalogue = realisticCatalogue(40);

    PrincipalPermissionsCache cache = newCache();
    PrincipalCollection principal = nextPrincipal();
    boolean[] batch = cache.isPermitted(principal, catalogue, () -> grants);

    PrincipalPermissionsCache singleCache = newCache();
    PrincipalCollection singlePrincipal = nextPrincipal();
    for (int i = 0; i < catalogue.size(); i++) {
      boolean single = singleCache.isPermitted(singlePrincipal, catalogue.get(i), () -> grants);
      assertEquals(batch[i], single, "batch and single check disagree for " + catalogue.get(i));
    }
  }

  /** Caching must not change the answer: the same question asked twice gets the same answer. */
  @Test
  void repeatedChecksAreStable() {
    List<Permission> grants = new ArrayList<>(developerGrants());
    grants.addAll(ciServiceAccountGrants());
    List<Permission> catalogue = realisticCatalogue(40);

    PrincipalPermissionsCache cache = newCache();
    PrincipalCollection principal = nextPrincipal();
    boolean[] first = cache.isPermitted(principal, catalogue, () -> grants);
    boolean[] second = cache.isPermitted(principal, catalogue, () -> grants);

    assertTrue(Arrays.equals(first, second), "a repeated whole-catalogue check changed its answers");
    for (int i = 0; i < catalogue.size(); i++) {
      assertEquals(shiroScan(grants, catalogue.get(i)), first[i], "disagrees with Shiro for " + catalogue.get(i));
    }
  }
}
