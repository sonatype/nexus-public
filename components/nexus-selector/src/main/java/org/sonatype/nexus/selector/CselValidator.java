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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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

import static java.lang.String.format;

/**
 * Walks the script, checking whether it represents a valid CSEL expression.
 *
 * @since 3.next
 */
class CselValidator
    extends ParserVisitorSupport
{
  private static final CselValidator INSTANCE = new CselValidator();

  private static final Set<String> VALID_IDENTIFIERS = ImmutableSet.of("format", "path");

  private static final Map<String, Set<String>> VALID_REFERENCES = ImmutableMap.of(
      "coordinate", ImmutableSet.of("groupId", "artifactId", "version", "extension", "classifier", "id"));

  private static final String EMBEDDED_STRING_MESSAGE = "String literal '%s' should not contain embedded string (\" or \')";

  private static final String BAD_IDENTIFIER_MESSAGE = "Invalid identifier %s, expected one of " + VALID_IDENTIFIERS;

  private static final String BAD_REFERENCE_MESSAGE = "Invalid reference %s, expected one of %s";

  private static final String TOO_MANY_PARTS_MESSAGE = "Invalid reference, too many parts";

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
    return node.childrenAccept(this, data);
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

  /**
   * Accept white-listed dotted references.
   */
  @Override
  protected Object visit(final ASTReference node, final Object data) {
    if (node.jjtGetNumChildren() == 2) {
      String ref = ((ASTIdentifier) node.jjtGetChild(LEFT)).getName();
      Set<String> validSubRefs = VALID_REFERENCES.get(ref);
      if (validSubRefs != null) {
        String subRef = ((ASTIdentifierAccess) node.jjtGetChild(RIGHT)).getName();
        if (validSubRefs.contains(subRef)) {
          return data;
        }
        else {
          throw new JexlException(node, format(BAD_REFERENCE_MESSAGE, ref + '.' + subRef, ref + '.' + validSubRefs));
        }
      }
      else {
        throw new JexlException(node, format(BAD_REFERENCE_MESSAGE, ref, VALID_REFERENCES.keySet()));
      }
    }
    else {
      throw new JexlException(node, TOO_MANY_PARTS_MESSAGE);
    }
  }
}
