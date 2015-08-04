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
package org.sonatype.nexus.test.booter;

/**
 * The interface to "boot" (control lifecycle) of the Nexus being under test in the ITs.
 *
 * @author cstamas
 */
public interface NexusBooter
{
  /**
   * Starts one instance of Nexus bundle. May be invoked only once, or after {@link #stopNexus()} is invoked only,
   * otherwise will throw IllegalStateException.
   */
  public void startNexus(final String testId)
      throws Exception;

  /**
   * Stops, and cleans up the started Nexus instance. May be invoked any times, it will NOOP if not needed to do
   * anything. Will try to ditch the used classloader. The {@link #clean()} method will be invoked on every
   * invocation
   * of this method, making it more plausible for JVM to recover/GC all the stuff from memory in case of any glitch.
   */
  public void stopNexus()
      throws Exception;
}
