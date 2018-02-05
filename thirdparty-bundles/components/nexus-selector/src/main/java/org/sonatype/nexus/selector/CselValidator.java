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
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.Parser;

import static java.util.Arrays.asList;

/**
 * @since 3.6
 */
@Named
@Singleton
public class CselValidator
    extends AbstractCselParserVisitor
{
  // this stops JEXL from using expensive new Throwable().getStackTrace() to find caller info
  private static final JexlInfo CALLER_INFO = new JexlInfo(JexlSelector.class.getName(), 0, 0);

  private final Parser parser = new Parser(new StringReader(";"));

  public boolean validate(final String expression) {
    ASTJexlScript parseTree = parser.parse(CALLER_INFO, expression, null, false, true);

    Boolean result = true;

    return (boolean) visit(parseTree, result);
  }

  /**
   * Accept valid reference identifiers such as "coordinate.groupId"
   */
  @Override
  protected Object visit(final ASTReference node, final Object data) {
    List<String> parentNames = asList("coordinate");
    List<String> childNames = asList("groupId", "artifactId", "version", "extension", "classifier", "id");
    if (node.jjtGetNumChildren() == 2) {
      ASTIdentifier parentNode = (ASTIdentifier) node.jjtGetChild(0);
      ASTIdentifierAccess childNode = (ASTIdentifierAccess) node.jjtGetChild(1);

      if (parentNames.contains(parentNode.getName()) && childNames.contains(childNode.getName())) {
        return data;
      }
      throw new JexlException(node,
          "'Invalid identifier=" + parentNode.getName() + '.' + childNode.getName() + ", expected one of " + parentNames
              + '.' + childNames + "'");
    }
    throw new JexlException(node, "'Invalid reference - too long'");
  }

  /**
   * Accept `a || b`
   */
  @Override
  protected Object visit(final ASTOrNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept `a && b`
   */
  @Override
  protected Object visit(final ASTAndNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept equals nodes of the form `a == b`
   */
  @Override
  protected Object visit(final ASTEQNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept equals regular expression nodes of the form `a =~ "regex"`
   */
  @Override
  protected Object visit(final ASTERNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept "Starts With" nodes of the form `a =^ "something"`
   */
  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept identifiers for specific fields
   */
  @Override
  protected Object visit(final ASTIdentifier node, final Object data) {
    List<String> validNames = asList("format", "path");
    if (validNames.contains(node.getName())) {
      return node.childrenAccept(this, data);
    }
    else {
      throw new JexlException(node, "'Invalid identifier=" + node.getName() + ", expected one of " + validNames + "'");
    }
  }

  /**
   * Accept string literals of the form "abc"
   */
  @Override
  protected Object visit(final ASTStringLiteral node, final Object data) {
    String literal = node.getLiteral();

    if (literal.contains("\"") || literal.contains("'")) {
      throw new UnsupportedOperationException("String literal " + literal + " should not contain embedded strings (\" or \')");
    }

    return node.childrenAccept(this, data);
  }

  /**
   * Accept valid parenthesized expressions `( expression )`
   */
  @Override
  protected Object visit(final ASTReferenceExpression node, final Object data) {
    return node.childrenAccept(this, data);
  }
}
