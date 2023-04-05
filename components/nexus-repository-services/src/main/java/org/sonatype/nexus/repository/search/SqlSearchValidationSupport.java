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
package org.sonatype.nexus.repository.search;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Support class for SQL search validation
 */
public abstract class SqlSearchValidationSupport
    extends ComponentSupport
{
  private static final int MIN_ALLOWED_SYMBOLS_TO_SEARCH = 3;

  /*
   * For SQL search we prohibit leading wildcards and less than 3 characters with wildcards for performance reasons.
   */
  protected Set<String> getValidTokens(final Set<String> tokens) {
    ValidationErrorsException validation = new ValidationErrorsException();
    Set<String> validTokens = new HashSet<>(tokens);

    Set<String> invalidTokens = tokens.stream()
        .filter(Objects::nonNull)
        .filter(SqlSearchValidationSupport::hasLeadingWildcard)
        .collect(Collectors.toSet());
    if (!invalidTokens.isEmpty()) {
      String errorMsg = "Leading wildcards are prohibited";
      validation.withError(errorMsg);
      log.debug(errorMsg + " for tokens: {}", invalidTokens);
      validTokens.removeAll(invalidTokens);
    }

    invalidTokens = tokens.stream()
            .filter(Objects::nonNull)
            .filter(SqlSearchValidationSupport::notEnoughSymbols)
            .collect(Collectors.toSet());
    if (!invalidTokens.isEmpty()) {
      String errorMsg = String.format("%d characters or more are required with a trailing wildcard (*)",
          MIN_ALLOWED_SYMBOLS_TO_SEARCH);
      validation.withError(errorMsg);
      log.debug(errorMsg + " for tokens: {}", invalidTokens);
      validTokens.removeAll(invalidTokens);
    }

    if (validTokens.isEmpty()) {
      log.debug("No valid search tokens");

      throw validation;
    }

    return validTokens;
  }

  private static boolean hasLeadingWildcard(final String token) {
    String trimmedToken = token.trim();
    return trimmedToken.startsWith("*") || trimmedToken.startsWith("?");
  }

  /**
   * Check if a given token contains trailing asterisk wildcard and returns a length of string without wildcard.
   *
   * @param token a token to check
   * @return the {@code true} or {@code false} if a {@code token} contains wildcard
   *         and a length of string without wildcard.
   */
  private static Pair<Boolean, Integer> checkTrailingAsterisk(final String token) {
    // The escaped asterisk (*) is not a wildcard token.
    String result = token.replace("\\*", "");

    boolean trailingAsteriskWildcard = result.endsWith("*");

    result = token.replace("*", "");

    return Pair.of(trailingAsteriskWildcard, result.length());
  }

  private static boolean notEnoughSymbols(final String token) {
    String trimmedToken = token.trim();
    Pair<Boolean, Integer> wildcard = checkTrailingAsterisk(trimmedToken);
    if (wildcard.getKey()) {
      return wildcard.getValue() < MIN_ALLOWED_SYMBOLS_TO_SEARCH;
    }

    return false;
  }
}
