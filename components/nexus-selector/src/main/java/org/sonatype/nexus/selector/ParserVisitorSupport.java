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

import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEmptyMethod;
import org.apache.commons.jexl3.parser.ASTExtendedLiteral;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTSizeMethod;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParserVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common scaffolding for partial {@link ParserVisitor} implementations.
 *
 * @since 3.next
 */
abstract class ParserVisitorSupport
    extends ParserVisitor
{
  protected static final int LEFT = 0;

  protected static final int RIGHT = 1;

  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  protected Object visit(final ASTJexlScript node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTBlock node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTIfStatement node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTWhileStatement node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTContinue node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTBreak node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTForeachStatement node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTReturnStatement node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTAssignment node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTVar node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTReference node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTTernaryNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTOrNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTAndNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTBitwiseOrNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTBitwiseXorNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTBitwiseAndNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTEQNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNENode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTLTNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTGTNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTLENode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTGENode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTERNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNRNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSWNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNSWNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTEWNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNEWNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTAddNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSubNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTMulNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTDivNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTModNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTUnaryMinusNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTBitwiseComplNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNotNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTIdentifier node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNullLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTTrueNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTFalseNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTNumberLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTStringLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTExtendedLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTArrayLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTRangeNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTMapLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTMapEntry node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTEmptyFunction node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTEmptyMethod node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSizeFunction node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTFunctionNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTMethodNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSizeMethod node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTConstructorNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTArrayAccess node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTIdentifierAccess node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTArguments node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTReferenceExpression node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetAddNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetSubNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetMultNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetDivNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetModNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetAndNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetOrNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTSetXorNode node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTJxltLiteral node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTAnnotation node, final Object data) {
    return doVisit(node, data);
  }

  @Override
  protected Object visit(final ASTAnnotatedStatement node, final Object data) {
    return doVisit(node, data);
  }

  protected abstract Object doVisit(final JexlNode node, final Object data);
}
