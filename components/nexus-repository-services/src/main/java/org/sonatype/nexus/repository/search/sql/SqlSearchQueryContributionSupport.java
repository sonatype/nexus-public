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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.EQ;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.OR;

/**
 * Base implementation for {@link SqlSearchQueryContribution}
 *
 * @since 3.38
 */
public abstract class SqlSearchQueryContributionSupport
    extends SqlSearchValidationSupport
    implements SqlSearchQueryContribution
{
  private static final String QUOTE = "\"";

  protected SearchMappingService mappingService;

  @Inject
  public void init(final SearchMappingService mappingService) {
    this.mappingService = checkNotNull(mappingService);
  }

  @Override
  public Optional<Expression> createPredicate(@Nullable final SearchFilter filter) {
    Optional<SearchField> field = getField(filter);

    log.debug("Mapping for {} is {}", filter, field);

    if (filter == null || !field.isPresent()) {
      return Optional.empty();
    }

    boolean exact = isExact(filter);

    return Optional.ofNullable(filter)
        .map(SearchFilter::getValue)
        .filter(Objects::nonNull)
        .map(String::trim)
        .map(query -> split(query)
            .map(tokens -> tokenize(exact, tokens))
            .map(TermCollection::create)
            .map(terms -> new SqlPredicate(EQ, field.get(), terms))
            .collect(Collectors.toList())
        )
        .map(expressions -> SqlClause.create(OR, expressions));
  }

  /**
   * Split the query by spaces outside of quotes. i.e. {@code "nexus*core foo*" -> ["nexus*core", "foo*"]}
   *
   * @param value a search filter
   */
  protected Stream<String> split(final String value) {
    if (isBlank(value)) {
      return Stream.of("");
    }

    Set<String> tokens = new LinkedHashSet<>();
    char[] chars = value.toCharArray();

    StringBuilder token = new StringBuilder();
    boolean quoted = false;
    for (int i=0; i<chars.length; i++) {
      char c = chars[i];
      if (c == '\\') {
        token.append(c);
        if (i + 1 < chars.length) {
          token.append(chars[++i]);
        }
      }
      else if (c == '"') {
        if (quoted) {
          tokens.add(token.toString().trim());
          token = new StringBuilder();
          quoted = false;
        }
        else {
          quoted = true;
        }
      }
      else if (!quoted && c == ' ') {
        tokens.add(token.toString().trim());
        token = new StringBuilder();
      }
      else {
        token.append(c);
      }
    }

    if (Strings2.notBlank(token.toString())) {
      tokens.add(token.toString().trim());
    }

    return getValidTokens(tokens).stream();
  }

  /**
   * Tokenize terms query strings into StringTerm instances
   *
   * @param exact indicates whether the associated {@link SearchMapping} indicated exact matching
   * @param value a string from {@link #split} to tokenize
   */
  protected Collection<StringTerm> tokenize(final boolean exact, final String value) {
    if (isBlank(value)) {
      return Collections.singleton(new ExactTerm(""));
    }

    Set<StringTerm> tokens = new LinkedHashSet<>();
    char[] chars = value.toCharArray();

    StringBuilder token = new StringBuilder();
    boolean quoted = false;
    boolean terminated = false;
    boolean terminalWildcard = false;
    for (int i=0; i<chars.length; i++) {
      char c = chars[i];
      if (c == '\\') {
        if (i + 1 < chars.length) {
          token.append(chars[++i]);
        }
      }
      else if (c == '"') {
        if (quoted) {
          doCreateMatchTerm(exact, token).ifPresent(tokens::add);
          token = new StringBuilder();
          quoted = false;
        }
        else {
          quoted = true;
        }
      }
      else if (!quoted && (c == ' ' || c == '*' || c == '?')) {
        terminalWildcard |= c == '*' || c == '?';
        terminated = true;
      }
      else if (terminated == true) {
        create(exact, terminalWildcard, token.toString().trim()).ifPresent(tokens::add);

        terminalWildcard = terminated = false;
        token = new StringBuilder();
        token.append(c);
      }
      else {
        token.append(c);
      }
    }

    create(exact, terminalWildcard, token.toString().trim()).ifPresent(tokens::add);

    return tokens;
  }

  private Optional<StringTerm> create(final boolean exact, final boolean terminalWildcard, final String token) {
    if (!terminalWildcard) {
      return doCreateMatchTerm(exact, token);
    }

    return Optional.of(new WildcardTerm(token));
  }

  private Optional<StringTerm> doCreateMatchTerm(final boolean exact, final CharSequence value) {
    return Optional.of(value)
        .map(CharSequence::toString)
        .map(String::trim)
        .filter(Strings2::notBlank)
        .map(t -> createMatchTerm(exact, t));
  }

  /**
   * Create a {@link StringTerm} from the value, and the {@link SearchMapping}'s indication whether the filter should be
   * treated as exact.
   *
   * @param exact indicates whether the associated {@link SearchMapping} indicated exact matching
   * @param value the term
   */
  protected StringTerm createMatchTerm(final boolean exact, final String value) {
    return exact ? new ExactTerm(value) : new LenientTerm(value);
  }

  protected static String maybeTrimQuotes(String term) {
    term = removeEnd(term, QUOTE);
    term = removeStart(term, QUOTE);
    return term;
  }

  protected boolean isExact(@Nullable final SearchFilter filter) {
    return Optional.ofNullable(filter)
        .map(SearchFilter::getProperty)
        .map(mappingService::isExactMatch)
        .orElse(false);
  }

  protected Optional<SearchField> getField(@Nullable final SearchFilter filter) {
    return Optional.ofNullable(filter)
        .map(SearchFilter::getProperty)
        .flatMap(mappingService::getSearchField);
  }
}
