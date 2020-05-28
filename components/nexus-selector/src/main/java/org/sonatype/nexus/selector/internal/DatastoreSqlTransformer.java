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
package org.sonatype.nexus.selector.internal;

import org.sonatype.nexus.selector.ParserVisitorSupport;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Base class to transform CSEL operators to SQL
 *
 * @since 3.24
 */
public abstract class DatastoreSqlTransformer
    extends ParserVisitorSupport
{
  @Override
  protected Object doVisit(final JexlNode node, final Object data) {
    throw new JexlException(node, "Expression not supported in CSEL selector, failing node is " + node.jexlInfo().toString());
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
   * Transform `a =^ "something"` into `a like "something%"`
   */
  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);
    if (rightChild instanceof ASTStringLiteral) {
      transformStartsWithOperator(leftChild, (ASTStringLiteral) rightChild, (SelectorSqlBuilder) data);
    }
    else if (leftChild instanceof ASTStringLiteral) {
      transformStartsWithOperator(rightChild, (ASTStringLiteral) leftChild, (SelectorSqlBuilder) data);
    }
    else {
      throw new JexlException(node, "Expected string literal");
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
      transformNotEqualsOperator(leftChild, (ASTStringLiteral) rightChild, (SelectorSqlBuilder) data);
    }
    else if (leftChild instanceof ASTStringLiteral) {
      transformNotEqualsOperator(rightChild, (ASTStringLiteral) leftChild, (SelectorSqlBuilder) data);
    }
    else {
      throw new JexlException(node, "Expected string literal");
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
      final SelectorSqlBuilder builder)
  {
    node.jjtAccept(this, builder);
    builder.appendOperator("like");
    builder.appendLiteral(literal.getLiteral() + '%');
    return builder;
  }

  private SelectorSqlBuilder transformNotEqualsOperator(
      final JexlNode node,
      final ASTStringLiteral literal,
      final SelectorSqlBuilder builder)
  {
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
