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
package org.sonatype.nexus.repository.view.matchers.token;

import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Request;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Matcher} that examines the {@link Request#getPath() request path} and attempts to parse it using the
 * {@link TokenParser}.
 *
 * If there is a match, the tokens are stored in the context under key {@link TokenMatcher.State}.
 *
 * @since 3.0
 */
public class TokenMatcher
    extends ComponentSupport
    implements Matcher
{
  public interface State
  {
    String pattern();

    Map<String, String> getTokens();
  }

  private final TokenParser parser;

  private final String pattern;

  public TokenMatcher(final String pattern) {
    this.pattern = checkNotNull(pattern);
    this.parser = new TokenParser(pattern);
  }

  @Override
  public boolean matches(final Context context) {
    checkNotNull(context);

    String path = context.getRequest().getPath();
    log.debug("Matching: {}~={}", path, parser);
    final Map<String, String> tokens = parser.parse(path);
    if (tokens == null) {
      // There was no match.
      return false;
    }

    // matched expose tokens in context
    context.getAttributes().set(State.class, new State()
    {
      @Override
      public String pattern() {
        return pattern;
      }

      @Override
      public Map<String, String> getTokens() {
        return tokens;
      }
    });
    return true;
  }

  @Override
  public String toString() {
    return "TokenMatcher [pattern=" + pattern + "]";
  }
}
