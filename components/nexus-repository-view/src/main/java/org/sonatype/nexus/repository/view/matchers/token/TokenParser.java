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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkState;


/**
 * Parses path-like strings against a template pattern, a literal string with embedded variables. e.g. {@code
 * "/{binName}/{bucketName}"} would match {@code "/bin12/overages"}.
 *
 * By default, variables are matched against the regexp {@link PatternParser#DEFAULT_VARIABLE_REGEXP}, which
 * excludes the path separator ({@code '/'}). To override this, specify the regexp explicitly:
 *
 * e.g. {@code "/{group:.+}/{artifact}/{version}/{name}-{version}.{ext}"}
 *
 * {@code "{letterCode:[A-Za-z]}"}<
 *
 * The backslash ({@code '\'}) serves as an escape character, if (say) it is necessary to use curly braces in the
 * regular expression.  If escape characters appear in regular expressions, these must be double-escaped.
 *
 * Caveat: the {@link TokenParser} cannot handle groups in variable regexp definitions. This will cause
 * parsing errors.
 *
 * @since 3.0
 */
public class TokenParser
  extends ComponentSupport
{
  private final List<VariableToken> variables;

  private final Pattern pattern;

  public TokenParser(final String templatePattern) {
    final List<Token> tokens = new PatternParser(templatePattern).getTokens();
    pattern = Pattern.compile(regexp(tokens));
    log.trace("Pattern: {}", pattern);

    // Separate the variable tokens
    variables = new ArrayList<>();
    for (Token token : tokens) {
      if (token instanceof VariableToken) {
        variables.add((VariableToken) token);
      }
    }
  }

  /**
   * Attempts to parse the provided path against the template pattern.  If the pattern matches, the resulting Map
   * contains an entry for each variable in the pattern. The variable names are keys, with the matching portions of the
   * path as values.  Returns {@code null} if the pattern does not match.
   */
  @Nullable
  public Map<String, String> parse(final String path) {
    final Matcher matcher = pattern.matcher(path);
    if (!matcher.matches()) {
      return null;
    }

    checkState(matcher.groupCount() == variables.size(),
        "Mismatch between the number of captured groups (%s) and the number of variables, %s.", matcher.groupCount(),
        variables.size());

    Map<String, String> values = new HashMap<>();
    for (int i = 0; i < matcher.groupCount(); i++) {
      final String name = variables.get(i).getName();
      final String value = matcher.group(i + 1);
      if (values.containsKey(name)) {
        final String existingValue = values.get(name);
        if (!Objects.equals(existingValue, value)) {
          log.trace("Variable '{}' values mismatch: '{}' vs '{}'", name, existingValue, value);
          return null;
        }
      }
      else {
        values.put(name, value);
      }
    }

    return values;
  }

  public String getPattern() {
    return pattern.toString();
  }

  private String regexp(final List<Token> tokens) {
    StringBuilder b = new StringBuilder();
    for (Token token : tokens) {
      b.append(token.toRegexp());
    }
    return b.toString();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "pattern=" + pattern +
        ", variables=" + variables +
        '}';
  }
}
