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

import java.io.StringReader;
import java.util.Map;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Strings;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
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
import org.apache.commons.jexl3.parser.Parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.6
 */
@Named
@Singleton
public class CselAssetSqlBuilder
    extends AbstractCselParserVisitor
{
  private static final String FORMAT_PLACEHOLDER = "${format}";

  private static final String PATH = "path";

  private static final int LEFT = 0;
  private static final int RIGHT = 1;

  // this stops JEXL from using expensive new Throwable().getStackTrace() to find caller info
  private static final JexlInfo CALLER_INFO = new JexlInfo(JexlSelector.class.getName(), 0, 0);

  /**
   * Build the where clause for a csel selector expression.
   *
   * @param expression a valid csel expression
   * @param format the format to test the expression against (maven2, etc)
   * @param parameterPrefix a unique prefix for parameterized values
   * @param fieldPrefix a prefix that will be prepended to fields in the query, i.e. $asset.  Can be an empty string
   *                    but cannot be null.
   * @return A CselAssetSql object containing parameterized sql and a map of the matching parameters
   */
  public CselAssetSql buildWhereClause(final String expression,
                                       final String format,
                                       final String parameterPrefix,
                                       final String fieldPrefix)
  {
    checkArgument(!Strings.isNullOrEmpty(expression));
    checkArgument(!Strings.isNullOrEmpty(format));
    checkArgument(!Strings.isNullOrEmpty(parameterPrefix));
    checkNotNull(fieldPrefix);

    Parser parser = new Parser(new StringReader(";"));
    ASTJexlScript parseTree = parser.parse(CALLER_INFO, expression, null, false, true);

    CselAssetSql cselAssetSql = (CselAssetSql) visit(parseTree, new CselAssetSql(parameterPrefix, fieldPrefix));
    StringBuilder whereClause = cselAssetSql.getSqlBuilder();

    for (int i = whereClause.indexOf(FORMAT_PLACEHOLDER); i >= 0; i = whereClause.indexOf(FORMAT_PLACEHOLDER)) {
      whereClause.replace(i, i + FORMAT_PLACEHOLDER.length(), format);
    }

    return cselAssetSql;
  }

  /**
   * Convert reference identifiers such as "component.groupId" into attribute names such as
   * "attributes.${format}.groupId"
   *
   * Note:
   * Calling code should do string replacement on ${format} to put the correct format in place (ie maven2)
   */
  @Override
  protected Object visit(final ASTReference node, final Object data) {
    CselAssetSql cselAssetSql = (CselAssetSql) data;
    StringBuilder result = cselAssetSql.getSqlBuilder();

    ASTIdentifierAccess childNode = (ASTIdentifierAccess) node.jjtGetChild(RIGHT);
    return result.append(cselAssetSql.getFieldPrefix()).append("attributes.").append(FORMAT_PLACEHOLDER).append(".")
        .append(childNode.getName());
  }

  /**
   * Convert `a || b` into `a or b`
   */
  @Override
  protected Object visit(final ASTOrNode node, final Object data) {
    return visitOperator(node, "or", data);
  }

  /**
   * Convert `a && b` into `a and b`
   */
  @Override
  protected Object visit(final ASTAndNode node, final Object data) {
    return visitOperator(node, "and", data);
  }

  /**
   * Convert `a == b` into `a = b`
   */
  @Override
  protected Object visit(final ASTEQNode node, final Object data) {
    return visitOperator(node, "=", data);
  }

  /**
   * Convert 'a != b' into 'a <> b'
   * @since 3.next
   */
  @Override
  protected Object visit(final ASTNENode node, final Object data) {
    return visitOperator(node, "<>", data);
  }

  /**
   * Convert `a =~ "regex"` into `a matches "regex"`
   */
  @Override
  protected Object visit(final ASTERNode node, final Object data) {
    return visitOperator(node, "matches", data);
  }

  /**
   * Convert `a =^ "something"` into `a like "something%"`
   */
  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    StringBuilder result = visitOperator(node, "like", data);

    CselAssetSql cselAssetSql = (CselAssetSql) data;
    String lastParameterName = cselAssetSql.getLastParameterName();

    if (lastParameterName != null) {
      Map<String,Object> parameters = cselAssetSql.getSqlParameters();
      Object parameter = parameters.get(lastParameterName);

      if (parameter instanceof String) {
        parameters.put(lastParameterName, parameters.get(lastParameterName) + "%");
        return result;
      }

      throw new JexlException(node, "Starts with expression must be assigned a string value");
    }

    throw new JexlException(node, "No parameters are defined");
  }

  /**
   * Convert field names
   */
  @Override
  protected Object visit(final ASTIdentifier node, final Object data) {
    CselAssetSql cselAssetSql = (CselAssetSql) data;
    StringBuilder result = cselAssetSql.getSqlBuilder();

    if (Objects.equals(PATH, node.getName())) { // Assets store the path in the name attribute
      result.append(cselAssetSql.getFieldPrefix()).append("name");
    }
    else if (Objects.equals("format", node.getName())) {
      result.append(cselAssetSql.getFieldPrefix()).append("format");
    }

    return result;
  }

  /**
   * Accept string literals of the form "abc"
   */
  @Override
  protected Object visit(final ASTStringLiteral node, final Object data) {
    return appendStringParameter((CselAssetSql) data, node.getLiteral());
  }

  private StringBuilder appendStringParameter(final CselAssetSql cselAssetSql, final String string) {
    String parameterName = cselAssetSql.getNextParameterName();

    cselAssetSql.getSqlParameters().put(parameterName, string);

    return cselAssetSql.getSqlBuilder().append(':' + parameterName);
  }

  /**
   * Accept valid parenthesized expressions `( expression )`
   */
  @Override
  protected Object visit(final ASTReferenceExpression node, final Object data) {
    CselAssetSql cselAssetSql = (CselAssetSql) data;
    StringBuilder result = cselAssetSql.getSqlBuilder();

    result.append('(');
    node.childrenAccept(this, data);
    result.append(')');

    return result;
  }

  private StringBuilder visitOperator(final JexlNode node, final String operator, final Object data) {
    CselAssetSql cselAssetSql = (CselAssetSql) data;
    StringBuilder result = cselAssetSql.getSqlBuilder();
    JexlNode leftChild = node.jjtGetChild(LEFT);
    JexlNode rightChild = node.jjtGetChild(RIGHT);

    if (isPathNode(leftChild, rightChild) || isPathNode(rightChild, leftChild)) {
      visitPath(cselAssetSql, leftChild, rightChild, operator);
    }
    else if (isNotEqualsOperator(operator)) {
      visitNotEqualsOperator(cselAssetSql, leftChild, rightChild, operator);
    }
    else {
      leftChild.jjtAccept(this, data);

      result.append(' ').append(operator).append(' ');

      rightChild.jjtAccept(this, data);
    }

    return result;
  }

  private void visitNotEqualsOperator(final CselAssetSql cselAssetSql, final JexlNode leftChild, final JexlNode rightChild, String operator) {
    StringBuilder result = cselAssetSql.getSqlBuilder();

    JexlNode varNode = rightChild;
    JexlNode stringNode = leftChild;
    if (isStringNode(rightChild)) {
      varNode = leftChild;
      stringNode = rightChild;
    }

    result.append("(");

    varNode.jjtAccept(this, cselAssetSql);

    //allow not equals check to allow null values as well
    result.append(" is null or ");

    varNode.jjtAccept(this, cselAssetSql);

    result.append(" ").append(operator).append(" ");

    stringNode.jjtAccept(this, cselAssetSql);

    result.append(")");
  }

  private Object visitPath(final CselAssetSql cselAssetSql,
                           final JexlNode left,
                           final JexlNode right,
                           final String operator)
  {
    StringBuilder result = cselAssetSql.getSqlBuilder();
    JexlNode identifier = left instanceof ASTIdentifier ? left : right;
    ASTStringLiteral string = (ASTStringLiteral) (right instanceof ASTStringLiteral ? right : left);

    identifier.jjtAccept(this, cselAssetSql);

    result.append(' ').append(operator).append(' ');

    String literal = string.getLiteral();
    if (literal.startsWith("/")) {
      appendStringParameter(cselAssetSql, literal.substring(1));
    }
    else {
      string.jjtAccept(this, cselAssetSql);
    }

    return result;
  }

  private boolean isPathNode(final JexlNode identifier, final JexlNode literal) {
    return identifier instanceof ASTIdentifier && literal instanceof ASTStringLiteral
        && PATH.equals(((ASTIdentifier) identifier).getName());
  }

  private boolean isStringNode(final JexlNode node) {
    return node instanceof ASTStringLiteral;
  }

  private boolean isNotEqualsOperator(final String operator) {
    return "<>".equals(operator);
  }
}
