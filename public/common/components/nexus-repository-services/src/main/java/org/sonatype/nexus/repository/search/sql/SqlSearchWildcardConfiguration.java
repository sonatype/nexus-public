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
package org.sonatype.nexus.repository.search.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring-managed configuration for SQL search wildcard validation.
 * Reads the minimum prefix length from nexus.properties and configures the validator.
 *
 * <p>
 * This class is the single authority for validation of the prefix length configuration.
 * The validated value is then passed to {@link SqlSearchWildcardValidator}.
 */
@Component
public class SqlSearchWildcardConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(SqlSearchWildcardConfiguration.class);

  private static final int DEFAULT_MIN_ALLOWED = 3;

  /**
   * Constructs the configuration by reading the value from nexus.properties
   * and immediately applying it to the validator.
   *
   * <p>
   * This constructor validates the configuration value and applies it to the validator.
   * Values must be between 1 and 100. Invalid values are rejected with a warning log
   * and the default value (3) is used instead.
   *
   * @param minAllowedSymbolsToSearch minimum characters required before a trailing wildcard.
   *          Defaults to 3 if not specified. Spring injects this from nexus.properties.
   */
  public SqlSearchWildcardConfiguration(
      @Value("${nexus.search.wildcard.minPrefixLength:3}") final int minAllowedSymbolsToSearch)
  {
    int value = minAllowedSymbolsToSearch;
    if (value < 1 || value > 100) {
      log.warn("Invalid minimum prefix length: {}. Must be between 1 and 100. Using default: {}",
          value, DEFAULT_MIN_ALLOWED);
      value = DEFAULT_MIN_ALLOWED;
    }
    else if (value != DEFAULT_MIN_ALLOWED) {
      log.info("Wildcard search minimum prefix length set to {} (default is {})",
          value, DEFAULT_MIN_ALLOWED);
    }
    SqlSearchWildcardValidator.setMinAllowedSymbolsToSearch(value);
  }
}
