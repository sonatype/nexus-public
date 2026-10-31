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
package org.sonatype.nexus.repository.proxy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for repository-specific URL escape rules configuration.
 *
 * <p>
 * This parser provides enhanced security and repository-specific configuration
 * compared to the simpler {@code UrlEscapeConfigParser}. Use this parser for
 * per-repository URL encoding rules.
 * </p>
 *
 * <p>
 * Format: repositoryName={pattern:replacement,pattern:replacement}
 * Multiple repositories are separated by commas.
 * Empty rules {} means use DEFAULT_RULES only (no additional custom rules).
 * </p>
 *
 * <p>
 * Example nexus.properties configuration:
 * </p>
 *
 * <pre>
 * nexus.proxy.url.escape.rules=pypi-proxy={+:%2B,#:%23},maven-central={+:%2B},npm-registry={}
 * </pre>
 *
 * <p>
 * Behavior:
 * </p>
 * <ul>
 * <li>Repository not in config → uses DEFAULT_RULES (%→%25, :→%3A, space→%20)</li>
 * <li>Repository with {} → uses DEFAULT_RULES only (no additional rules)</li>
 * <li>Repository with custom rules → merges with DEFAULT_RULES (custom overrides)</li>
 * </ul>
 *
 * <h2>Security Features:</h2>
 * <ul>
 * <li>Configuration is typically set by administrators via nexus.properties</li>
 * <li>Repository names are escaped with Pattern.quote() to prevent regex injection</li>
 * <li>Special characters in patterns/replacements should follow URL percent-encoding rules</li>
 * <li>Configuration string is parsed safely with input validation and error handling</li>
 * <li>Maximum config length: 10KB to prevent DoS</li>
 * <li>Maximum rules per repository: 50 to prevent memory exhaustion</li>
 * <li>Null byte and control character detection/rejection</li>
 * </ul>
 *
 * <h2>Difference from UrlEscapeConfigParser:</h2>
 * <ul>
 * <li>UrlEscapeConfigParser: Simple format for global configuration (500 char limit, 20 rules)</li>
 * <li>This parser: Repository-specific format with enhanced security (10KB limit, 50 rules)</li>
 * </ul>
 *
 * @see EscapeHelper
 */
public class UrlEscapeRulesParser
{
  private static final Logger log = LoggerFactory.getLogger(UrlEscapeRulesParser.class);

  /** Maximum length of configuration string to prevent DoS attacks */
  private static final int MAX_CONFIG_LENGTH = 10240; // 10KB

  /** Maximum number of rules per repository to prevent memory exhaustion */
  private static final int MAX_RULES_PER_REPO = 50;

  /** Pattern to validate a valid replacement string (whole string match) */
  private static final Pattern VALID_REPLACEMENT = Pattern.compile("([^%]|%[0-9A-Fa-f]{2})*");

  /** Cache of compiled patterns by repository name for performance */
  private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

  private UrlEscapeRulesParser() {
    // Utility class
  }

  /**
   * Parse repository-specific URL escape rules from configuration.
   *
   * @param config the configuration string
   * @param repositoryName the repository name to look up
   * @return map of pattern to replacement, or null if repository not found in config
   */
  public static Map<String, String> parseRepositoryRules(final String config, final String repositoryName) {
    if (config == null || config.trim().isEmpty()) {
      return null;
    }

    // Input validation: prevent DoS via huge config strings
    if (config.length() > MAX_CONFIG_LENGTH) {
      log.warn("URL escape rules config exceeds maximum length ({} > {}), ignoring",
          config.length(), MAX_CONFIG_LENGTH);
      return null;
    }

    if (repositoryName == null || repositoryName.trim().isEmpty()) {
      log.warn("Repository name is null or empty, cannot parse repository-specific rules");
      return null;
    }

    // Parse format: repoName={rules},otherRepo={rules}
    // Use cached pattern for performance
    try {
      Pattern pattern = PATTERN_CACHE.computeIfAbsent(repositoryName,
          name -> Pattern.compile(Pattern.quote(name) + "=\\{([^}]*)\\}"));
      Matcher matcher = pattern.matcher(config);

      if (matcher.find()) {
        String rules = matcher.group(1);
        log.debug("Found repository-specific URL escape rules for {}", repositoryName);
        return parseRules(rules, repositoryName);
      }

      log.debug("No repository-specific URL escape rules found for {}", repositoryName);
      return null;
    }
    catch (Exception e) {
      log.warn("Failed to parse repository-specific URL escape rules for {}: {}",
          repositoryName, e.getMessage());
      return null;
    }
  }

  /**
   * Parse individual rules: pattern:replacement,pattern:replacement
   *
   * @param rules the rules string
   * @param repositoryName repository name for logging
   * @return map of pattern to replacement
   */
  private static Map<String, String> parseRules(final String rules, final String repositoryName) {
    if (rules == null || rules.trim().isEmpty()) {
      return Collections.emptyMap(); // Empty rules = disabled
    }

    Map<String, String> result = new LinkedHashMap<>();
    String[] pairs = rules.split(",");

    for (String pair : pairs) {
      // Check rule count limit
      if (result.size() >= MAX_RULES_PER_REPO) {
        log.warn("Maximum rules limit ({}) reached for repository {}, ignoring remaining rules",
            MAX_RULES_PER_REPO, repositoryName);
        break;
      }

      // Split by colon first
      String[] parts = pair.split(":", 2);
      if (parts.length == 2) {
        String rawPattern = parts[0];
        String replacement = parts[1].trim();

        // Smart pattern parsing:
        // - If pattern has non-whitespace chars, trim both leading and trailing whitespace (formatting)
        // - If pattern is ONLY whitespace, preserve it as the actual pattern (e.g., space character)
        String pattern;
        if (!rawPattern.trim().isEmpty()) {
          // Contains non-whitespace, so whitespace is just formatting
          pattern = rawPattern.trim();
        }
        else if (!rawPattern.isEmpty()) {
          // Only whitespace, so it IS the pattern (e.g., space character)
          pattern = rawPattern;
        }
        else {
          // Empty pattern
          pattern = rawPattern;
        }

        if (!pattern.isEmpty() && !replacement.isEmpty()) {
          // Validate replacement format (should be valid percent-encoding or plain text)
          if (!isValidReplacement(replacement)) {
            log.warn("Skipping invalid replacement format for repository {}: '{}'. " +
                "Use valid percent-encoding (e.g., %2B) or plain text.", repositoryName, replacement);
            continue;
          }

          result.put(pattern, replacement);
          log.debug("Parsed URL escape rule: '{}' -> '{}'", pattern, replacement);
        }
      }
      else if (parts.length == 1 && !parts[0].trim().isEmpty()) {
        log.warn("Skipping malformed URL escape rule: '{}'. Expected 'pattern:replacement'", pair.trim());
      }
    }

    return result;
  }

  /**
   * Validate that a replacement string is safe and well-formed.
   * Accepts valid percent-encoding (e.g., %2B) or plain text without control characters.
   *
   * @param replacement the replacement string to validate
   * @return true if valid, false otherwise
   */
  private static boolean isValidReplacement(final String replacement) {
    if (replacement == null || replacement.isEmpty()) {
      return false;
    }

    // Check for null bytes or control characters (security risk)
    for (char c : replacement.toCharArray()) {
      if (c == '\0' || (c < 32 && c != '\t' && c != '\n' && c != '\r')) {
        return false;
      }
    }

    // If contains percent sign, validate percent-encoding format
    if (replacement.contains("%")) {
      // Must be valid percent-encoding (e.g., %2B) or mix of plain text and percent-encoding
      // Standalone % without hex digits is invalid
      return VALID_REPLACEMENT.matcher(replacement).matches();
    }

    return true;
  }

  /**
   * Clear the pattern cache. Used for testing.
   */
  static void clearCache() {
    PATTERN_CACHE.clear();
  }
}
