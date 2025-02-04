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
package org.sonatype.nexus.internal.log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.LoggerLevel;

/**
 * In-memory {@link LoggerOverrides}.
 */
public class MemoryLoggerOverrides
    extends ComponentSupport
    implements LoggerOverrides
{
  private final Map<String, LoggerLevel> backing = new HashMap<>();

  public Map<String, LoggerLevel> getBacking() {
    return backing;
  }

  @Override
  public void load() {
    // empty
  }

  @Override
  public void save() {
    // empty
  }

  @Override
  public void reset() {
    backing.clear();
  }

  @Override
  public void set(final String name, final LoggerLevel level) {
    backing.put(name, level);
  }

  @Override
  @Nullable
  public LoggerLevel get(final String name) {
    return backing.get(name);
  }

  @Override
  @Nullable
  public LoggerLevel remove(final String name) {
    return backing.remove(name);
  }

  @Override
  public boolean contains(final String name) {
    return backing.containsKey(name);
  }

  @Override
  public Iterator<Entry<String, LoggerLevel>> iterator() {
    return backing.entrySet().iterator();
  }

  @Override
  public Map<String, LoggerLevel> syncWithDBAndGet() {
    return backing;
  }
}
