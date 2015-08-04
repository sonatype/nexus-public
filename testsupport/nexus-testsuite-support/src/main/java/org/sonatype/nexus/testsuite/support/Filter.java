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
package org.sonatype.nexus.testsuite.support;

import java.util.Map;

/**
 * Filter placeholders. For example could take as input "some.company:foo:${project.version}" and replaces the
 * placeholder "${project.version}" with the actual value "some.company:foo:1.0" by looking up the version from test
 * pom.
 *
 * @since 2.2
 */
public interface Filter
{

  /**
   * Filters placeholders.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Cannot be null.
   * @return filtered value. If null the filter is not considered in filtering chain.
   */
  String filter(final Map<String, String> context, String value);

}
