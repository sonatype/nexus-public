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
package org.sonatype.nexus.repository.golang.internal.util;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for working with Go routes and paths.
 *
 * @since 3.17
 */
@Named
@Singleton
public class GolangPathUtils
{
  /**
   * Returns the module from a {@link
   * TokenMatcher.State}.
   */
  public String module(final TokenMatcher.State state) {
    return match(state, "module");
  }

  /**
   * Returns the version from a {@link TokenMatcher.State}.
   */
  public String version(final TokenMatcher.State state) {
    return match(state, "version");
  }

  /**
   * Returns the extension from a {@link TokenMatcher.State}.
   */
  public String extension(final TokenMatcher.State state) {
    return match(state, "extension");
  }

  /**
   * Builds a go asset path from a {@link TokenMatcher.State}.
   */
  public String assetPath(final TokenMatcher.State state) {
    String module = module(state);
    String version = version(state);
    String extension = extension(state);
    String fullPath = String.format("%s/@v/%s.%s", module, version, extension);

    return fullPath;
  }

  /**
   * Builds a go list path from a {@link TokenMatcher.State}.
   */
  public String listPath(final TokenMatcher.State state) {
    String module = module(state);

    return String.format("%s/@v/list", module);
  }

  /**
   * Builds a go latest path from a {@link TokenMatcher.State}.
   */
  public String latestPath(final TokenMatcher.State state) {
    String module = module(state);

    return String.format("%s/@latest", module);
  }

  /**
   * Utility method encapsulating getting a particular token by name from a matcher, including preconditions.
   */
  private String match(final TokenMatcher.State state, final String name) {
    checkNotNull(state);
    String result = state.getTokens().get(name);
    checkNotNull(result);
    return result;
  }

  /**
   * Returns the {@link TokenMatcher.State} for the content.
   */
  public TokenMatcher.State matcherState(final Context context) {
    return context.getAttributes().require(TokenMatcher.State.class);
  }

  /**
   * Returns the module name and version from a given {@link TokenMatcher.State}
   *
   * @param state
   * @return {@link GolangAttributes}
   */
  public GolangAttributes getAttributesFromMatcherState(final TokenMatcher.State state) {
    GolangAttributes golangAttributes = new GolangAttributes();
    golangAttributes.setModule(module(state));
    golangAttributes.setVersion(version(state));

    return golangAttributes;
  }
}
