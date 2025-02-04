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
package org.sonatype.nexus.common.thread;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to simplify boilerplate to begin (capture, set) and restore the current Thread's context-class-loader.
 *
 * <pre>{@code
 * try (TcclBlock tccl = TcclBlock.begin(newClassLoader)) {
 *   // do something which requires TCCL to be newClassLoader
 * }
 * }</pre>
 *
 * @since 3.0
 */
public final class TcclBlock
    implements AutoCloseable
{
  private final ClassLoader previous;

  private TcclBlock(final ClassLoader previous) {
    this.previous = previous;
  }

  /**
   * Restore the Thread-context-class-loader to previous.
   */
  @Override
  public void close() {
    Thread.currentThread().setContextClassLoader(previous);
  }

  /**
   * Set the Thread-context-class-loader to given class-loader and return reference to restore.
   */
  public static TcclBlock begin(final ClassLoader classLoader) {
    checkNotNull(classLoader);

    // capture
    Thread thread = Thread.currentThread();
    ClassLoader current = thread.getContextClassLoader();

    // set
    thread.setContextClassLoader(classLoader);

    return new TcclBlock(current);
  }

  /**
   * Helper to return block using class-loader of given type.
   */
  public static TcclBlock begin(final Class<?> type) {
    checkNotNull(type);
    return begin(type.getClassLoader());
  }

  /**
   * Helper to return block using class-loader of given owner.
   */
  public static TcclBlock begin(final Object owner) {
    checkNotNull(owner);
    return begin(owner.getClass().getClassLoader());
  }
}
