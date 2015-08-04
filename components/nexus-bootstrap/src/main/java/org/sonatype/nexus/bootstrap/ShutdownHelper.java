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
package org.sonatype.nexus.bootstrap;

/**
 * Helper to cope with different mechanisms to shutdown.
 *
 * @since 2.2
 */
public class ShutdownHelper
{
  public static interface ShutdownDelegate
  {
    void doExit(int code);

    void doHalt(int code);
  }

  public static class JavaShutdownDelegate
      implements ShutdownDelegate
  {
    @Override
    public void doExit(final int code) {
      System.exit(code);
    }

    @Override
    public void doHalt(final int code) {
      Runtime.getRuntime().halt(code);
    }
  }

  private static ShutdownDelegate delegate = new JavaShutdownDelegate();

  public static ShutdownDelegate getDelegate() {
    if (delegate == null) {
      throw new IllegalStateException();
    }
    return delegate;
  }

  public static void setDelegate(final ShutdownDelegate delegate) {
    if (delegate == null) {
      throw new NullPointerException();
    }
    ShutdownHelper.delegate = delegate;
  }

  public static void exit(final int code) {
    getDelegate().doExit(code);
  }

  public static void halt(final int code) {
    getDelegate().doHalt(code);
  }
}
