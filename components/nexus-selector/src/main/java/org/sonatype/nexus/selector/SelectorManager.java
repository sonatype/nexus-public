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

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.entity.EntityId;

/**
 * Manages content selectors.
 *
 * since 3.1
 */
public interface SelectorManager
    extends Lifecycle
{
  /**
   * Return all existing content selectors.
   */
  List<SelectorConfiguration> browse();

  /**
   * Return all JEXL content selectors.
   *
   * @since 3.6
   */
  default List<SelectorConfiguration> browseJexl() {
    return browse(JexlSelector.TYPE);
  }

  /**
   * Return all content selectors of the given type.
   *
   * @since 3.next
   */
  List<SelectorConfiguration> browse(String selectorType);

  /**
   * @return the content selectors for the active user
   *
   * @since 3.6
   */
  List<SelectorConfiguration> browseActive(List<String> repositoryNames, List<String> formats);

  /**
   * Read content selector by id.
   */
  @Nullable
  SelectorConfiguration read(EntityId entityId);

  /**
   * Persist a new selector configuration.
   */
  void create(SelectorConfiguration configuration);

  /**
   * Persist an existing selector configuration.
   */
  void update(SelectorConfiguration configuration);

  /**
   * Delete an existing selector configuration.
   */
  void delete(SelectorConfiguration configuration);

  /**
   * Evaluate the specified content selector against the given variable source.
   *
   * @throws SelectorEvaluationException if the given selector cannot be evaluated
   */
  boolean evaluate(SelectorConfiguration selectorConfiguration, VariableSource variableSource)
      throws SelectorEvaluationException;

  /**
   * Convert the specified content selector to SQL for use as a 'where' clause.
   *
   * @throws SelectorEvaluationException if the given selector cannot be converted
   *
   * @since 3.next
   */
  void toSql(SelectorConfiguration selectorConfiguration, SelectorSqlBuilder sqlBuilder)
      throws SelectorEvaluationException;
}
