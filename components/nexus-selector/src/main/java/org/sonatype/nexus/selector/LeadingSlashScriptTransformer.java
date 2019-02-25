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

import java.lang.reflect.Method;

import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Node;

/**
 * Walks the script, transforming path expressions with leading slashes so they match paths without leading slashes.
 *
 * @see LeadingSlashRegexTransformer
 *
 * @since 3.next
 */
class LeadingSlashScriptTransformer
    extends ParserVisitorSupport
{
  private static final LeadingSlashScriptTransformer INSTANCE = new LeadingSlashScriptTransformer();

  private static final String PATH = "path";

  private final Method setLiteralMethod = discoverSetLiteralMethod();

  /**
   * Finds leading slashes in the script and transforms them to handle paths that don't begin with a slash.
   *
   * @param script the JEXL script to transform
   * @return the transformed JEXL script
   */
  public static ASTJexlScript trimLeadingSlashes(final ASTJexlScript script) {
    if (INSTANCE.setLiteralMethod != null) {
      script.childrenAccept(INSTANCE, null);
    }
    return script;
  }

  private LeadingSlashScriptTransformer() {
    // utility class
  }

  @Override
  protected Object doVisit(final JexlNode node, final Object data) {

    if (node.jjtGetNumChildren() == 2) {
      Node leftChild = node.jjtGetChild(LEFT);
      Node rightChild = node.jjtGetChild(RIGHT);

      if (isPathExpression(leftChild, rightChild)) {
        transformPathLiteral((ASTStringLiteral) rightChild);
      }
      else if (isPathExpression(rightChild, leftChild)) {
        transformPathLiteral((ASTStringLiteral) leftChild);
      }
      else {
        leftChild.jjtAccept(this, data);
        rightChild.jjtAccept(this, data);
      }
    }
    else {
      node.childrenAccept(this, data);
    }

    return data;
  }

  private boolean isPathExpression(final Node identifier, final Node literal) {
    return identifier instanceof ASTIdentifier
        && PATH.equals(((ASTIdentifier) identifier).getName())
        && literal instanceof ASTStringLiteral;
  }

  private void transformPathLiteral(final ASTStringLiteral node) {
    String path = node.getLiteral();
    String transformedPath = LeadingSlashRegexTransformer.trimLeadingSlashes(path);
    if (!transformedPath.equals(path)) {
      try {
        setLiteralMethod.invoke(node, transformedPath);
      }
      catch (Exception | LinkageError e) {
        log.warn("Cannot replace leading slash in path selector {} with {}", path, transformedPath, e);
      }
    }
  }

  private Method discoverSetLiteralMethod() {
    Method _setLiteralMethod; // NOSONAR
    try {
      _setLiteralMethod = ASTStringLiteral.class.getDeclaredMethod("setLiteral", String.class);
      _setLiteralMethod.setAccessible(true);
    }
    catch (Exception | LinkageError e) {
      log.warn("Cannot modify ASTStringLiterals, leading slashes in path selectors won't be replaced", e);
      _setLiteralMethod = null;
    }
    return _setLiteralMethod;
  }
}
