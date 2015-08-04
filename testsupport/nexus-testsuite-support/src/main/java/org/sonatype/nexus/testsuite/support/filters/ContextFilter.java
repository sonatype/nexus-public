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
package org.sonatype.nexus.testsuite.support.filters;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.testsuite.support.Filter;

import com.google.common.collect.Maps;

/**
 * Replaces placeholders with values from filtering context.
 *
 * @since 2.2
 */
@Named
@Singleton
public class ContextFilter
    extends MapFilterSupport
    implements Filter
{

  /**
   * Mappings = context.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Ignored by this filter.
   * @return mappings = context
   */
  @Override
  Map<String, String> mappings(final Map<String, String> context, final String value) {
    return Maps.newHashMap(context);
  }

}
