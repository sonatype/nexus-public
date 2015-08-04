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
package org.sonatype.nexus.proxy.maven.routing.discovery;

import org.sonatype.nexus.proxy.maven.routing.PrefixSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The result of a strategy discovery.
 *
 * @author cstamas
 */
public class StrategyResult
{
  private final String message;

  private final PrefixSource prefixSource;

  private final boolean routingEnabled;

  /**
   * Constructor.
   */
  public StrategyResult(final String message, final PrefixSource prefixSource, boolean routingEnabled) {
    this.message = checkNotNull(message);
    this.prefixSource = checkNotNull(prefixSource);
    this.routingEnabled = routingEnabled;
  }

  /**
   * Returns strategy specific message (probably explaining how did it get the results).
   *
   * @return the message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the prefix source, as a result of discovery.
   *
   * @return entry source discovered by strategy.
   */
  public PrefixSource getPrefixSource() {
    return prefixSource;
  }

  /**
   * Returns <code>false</code> if remote explicitly requested automatic routing to be disabled. This normally
   * indicates that remote is not able to provide reliable path prefix information. For example, remote itself is a
   * proxy of a repository that does not provide prefix file and cannot be scraped.
   */
  public boolean isRoutingEnabled() {
    return routingEnabled;
  }
}
