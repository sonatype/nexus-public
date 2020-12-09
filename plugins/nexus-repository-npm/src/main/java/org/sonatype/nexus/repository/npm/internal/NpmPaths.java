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
package org.sonatype.nexus.repository.npm.internal;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Route.Builder;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher;
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;

/**
 * @since 3.27
 */
public final class NpmPaths
{
  public static final String T_TOKEN = "token";

  public static final String T_PACKAGE_NAME = "packageName";

  public static final String T_PACKAGE_VERSION = "packageVersion";

  public static final String T_PACKAGE_TAG = "packageTag";

  public static final String T_PACKAGE_SCOPE = "packageScope";

  public static final String T_REVISION = "revision";

  public static final String T_TARBALL_NAME = "tarballName";

  public static final String T_USERNAME = "userName";

  public static final String USER_LOGIN_PREFIX = "/-/user/org.couchdb.user:";

  /**
   * Matcher for npm package search index.
   */
  public static Builder searchIndexMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET),
            LogicMatchers.or(
                new LiteralMatcher("/-/all"),
                new LiteralMatcher("/-/all/since")
            )
        )
    );
  }

  /**
   * Matcher for npm package v1 search.
   */
  public static Builder searchV1Matcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET),
            new LiteralMatcher("/-/v1/search")
        )
      );
  }

  /**
   * Matcher for npm whoami command.
   */
  public static Builder whoamiMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET),
            new LiteralMatcher("/-/whoami")
        )
    );
  }

  /**
   * Matcher for npm ping command.
   */
  public static Builder pingMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET),
            new LiteralMatcher("/-/ping")
        )
    );
  }

  /**
   * Matcher for npm package metadata.
   */
  public static Builder packageMatcher(final String ...httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher("/{" + T_PACKAGE_NAME + "}"),
                new TokenMatcher("/@{" + T_PACKAGE_SCOPE + "}/{" + T_PACKAGE_NAME + "}"),
                new TokenMatcher("/{" + T_PACKAGE_NAME + "}/{" + T_PACKAGE_VERSION + "}")
            )
        )
    );
  }

  /**
   * Matcher for npm package metadata.
   */
  public static Builder packageMatcherWithRevision(final String httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher("/{" + T_PACKAGE_NAME + "}/-rev/{" + T_REVISION + "}"),
                new TokenMatcher("/@{" + T_PACKAGE_SCOPE + "}/{" + T_PACKAGE_NAME + "}/-rev/{" + T_REVISION + "}")
            )
        )
    );
  }

  /**
   * Matcher for npm package tarballs.
   */
  public static Builder tarballMatcher(final String ...httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher("/{" + T_PACKAGE_NAME + "}/-/{" + T_TARBALL_NAME + "}"),
                new TokenMatcher("/@{" + T_PACKAGE_SCOPE + "}/{" + T_PACKAGE_NAME + "}/-/{" + T_TARBALL_NAME + "}")
            )
        )
    );
  }

  /**
   * Matcher for npm package dist-tags.
   */
  public static Builder distTagsMatcher(final String httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher("/-/package/{" + T_PACKAGE_NAME + "}/dist-tags"),
                new TokenMatcher("/-/package/@{" + T_PACKAGE_SCOPE + "}/{" + T_PACKAGE_NAME + "}/dist-tags")
            )
        )
    );
  }

  /**
   * Matcher for npm package dist-tags.
   */
  public static Builder distTagsUpdateMatcher(final String httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher("/-/package/{" + T_PACKAGE_NAME + "}/dist-tags/{" + T_PACKAGE_TAG +"}"),
                new TokenMatcher("/-/package/@{" + T_PACKAGE_SCOPE + "}/{" + T_PACKAGE_NAME + "}/dist-tags/{"
                    + T_PACKAGE_TAG + "}")
            )
        )
    );
  }

  /**
   * Matcher for npm package tarballs.
   */
  public static Builder tarballMatcherWithRevision(final String httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            LogicMatchers.or(
                new TokenMatcher("/{" + T_PACKAGE_NAME + "}/-/{" + T_TARBALL_NAME + "}/-rev/{" + T_REVISION + "}"),
                new TokenMatcher("/@{" + T_PACKAGE_SCOPE + "}/{" + T_PACKAGE_NAME + "}/-/{" + T_TARBALL_NAME
                    + "}/-rev/{" + T_REVISION + "}")
            )
        )
    );
  }

  /**
   * Matcher for {@code npm adduser}.
   */
  public static Builder userMatcher(final String httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            new TokenMatcher(USER_LOGIN_PREFIX + "{" + T_USERNAME + "}")
        )
    );
  }

  /**
   * Matcher for {@code npm logout}.
   */
  public static Builder tokenMatcher(final String httpMethod) {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(httpMethod),
            new TokenMatcher("/-/user/token/{" + T_TOKEN + "}")
        )
    );
  }

  /**
   * Matcher for {@code npm audit}.
   */
  public static Builder auditMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(POST),
            new LiteralMatcher("/-/npm/v1/security/audits") // is used while npm audit
        )
    );
  }

  /**
   * Matcher for {@code npm audit quick}.
   */
  public static Builder auditQuickMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(POST),
            new LiteralMatcher("/-/npm/v1/security/audits/quick") // is used while npm install
        )
    );
  }

  @Nonnull
  public static NpmPackageId packageId(final TokenMatcher.State state) {
    checkNotNull(state);
    String packageName = state.getTokens().get(T_PACKAGE_NAME);
    checkNotNull(packageName);

    String version = state.getTokens().get(T_PACKAGE_VERSION);
    if (!isBlank(version)) {
      packageName += "-" + version;
    }

    String packageScope = state.getTokens().get(T_PACKAGE_SCOPE);
    return new NpmPackageId(packageScope, packageName);
  }

  public static Optional<String> version(final TokenMatcher.State state) {
    checkNotNull(state);

    return Optional.ofNullable(state.getTokens().get(T_PACKAGE_VERSION));
  }

  @Nonnull
  public static String tarballName(final TokenMatcher.State state) {
    checkNotNull(state);
    String tarballName = state.getTokens().get(T_TARBALL_NAME);
    checkNotNull(tarballName);
    return tarballName;
  }

  @Nullable
  public static DateTime indexSince(final Parameters parameters) {
    // npm "incremental" index support: tells when it did last updated index
    // GET /-/all/since?stale=update_after&startkey=1441712501000
    if (parameters != null && "update_after".equals(parameters.get("stale"))) {
      String tsStr = parameters.get("startkey");
      if (!isBlank(tsStr)) {
        try {
          return new DateTime(Long.parseLong(tsStr));
        }
        catch (NumberFormatException e) {
          // ignore
        }
      }
    }
    return null;
  }

  @Nullable
  static String revision(final TokenMatcher.State state) {
    checkNotNull(state);
    return state.getTokens().get(T_REVISION);
  }
}
