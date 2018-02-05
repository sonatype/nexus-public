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
package org.sonatype.nexus.repository.view.matchers;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Regular expression string matcher.
 *
 * @since 3.0
 */
public class RegexMatcher
  extends ComponentSupport
  implements Matcher
{
  public interface State
  {
    MatchResult getMatchResult();
  }

  private final Pattern pattern;

  public RegexMatcher(final Pattern pattern) {
    this.pattern = checkNotNull(pattern);
  }

  public RegexMatcher(final String regex, final int flags) {
    checkNotNull(regex);
    this.pattern = Pattern.compile(regex, flags);
  }

  public RegexMatcher(final String regex) {
    checkNotNull(regex);
    this.pattern = Pattern.compile(regex);
  }

  @Override
  public boolean matches(final Context context) {
    checkNotNull(context);

    String path = context.getRequest().getPath();
    log.debug("Matching: {}~={}", path, pattern);
    final java.util.regex.Matcher m = pattern.matcher(path);

    if (m.matches()) {
      // expose match result in context
      context.getAttributes().set(State.class, new State()
      {
        @Override
        public MatchResult getMatchResult() {
          return m;
        }
      });
      return true;
    }

    // no match
    return false;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "pattern=" + pattern +
        '}';
  }
}
