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
package org.sonatype.nexus.repository.search.sql.query.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Used with {@link CselToExpression} to convert a CSEL content selector into an {@link Expression} for SQL based
 * searches
 */
public class SelectorExpressionBuilder
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private List<Object> elements = new ArrayList<>();

  private final SearchMappingService mappingService;

  private final Map<String, SearchField> propertyAliases = new HashMap<>();

  public SelectorExpressionBuilder(final SearchMappingService mappingService) {
    this.mappingService = checkNotNull(mappingService);
  }

  /**
   * Aliases the given property name to a specific record field.
   */
  public SelectorExpressionBuilder propertyAlias(final String name, final SearchField field) {
    propertyAliases.put(checkNotNull(name), checkNotNull(field));
    return this;
  }

  public SelectorExpressionBuilder appendOperand(final Operand op) {
    elements.add(op);
    return this;
  }

  public SelectorExpressionBuilder appendField(final String fieldName) {
    elements.add(lookup(fieldName));
    return this;
  }

  public SelectorExpressionBuilder appendTerm(final Term term) {
    elements.add(term);
    return this;
  }

  /**
   * Used to append a nested expression.
   *
   * @param runnable a runnable which will be invoked when the builder is ready to accept calls for the nested
   *          expression
   */
  public SelectorExpressionBuilder appendExpression(final Runnable runnable) {
    List<Object> temp = elements;
    elements = new ArrayList<>();

    runnable.run();

    Expression exp = build();

    elements = temp;
    elements.add(exp);

    return this;
  }

  public Expression build() {
    try {
      // First build predicates, e.g. foo = 'bar'
      List<Object> compiled = createPredicates(elements);
      // Now combine the AND clauses as these are higher priorities than OR
      compiled = createClauses(compiled, Operand.AND);
      // Finally combine the OR clauses
      compiled = createClauses(compiled, Operand.OR);

      checkState(compiled.size() == 1, compiled);

      return (Expression) compiled.get(0);
    }
    catch (RuntimeException e) {
      throw new RuntimeException("Unable to create expression for: " + elements, e);
    }
  }

  private static List<Object> createPredicates(final List<Object> elements) {
    List<Object> compiled = new ArrayList<>();
    List<Object> working = new ArrayList<>();
    for (Object element : elements) {
      working.add(element);

      if (working.size() == 3) {
        if (working.get(1) instanceof Operand) {
          Object operand = working.get(1);
          if (operand != Operand.AND && operand != Operand.OR) {
            compiled.add(new SqlPredicate((Operand) operand, (SearchField) working.get(0), (Term) working.get(2)));
            working.clear();
            continue;
          }
        }
        compiled.add(working.remove(0));
      }
    }
    compiled.addAll(working);
    return compiled;
  }

  private static List<Object> createClauses(final List<Object> elements, final Operand operand) {
    if (elements.size() == 1) {
      return elements;
    }

    List<Object> compiled = new ArrayList<>();
    List<Object> working = new ArrayList<>();

    Operand[] current = new Operand[1];

    Runnable createClause = () -> compiled.add(SqlClause.create(operand, working.stream()
        .map(Expression.class::cast)
        .collect(Collectors.toList())));

    Runnable consume = () -> {
      for (Object o : working) {
        compiled.add(o);
        compiled.add(current[0]);
      }
      compiled.remove(compiled.size() - 1);
    };

    for (Object element : elements) {
      if (element instanceof Operand) {
        boolean changed = current[0] != null && current[0] != element;
        if (changed && current[0] == operand) {
          // The operand has changed, so we need to create a clause for what we've already collected
          createClause.run();
          compiled.add(element);
          working.clear();
        }
        else if (changed) {
          // Emit all items except the last with the old operator between them.
          // The last item seeds the new operator group (operator precedence: AND > OR).
          for (int i = 0; i < working.size() - 1; i++) {
            compiled.add(working.get(i));
            compiled.add(current[0]);
          }
          Object carry = working.get(working.size() - 1);
          working.clear();
          working.add(carry);
        }
        current[0] = (Operand) element;
      }
      else {
        working.add(element);
      }
    }

    if (!working.isEmpty()) {
      if (!compiled.isEmpty() && !(compiled.get(compiled.size() - 1) instanceof Operand)) {
        compiled.add(current[0]);
      }
      if (current[0] == operand) {
        createClause.run();
      }
      else {
        consume.run();
      }
    }

    return compiled;
  }

  private SearchField lookup(final String fieldName) {
    Optional<SearchField> field = mappingService.getSearchField(fieldName);

    if (field.isPresent()) {
      return field.get();
    }

    return Optional.ofNullable(propertyAliases.get(fieldName))
        .orElseThrow(() -> new IllegalArgumentException("Unknown field name: " + fieldName));
  }
}
