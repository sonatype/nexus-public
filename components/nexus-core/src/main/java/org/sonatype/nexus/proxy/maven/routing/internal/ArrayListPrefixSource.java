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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.proxy.maven.routing.PrefixSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link PrefixSource} implementation backed by {@link ArrayList}.
 *
 * @author cstamas
 * @since 2.4
 */
public class ArrayListPrefixSource
    implements PrefixSource
{
  private final List<String> entries;

  private final long created;

  /**
   * Constructor with entries. Will have last modified timestamp as "now" (moment of creation).
   *
   * @param entries list of entries, might not be {@code null}
   */
  public ArrayListPrefixSource(final List<String> entries) {
    this(entries, System.currentTimeMillis());
  }

  /**
   * Constructor with entries and timestamp.
   *
   * @param entries list of entries, might not be {@code null}.
   * @param created the timestamp this instance should report.
   */
  public ArrayListPrefixSource(final List<String> entries, final long created) {
    this.entries = Collections.unmodifiableList(checkNotNull(entries));
    this.created = created;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public boolean supported() {
    return true;
  }

  @Override
  public List<String> readEntries()
      throws IOException
  {
    return entries;
  }

  @Override
  public long getLostModifiedTimestamp() {
    return created;
  }
}
