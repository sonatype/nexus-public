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
package org.sonatype.nexus.repository.search.sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.nexus.rest.ValidationErrorsException;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates search tokens for SQL search queries.
 */
public final class SqlSearchWildcardValidator
{
  private static final Logger log = LoggerFactory.getLogger(SqlSearchWildcardValidator.class);

  private static final char ZERO_OR_MORE_CHARACTERS = '*';

  private static final char ANY_CHARACTER = '?';

  private static final int MIN_ALLOWED_SYMBOLS_TO_SEARCH = 3;

  private static final int MIN_ALLOWED_FOR_SPECIAL_CHAR_AND_WILDCARD = 2;

  private SqlSearchWildcardValidator() {
    // Utility class
  }

  /**
   * For SQL search we prohibit leading wildcards and less than 3 characters with wildcards for performance reasons.
   *
   * @param tokens the collection of tokens to validate
   * @return a new set containing only valid tokens
   * @throws ValidationErrorsException if all tokens are invalid
   */
  public static Set<String> getValidTokens(final Collection<String> tokens) {
    ValidationErrorsException validation = new ValidationErrorsException();
    Set<String> validTokens = new LinkedHashSet<>(tokens);

    Set<String> invalidTokens = tokens.stream()
        .filter(Objects::nonNull)
        .filter(SqlSearchWildcardValidator::hasLeadingWildcard)
        .collect(Collectors.toSet());
    if (!invalidTokens.isEmpty()) {
      String errorMsg = "Leading wildcards are prohibited";
      validation.withError(errorMsg);
      log.debug("{} for tokens: {}", errorMsg, invalidTokens);
      validTokens.removeAll(invalidTokens);
    }

    invalidTokens = tokens.stream()
        .filter(Objects::nonNull)
        .filter(SqlSearchWildcardValidator::hasLeadingSpecialCharacterAndWildcard)
        .collect(Collectors.toSet());
    if (!invalidTokens.isEmpty()) {
      String errorMsg = "Searches cannot begin with a special character followed by a wildcard";
      validation.withError(errorMsg);
      log.debug("{} for tokens: {}", errorMsg, invalidTokens);
      validTokens.removeAll(invalidTokens);
    }

    invalidTokens = tokens.stream()
        .filter(Objects::nonNull)
        .filter(SqlSearchWildcardValidator::notEnoughSymbols)
        .collect(Collectors.toSet());
    if (!invalidTokens.isEmpty()) {
      String errorMsg = String.format("%d characters or more are required with a trailing wildcard (*)",
          MIN_ALLOWED_SYMBOLS_TO_SEARCH);
      validation.withError(errorMsg);
      log.debug("{} for tokens: {}", errorMsg, invalidTokens);
      validTokens.removeAll(invalidTokens);
    }

    if (validTokens.isEmpty()) {
      log.debug("No valid search tokens");
      throw validation;
    }

    return validTokens;
  }

  /**
   * Validates a single token. It is a simplification of {@link #getValidTokens(Collection)} for the case when only one
   * token is expected.
   *
   * @param token the token to validate
   * @throws ValidationErrorsException if the token is invalid
   */
  public static void validateToken(final String token) {
    getValidTokens(Arrays.asList(token));
  }

  /**
   * Checks if a token contains wildcard characters (* or ?).
   *
   * @param token the token to check
   * @return true if the token contains wildcards, false otherwise
   */
  public static boolean containsWildcards(final String token) {
    return token.contains(String.valueOf(ZERO_OR_MORE_CHARACTERS)) || token.contains(String.valueOf(ANY_CHARACTER));
  }

  private static boolean hasLeadingWildcard(final String token) {
    String trimmedToken = token.trim();
    return !trimmedToken.isEmpty() && isWildcard(trimmedToken.charAt(0));
  }

  private static boolean hasLeadingSpecialCharacterAndWildcard(final String token) {
    String trimmedToken = token.trim();
    if (trimmedToken.length() < MIN_ALLOWED_FOR_SPECIAL_CHAR_AND_WILDCARD) {
      return false;
    }
    char firstChar = trimmedToken.charAt(0);
    return !(firstChar == '\\' || Character.isLetterOrDigit(firstChar)) && isWildcard(trimmedToken.charAt(1));
  }

  private static boolean isWildcard(final char character) {
    return character == ZERO_OR_MORE_CHARACTERS || character == ANY_CHARACTER;
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
