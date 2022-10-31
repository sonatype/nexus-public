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
package org.sonatype.nexus.repository.content.search.table.internal;

import org.sonatype.nexus.repository.search.table.SelectorTsQuerySqlBuilder;
import org.sonatype.nexus.selector.CselToSql;
import org.sonatype.nexus.selector.ParserVisitorSupport;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.JexlNode;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.sonatype.nexus.repository.search.table.TableSearchContentSelectorSqlFilterGenerator.PATHS;

/**
 * Walks the script,transforming CSEL expressions into Postgres Full Text Search queries.
 */
//Don't inject
public class CselToTsQuerySql
    extends ParserVisitorSupport
    implements CselToSql
{
  private static final String EXPECTED_STRING_LITERAL = "Expected string literal";

  private static final String TSQUERY_MATCH_OPERATOR = "@@";

  private static final String TSQUERY_NEGATION_OPERATOR = "@@ !!";

  private static final String PREFIX_OPERATOR = ":*";

  public static final String TOKEN_END_REGEX = "(}|$)";

  public static final String TOKEN_START_REGEX = "(^|{)";

  @Override
  public void transformCselToSql(final ASTJexlScript script, final SelectorSqlBuilder builder) {
    script.childrenAccept(this, builder);
  }

  @Override
  protected Object doVisit(final JexlNode node, final Object data) {
    throw new JexlException(node,
        "Expression not supported in CSEL selector, failing node is " + node.jexlInfo().toString());
  }

  /**
   * Transform `a || b` into `a or b`
   */
  @Override
  protected Object visit(final ASTOrNode node, final Object data) {
    return transformOperator(node, "or", (SelectorSqlBuilder) data);
  }

  /**
   * Transform `a && b` into `a and b`
   */
  @Override
  protected Object visit(final ASTAndNode node, final Object data) {
    return transformOperator(node, "and", (SelectorSqlBuilder) data);
  }

  /**
   * Transform `a == b` into `a @@ TO_TSQUERY('simple', b)`
   */
  @Override
  protected Object visit(final ASTEQNode node, final Object data) {
    SelectorSqlBuilder builder = (SelectorSqlBuilder) data;
    transformOperator(node, TSQUERY_MATCH_OPERATOR, builder);
    return builder;
  }

  /**
   * Transform `a =~ b` into `a ~ b`
   */
  @Override
  protected Object visit(final ASTERNode node, final Object data) {
    return transformMatchesOperator(node, (SelectorTsQuerySqlBuilder) data);
  }

  /**
   * Transform `a != b` into `(a is null or a @@ !!to_tsquery('simple', b)`
   */
  @Override
  protected Object visit(final ASTNENode node, final Object data) {
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    if (rightChild instanceof ASTStringLiteral) {
      transformNotEqualsOperator(leftChild, (ASTStringLiteral) rightChild, (SelectorTsQuerySqlBuilder) data);
    }
    else if (leftChild instanceof ASTStringLiteral) {
      transformNotEqualsOperator(rightChild, (ASTStringLiteral) leftChild, (SelectorTsQuerySqlBuilder) data);
    }
    else {
      throw new JexlException(node, EXPECTED_STRING_LITERAL);
    }
    return data;
  }

  /**
   * Transform `a =^ "something"` into `a @@ TO_TSQUERY('simple', something:*)"`
   */
  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    SelectorTsQuerySqlBuilder builder = (SelectorTsQuerySqlBuilder) data;
    if (rightChild instanceof ASTStringLiteral) {
      transformStartsWithOperator(leftChild, (ASTStringLiteral) rightChild, builder);
    }
    else if (leftChild instanceof ASTStringLiteral) {
      transformStartsWithOperator(rightChild, (ASTStringLiteral) leftChild, builder);
    }
    else {
      throw new JexlException(node, EXPECTED_STRING_LITERAL);
    }
    return data;
  }

  /**
   * Apply `( expression )`
   */
  @Override
  protected Object visit(final ASTReferenceExpression node, final Object data) {
    ((SelectorSqlBuilder) data).appendExpression(() -> node.childrenAccept(this, data));
    return data;
  }

  /**
   * Transform identifiers into asset fields.
   */
  @Override
  protected Object visit(final ASTIdentifier node, final Object data) {
    ((SelectorSqlBuilder) data).appendProperty(node.getName());
    return data;
  }

  /**
   * Store string literals as parameters and specify as <code>to_tsquery()</code> argument.
   */
  @Override
  protected Object visit(final ASTStringLiteral node, final Object data) {
    ((SelectorTsQuerySqlBuilder) data).appendTsQueryFunction(node.getLiteral());
    return data;
  }

  /**
   * Transform dotted references into format-specific attributes.
   */
  @Override
  protected Object visit(final ASTReference node, final Object data) {
    ASTIdentifierAccess subRef = (ASTIdentifierAccess) node.jjtGetChild(RIGHT);
    ((SelectorSqlBuilder) data).appendProperty(subRef.getName());
    return data;
  }

  protected SelectorSqlBuilder transformOperator(
      final JexlNode node,
      final String operator,
      final SelectorSqlBuilder builder)
  {
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    leftChild.jjtAccept(this, builder);
    builder.appendOperator(operator);
    rightChild.jjtAccept(this, builder);
    return builder;
  }

  private SelectorSqlBuilder transformStartsWithOperator(
      final JexlNode node,
      final ASTStringLiteral literal,
      final SelectorTsQuerySqlBuilder builder)
  {
    node.jjtAccept(this, builder);
    builder.appendOperator(TSQUERY_MATCH_OPERATOR);
    builder.appendTsQueryFunction(literal.getLiteral() + PREFIX_OPERATOR);
    return builder;
  }

  /**
   * Builds the regex expression for a regex selector.
   * The database column that it checks is a VARCHAR containing one or more paths
   * where each path is surrounded with a '{}'. The paths are space delimited.
   *
   * Therefore, to simulate matching a full path within the VARCHAR (e.g. {/foo/bar} ) within the VARCHAR,
   * this method uses '(^|{})' to represent '^' and '(}|$)' to represent '$'
   */
  protected SelectorSqlBuilder transformMatchesOperator(
      final JexlNode node,
      final SelectorTsQuerySqlBuilder builder)
  {
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    builder.appendProperty(PATHS);

    builder.appendOperator("~");
    if (rightChild instanceof ASTStringLiteral) {
      String pattern = ((ASTStringLiteral) rightChild).getLiteral();

      if (pattern.charAt(0) != '^') {
        if (pattern.charAt(pattern.length() - 1) == '$') {
          pattern = removeEnd(pattern, "$");
        }
        pattern = String.format(TOKEN_START_REGEX + "(%s)" + TOKEN_END_REGEX, pattern); // match entire string
      }
      else {
        pattern = removeStart(pattern, "^");
        pattern = TOKEN_START_REGEX + pattern;
        if (pattern.charAt(pattern.length() - 1) == '$') {
          pattern = removeEnd(pattern, "$");
          pattern = pattern + TOKEN_END_REGEX;
        }
      }

      builder.appendLiteral(pattern);
    }
    else {
      throw new JexlException(node, EXPECTED_STRING_LITERAL);
    }
    return builder;
  }

  private SelectorSqlBuilder transformNotEqualsOperator(
      final JexlNode node,
      final ASTStringLiteral literal,
      final SelectorTsQuerySqlBuilder builder)
  {
    builder.appendExpression(() -> {
      node.jjtAccept(this, builder);
      builder.appendOperator("is null or");
      node.jjtAccept(this, builder);
      builder.appendOperator(TSQUERY_NEGATION_OPERATOR);
      literal.jjtAccept(this, builder);
    });
    return builder;
  }
}
