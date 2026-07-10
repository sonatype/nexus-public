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
package org.slf4j;

/**
 * No-op compatibility shim for {@code org.slf4j.NDC} (Nested Diagnostic Context).
 *
 * <p>
 * SLF4J 1.x shipped {@code org.slf4j.NDC} as a thin wrapper over the underlying logger's NDC
 * implementation; SLF4J 2.x removed it entirely (callers were expected to migrate to {@code MDC}).
 * Our {@code directjngine} dependency (com.softwarementors.extjs.djn 2.3.0) still calls
 * {@code NDC.push(String)} and {@code NDC.pop()} in {@code DirectJNgineServlet#doPost} (3 callsites).
 * Without this class on the classpath, every ExtDirect request fails with
 * {@code NoClassDefFoundError: org/slf4j/NDC}, which surfaces in the UI as a 500 on
 * {@code /service/extdirect/poll/rapture_State_get} and prevents pages from rendering after login.
 *
 * <p>
 * NDC is a thread-local stack of context strings; in SLF4J 1.x its values were pushed onto the
 * underlying log appender's diagnostic context for inclusion in log output. directjngine uses it
 * only to tag log messages with a request-scoped string ("DirectJNgine"). Nexus does not consume
 * NDC values anywhere in its logback configuration, so a no-op shim preserves observable behavior:
 * the only loss is a context tag in directjngine's own debug logs, which we don't display.
 *
 * <p>
 * This shim is intentionally placed inside {@code nexus-extdirect} (the sole module that pulls
 * directjngine into the runtime classpath) rather than as a standalone module, to keep the blast
 * radius minimal and the lifetime obvious: it can be deleted as soon as directjngine is respun
 * against SLF4J 2.x (i.e. switched to MDC) or replaced. NEXUS-46395.
 *
 * <p>
 * The class deliberately implements only the small subset of the SLF4J 1.x NDC API actually
 * referenced by directjngine. If a future dependency calls a method not listed here, the JVM will
 * raise {@code NoSuchMethodError} at the callsite — making the missing API discoverable rather than
 * silently broken.
 */
public final class NDC
{
  private NDC() {
    // no instances
  }

  /**
   * SLF4J 1.x: push a context value onto the NDC stack. No-op here.
   * Called by {@code DirectJNgineServlet#doPost}.
   */
  public static void push(@SuppressWarnings("unused") final String message) {
    // no-op
  }

  /**
   * SLF4J 1.x: pop the top context value off the NDC stack and return it. Returns "" here.
   * Called by {@code DirectJNgineServlet#doPost}.
   */
  public static String pop() {
    return "";
  }

  /**
   * SLF4J 1.x: peek at the top context value without popping. Returns "" here.
   * Not currently called by directjngine but included for forward-compatibility with other
   * potential SLF4J-1.x-era dependencies dragged in via plugins.
   */
  public static String peek() {
    return "";
  }

  /**
   * SLF4J 1.x: clear the NDC stack. No-op here.
   */
  public static void clear() {
    // no-op
  }

  /**
   * SLF4J 1.x: remove the NDC for the current thread. No-op here.
   */
  public static void remove() {
    // no-op
  }
}
