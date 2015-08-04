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

/**
 * This is an exception to mark that a strategy has failed to do the work. This exception should be used to mark some
 * expected case (like no remote prefix file, or not able to scrape), so to say "soft" failure.
 *
 * @author cstamas
 * @since 2.4
 */
@SuppressWarnings("serial")
public class StrategyFailedException
    extends Exception
{
  /**
   * Constructor.
   */
  public StrategyFailedException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Constructor.
   */
  public StrategyFailedException(String msg) {
    super(msg);
  }
}
