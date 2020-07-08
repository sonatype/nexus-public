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
package org.sonatype.nexus.datastore;

import java.util.regex.Pattern;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.regex.Pattern.compile;

/**
 * Base class to collect common functions from multiple {@link DataStoreConfigurationSource} implementations.
 *
 * @since 3.25.0
 */
public abstract class DataStoreConfigurationSourceSupport
    extends ComponentSupport
    implements DataStoreConfigurationSource
{
  protected static final Pattern VALID_NAME_PATTERN = compile(NamePatternConstants.REGEX);

  /**
   * Checks the given store name is valid using the standard name regex.
   */
  protected static void checkName(final String storeName) {
    checkArgument(VALID_NAME_PATTERN.matcher(storeName).matches(), "%s is not a valid data store name", storeName);
  }
}
