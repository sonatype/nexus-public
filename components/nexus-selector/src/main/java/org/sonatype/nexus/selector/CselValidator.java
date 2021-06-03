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

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.JexlNode;

import static java.lang.String.format;

/**
 * Walks the script, checking whether it represents a valid CSEL expression.
 *
 * @since 3.16
 */
class CselValidator
    extends ParserVisitorSupport
{
  private static final CselValidator INSTANCE = new CselValidator();

  private static final Set<String> VALID_IDENTIFIERS = ImmutableSet.of("format", "path");

  private static final String EMBEDDED_STRING_MESSAGE = "String literal '%s' should not contain embedded string (\" or \')";

  private static final String BAD_IDENTIFIER_MESSAGE = "Invalid identifier %s, expected one of " + VALID_IDENTIFIERS;

  /**
   * Validates the given CSEL expression (in script form).
   *
   * @param script the CSEL script to validate
   */
  public static void validateCselExpression(final ASTJexlScript script) {
    script.childrenAccept(INSTANCE, null);
  }

  private CselValidator() {
    // utility class
  }

  @Override
  protected Object doVisit(final JexlNode node, final Object data) {
    throw new JexlException(node, "Expression not supported in CSEL selector");
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
   * Accept `a == b`
   */
  @Override
  protected Object visit(final ASTEQNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept `a != b`
   */
  @Override
  protected Object visit(final ASTNENode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept `a =~ "regex"`
   */
  @Override
  protected Object visit(final ASTERNode node, final Object data) {
    try {
      Pattern.compile(node.jjtGetChild(1).toString());
      return node.childrenAccept(this, data);
    }
    catch (PatternSyntaxException e) {
      throw new JexlException(node, e.getDescription());
    }
  }

  /**
   * Accept `a =^ "something"`
   */
  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept `( expression )`
   */
  @Override
  protected Object visit(final ASTReferenceExpression node, final Object data) {
    return node.childrenAccept(this, data);
  }

  /**
   * Accept string literals without embedded strings.
   */
  @Override
  protected Object visit(final ASTStringLiteral node, final Object data) {
    String literal = node.getLiteral();
    if (!literal.contains("\"") && !literal.contains("'")) {
      return node.childrenAccept(this, data);
    }
    else {
      throw new JexlException(node, format(EMBEDDED_STRING_MESSAGE, literal));
    }
  }

  /**
   * Accept white-listed identifiers.
   */
  @Override
  protected Object visit(final ASTIdentifier node, final Object data) {
    String id = node.getName();
    if (VALID_IDENTIFIERS.contains(id)) {
      return node.childrenAccept(this, data);
    }
    else {
      throw new JexlException(node, format(BAD_IDENTIFIER_MESSAGE, id));
    }
  }
}
