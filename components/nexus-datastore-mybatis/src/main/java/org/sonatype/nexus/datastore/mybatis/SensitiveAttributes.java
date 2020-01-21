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
package org.sonatype.nexus.datastore.mybatis;

import java.util.function.Predicate;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import org.apache.ibatis.session.Configuration;

import static java.util.regex.Pattern.compile;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * Filter sensitive attributes that require encryption at rest.
 *
 * By default this includes any attribute whose name contains 'password' or 'secret'. Additional attribute
 * names can be configured in {@code $install-dir/etc/fabric/config-store-mybatis.xml} as a comma-separated
 * list under the 'sensitiveAttributes' property, for example:
 *
 * <pre>
 * <property name="sensitiveAttributes" value="passcode,token"/>
 * </pre>
 *
 * @since 3.21
 */
final class SensitiveAttributes
{
  private static final String SENSITIVE_ATTRIBUTES_KEY = "sensitiveAttributes";

  private static final Predicate<String> SENSITIVE_FILTER = compile("(?i)(password|secret)").asPredicate();

  private SensitiveAttributes() {
    // static utility class
  }

  /**
   * Builds the filter that determines whether a JSON field is considered sensitive or not.
   */
  public static Predicate<String> buildSensitiveAttributeFilter(final Configuration config) {
    String additionalAttributes = config.getVariables().getProperty(SENSITIVE_ATTRIBUTES_KEY);

    if (isBlank(additionalAttributes)) {
      return SENSITIVE_FILTER;
    }

    Predicate<String> additionalFilter = ImmutableSet.copyOf(
        Splitter.on(',').trimResults().omitEmptyStrings().split(additionalAttributes))::contains;

    return SENSITIVE_FILTER.or(additionalFilter);
  }
}
