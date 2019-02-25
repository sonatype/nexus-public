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
package org.sonatype.nexus.selector;

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

/**
 * Walks the script, transforming CSEL expressions into SQL clauses.
 *
 * @since 3.next
 */
class CselToSql
    extends ParserVisitorSupport
{
  private static final CselToSql INSTANCE = new CselToSql();

  /**
   * Transforms the given CSEL expression (in script form) to SQL for use in a 'where' clause.
   *
   * @param script the CSEL script to transform
   * @param builder the SQL builder to use
   */
  public static void transformCselToSql(final ASTJexlScript script, final SelectorSqlBuilder builder) {
    script.childrenAccept(INSTANCE, builder);
  }

  private CselToSql() {
    // utility class
  }

  @Override
  protected Object doVisit(final JexlNode node, final Object data) {
    throw new JexlException(node, "Expression not supported in CSEL selector");
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
   * Transform `a == b` into `a = b`
   */
  @Override
  protected Object visit(final ASTEQNode node, final Object data) {
    return transformOperator(node, "=", (SelectorSqlBuilder) data);
  }

  /**
   * Transform `a =~ "regex"` into `a matches "regex"`
   */
  @Override
  protected Object visit(final ASTERNode node, final Object data) {
    return transformOperator(node, "matches", (SelectorSqlBuilder) data);
  }

  /**
   * Transform `a =^ "something"` into `a like "something%"`
   */
  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    if (rightChild instanceof ASTStringLiteral) {
      transformStartsWithOperator(leftChild, rightChild, (SelectorSqlBuilder) data);
    }
    else {
      transformStartsWithOperator(rightChild, leftChild, (SelectorSqlBuilder) data);
    }
    return data;
  }

  /**
   * Transform `a != b` into `(a is null or a <> b)`
   */
  @Override
  protected Object visit(final ASTNENode node, final Object data) {
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    if (rightChild instanceof ASTStringLiteral) {
      transformNotEqualsOperator(leftChild, rightChild, (SelectorSqlBuilder) data);
    }
    else {
      transformNotEqualsOperator(rightChild, leftChild, (SelectorSqlBuilder) data);
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
   * Store string literals as parameters.
   */
  @Override
  protected Object visit(final ASTStringLiteral node, final Object data) {
    ((SelectorSqlBuilder) data).appendLiteral(node.getLiteral());
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
   * Transform dotted references into format-specific attributes.
   */
  @Override
  protected Object visit(final ASTReference node, final Object data) {
    ASTIdentifierAccess subRef = (ASTIdentifierAccess) node.jjtGetChild(RIGHT);
    ((SelectorSqlBuilder) data).appendProperty(subRef.getName());
    return data;
  }

  private SelectorSqlBuilder transformOperator(final JexlNode node,
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

  private SelectorSqlBuilder transformStartsWithOperator(final JexlNode node,
                                                         final JexlNode literal,
                                                         final SelectorSqlBuilder builder)
  {
    if (!(literal instanceof ASTStringLiteral)) {
      throw new JexlException(node, "Expected string literal");
    }
    node.jjtAccept(this, builder);
    builder.appendOperator("like");
    builder.appendLiteral(((ASTStringLiteral) literal).getLiteral() + '%');
    return builder;
  }

  private SelectorSqlBuilder transformNotEqualsOperator(final JexlNode node,
                                                        final JexlNode literal,
                                                        final SelectorSqlBuilder builder)
  {
    if (!(literal instanceof ASTStringLiteral)) {
      throw new JexlException(node, "Expected string literal");
    }
    builder.appendExpression(() -> {
      node.jjtAccept(this, builder);
      builder.appendOperator("is null or");
      node.jjtAccept(this, builder);
      builder.appendOperator("<>");
      literal.jjtAccept(this, builder);
    });
    return builder;
  }
}
