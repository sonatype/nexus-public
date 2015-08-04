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
package org.sonatype.nexus.plugins.lvo;

import java.io.IOException;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;

/**
 * The LVO Plugin interface.
 *
 * @author cstamas
 */
public interface LvoService
{
  /**
   * Returns the latest V (V from GAV) that fits the properties specified by key.
   *
   * @return the V, null if key exists but we are unable to calculate LV.
   */
  DiscoveryResponse getLatestVersionForKey(String key)
      throws NoSuchKeyException,
             NoSuchStrategyException,
             NoSuchRepositoryException,
             IOException;

  /**
   * Queries for the latest V (V from GAV) that fits the properties specified by key. If the passed in v is equal of
   * the latest v, returns null.
   *
   * @param v current version associated with key.
   * @return the V if newer found, null if key exists but we are unable to calculate LV or no newer version exists.
   */
  DiscoveryResponse queryLatestVersionForKey(String key, String v)
      throws NoSuchKeyException,
             NoSuchStrategyException,
             NoSuchRepositoryException,
             IOException;
}
