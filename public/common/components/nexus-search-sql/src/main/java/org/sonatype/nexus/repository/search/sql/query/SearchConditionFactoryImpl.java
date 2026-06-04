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
package org.sonatype.nexus.repository.search.sql.query;

import org.sonatype.nexus.repository.search.sql.ExpressionGroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory that provides the appropriate {@link SearchConditionFactory} implementation based on the detected database
 * type.
 * This delegates to either H2SearchConditionFactory or PostgresSearchConditionFactory at runtime after database
 * configuration is loaded.
 */
@Component
@Primary
public class SearchConditionFactoryImpl
    implements SearchConditionFactory
{
  private final DatabaseTypeDetector databaseTypeDetector;

  private final ApplicationContext context;

  private SearchConditionFactory delegate;

  @Autowired
  public SearchConditionFactoryImpl(
      final DatabaseTypeDetector databaseTypeDetector,
      final ApplicationContext context)
  {
    this.databaseTypeDetector = checkNotNull(databaseTypeDetector);
    this.context = checkNotNull(context);
  }

  /**
   * Returns the appropriate SearchConditionFactory implementation based on detected database type.
   */
  private SearchConditionFactory getDelegate() {
    if (delegate == null) {
      delegate = BeanFactoryAnnotationUtils.qualifiedBeanOfType(context, SearchConditionFactory.class,
          databaseTypeDetector.isH2() ? "h2" : "postgres");
    }
    return delegate;
  }

  @Override
  public SqlSearchQueryConditionGroup build(final ExpressionGroup expressionGroup) {
    return getDelegate().build(expressionGroup);
  }
}
