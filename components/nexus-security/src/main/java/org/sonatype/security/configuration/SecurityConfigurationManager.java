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
package org.sonatype.security.configuration;

import java.util.List;

import org.sonatype.configuration.validation.InvalidConfigurationException;

public interface SecurityConfigurationManager
{

  void setAnonymousAccessEnabled(boolean anonymousAccessEnabled);

  boolean isAnonymousAccessEnabled();

  void setAnonymousUsername(String anonymousUsername)
      throws InvalidConfigurationException;

  String getAnonymousUsername();

  void setAnonymousPassword(String anonymousPassword)
      throws InvalidConfigurationException;

  String getAnonymousPassword();

  /**
   * The number of iterations to be used when hashing passwords
   *
   * @return number of hash iterations
   * @since 3.1
   */
  int getHashIterations();

  void setRealms(List<String> realms)
      throws InvalidConfigurationException;

  List<String> getRealms();

  /**
   * Clear the cache and reload from file
   */
  void clearCache();

  /**
   * Save to disk what is currently cached in memory
   */
  void save();
}
