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
package org.sonatype.nexus.common.template;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.common.encoding.EncodingUtil;

import org.apache.commons.text.StringEscapeUtils;

import static java.util.stream.Collectors.joining;

/**
 * Helper to escape values.
 *
 * @since 3.0
 */
@TemplateAccessible
public class EscapeHelper
{

  private static final Map<String, String> DEFAULT_RULES;

  static {
    Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put("%", "%25");
    defaults.put(":", "%3A");
    defaults.put(" ", "%20");
    DEFAULT_RULES = Collections.unmodifiableMap(defaults);
  }

  /**
   * Returns a copy of the default URL escape rules.
   * Used for merging with repository-specific rules.
   *
   * @return copy of default rules map
   */
  public static Map<String, String> getDefaultRules() {
    return new LinkedHashMap<>(DEFAULT_RULES);
  }

  private final Map<String, String> transformRules;

  private final Pattern transformPattern; // compiled once, preserves rule order

  /**
   * Create an EscapeHelper with default URL escape rules.
   * Default rules: %→%25, :→%3A, space→%20
   */
  public EscapeHelper() {
    this.transformRules = DEFAULT_RULES;

    // Build alternation in *insertion order*; Pattern.quote each key
    String alternation = transformRules.keySet()
        .stream()
        .map(Pattern::quote)
        .collect(Collectors.joining("|"));
    this.transformPattern = Pattern.compile(alternation);
  }

  public EscapeHelper(final String urlEscapeRulesConfig) {
    this.transformRules = urlEscapeRulesConfig == null
        ? DEFAULT_RULES
        : UrlEscapeConfigParser.parseRules(urlEscapeRulesConfig);

    // Build alternation in *insertion order*; Pattern.quote each key
    if (transformRules.isEmpty()) {
      this.transformPattern = null; // means: no transform
    }
    else {
      String alternation = transformRules.keySet()
          .stream()
          .map(Pattern::quote)
          .collect(Collectors.joining("|"));
      this.transformPattern = Pattern.compile(alternation);
    }
  }

  /**
   * Create an EscapeHelper with custom rules map.
   *
   * This constructor is used for repository-specific URL escape rules.
   *
   * @param customRules map of pattern to replacement, or null/empty for no rules
   */
  public EscapeHelper(final Map<String, String> customRules) {
    this.transformRules = customRules != null && !customRules.isEmpty()
        ? new LinkedHashMap<>(customRules)
        : new LinkedHashMap<>();

    // Build alternation in *insertion order*; Pattern.quote each key
    if (transformRules.isEmpty()) {
      this.transformPattern = null; // means: no transform
    }
    else {
      String alternation = transformRules.keySet()
          .stream()
          .map(Pattern::quote)
          .collect(Collectors.joining("|"));
      this.transformPattern = Pattern.compile(alternation);
    }
  }

  public String html(final String value) {
    return StringEscapeUtils.escapeHtml4(value);
  }

  public String html(final Object value) {
    return html(String.valueOf(value));
  }

  public String url(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    else {
      return EncodingUtil.urlEncode(value);
    }
  }

  public String url(final Object value) {
    return url(String.valueOf(value));
  }

  public String xml(final String value) {
    return StringEscapeUtils.escapeXml10(value);
  }

  public String xml(final Object value) {
    return xml(String.valueOf(value));
  }

  public String uri(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    else {
      return url(value)
          .replaceAll("\\+", "%20")
          .replaceAll("\\%21", "!")
          .replaceAll("\\%27", "'")
          .replaceAll("\\%28", "(")
          .replaceAll("\\%29", ")")
          .replaceAll("\\%7E", "~");
    }
  }

  private String transform(final String value) {
    if (value == null || value.isEmpty() || transformPattern == null) {
      return value;
    }
    Matcher m = transformPattern.matcher(value);
    StringBuilder builder = new StringBuilder(value.length());
    while (m.find()) {
      String match = m.group();
      String replacement = transformRules.get(match);
      m.appendReplacement(builder, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(builder);
    return builder.toString();
  }

  public String uri(final Object value) {
    return uri(String.valueOf(value));
  }

  public String uriSegments(final String value) {
    return Stream.of(value.split("/")).map(this::transform).collect(joining("/"));
  }

  /**
   * Variant of {@link #uriSegments(String)} that preserves trailing empty segments so a value like
   * "simple/flask/" round-trips as "simple/flask/" — not "simple/flask". The default
   * {@code String.split("/")} drops trailing empties, which is fine for leaf-file URLs but wrong
   * for callers that resolve relative links against the resulting base URI. PEP 503 PyPI simple
   * indexes serve DIFFERENT link prefixes for "/simple/pkg/" (../../packages/...) vs "/simple/pkg"
   * (../packages/...), so silently dropping the trailing slash pulls back one-level-short links
   * that then resolve to a wrong upstream URL on the next fetch. Use only when the caller needs
   * exact byte-preservation of trailing slashes.
   */
  public String uriSegmentsPreserveTrailing(final String value) {
    return Stream.of(value.split("/", -1)).map(this::transform).collect(joining("/"));
  }

  /**
   * Strip java el start token from a string
   * 
   * @since 3.14
   */
  public String stripJavaEl(final String value) {
    if (value != null) {
      return value.replaceAll("\\$+\\{", "{").replaceAll("\\$+\\\\A\\{", "{");
    }
    return null;
  }
}
