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
import java.util.List;

import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.routing.WritablePrefixSource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.util.PathUtils.elementsOf;
import static org.sonatype.nexus.util.PathUtils.pathFrom;

/**
 * A modifier for {@link WritablePrefixSource}, that makes you able to "edit" it by adding and removing entries from
 * it.
 * It performs entry source changes only when needed (entries added or removed does modify prefix list), and defers
 * saving of modified prefix list until you invoke {@link #apply()}.
 *
 * @author cstamas
 * @since 2.4
 */
public class WritablePrefixSourceModifier
{
  private final WritablePrefixSource writablePrefixSource;

  private final int maxDepth;

  private final List<String> toBeAdded;

  private final List<String> toBeRemoved;

  private List<String> prefixSourceEntries;

  private PathMatcher pathMatcher;

  /**
   * Constructor. The {@link WritablePrefixSource} must been ensured some way that the content exists and will not
   * get
   * modified and/or get deleted. Usually, if backed by {@link StorageFileItem} like {@link FilePrefixSource}, you'd
   * want to lock the file at least for {@link Action#read}. If these conditions are not met or assured, use of this
   * class will lead to {@link NullPointerException} and other problems. If the {@link WritablePrefixSource} you
   * operate against is not shared (across multiple threads, is accessed in single thread only), this can be
   * neglected.
   */
  public WritablePrefixSourceModifier(final WritablePrefixSource writablePrefixSource, final int maxDepth)
      throws IOException
  {
    checkArgument(maxDepth >= 2);
    this.writablePrefixSource = checkNotNull(writablePrefixSource);
    this.maxDepth = maxDepth;
    this.toBeAdded = new ArrayList<String>();
    this.toBeRemoved = new ArrayList<String>();
    reset(writablePrefixSource.readEntries());
  }

  /**
   * Adds entry to {@link WritablePrefixSource} being modified. Returns {@code true} if the invocation actually did
   * change the prefix list. Changes are cached, entry source is not modified until you invoke {@link #apply()}
   * method.
   *
   * @return {@code true} if the invocation actually did change the prefix list.
   */
  public boolean offerEntry(final String entry) {
    boolean modified = false;
    final String normalizedEntry = pathFrom(elementsOf(entry), maxDepth);
    if (!pathMatcher.matches(normalizedEntry) && !toBeAdded.contains(normalizedEntry)) {
      toBeAdded.add(normalizedEntry);
      modified = true;
    }
    return modified;
  }

  /**
   * Removes entry from {@link WritablePrefixSource} being modified. Returns {@code true} if the invocation actually
   * did change the prefix list. Changes are cached, entry source is not modified until you invoke {@link #apply()}
   * method.
   *
   * @return {@code true} if the invocation actually did change the prefix list.
   */
  public boolean revokeEntry(final String entry) {
    boolean modified = false;
    final String normalizedEntry = pathFrom(elementsOf(entry));
    if (pathMatcher.contains(normalizedEntry) && !toBeRemoved.contains(normalizedEntry)) {
      for (String prefixEntry : prefixSourceEntries) {
        if (prefixEntry.startsWith(normalizedEntry)) {
          toBeRemoved.add(prefixEntry);
          modified = true;
        }
      }
    }
    return modified;
  }

  /**
   * Returns {@code true} if this modifier has cached changes.
   *
   * @return {@code true} if this modifier has cached changes.
   */
  public boolean hasChanges() {
    return !toBeRemoved.isEmpty() || !toBeAdded.isEmpty();
  }

  /**
   * Applies cached changes to backing {@link WritablePrefixSource}. After returning from this method, backing
   * {@link WritablePrefixSource} is modified (if there were cached changes). Returns {@code true} if there were
   * cached changes, and hence, the backing entry source did change.
   *
   * @return {@code true} if there were cached changes and entry source was modified.
   */
  public boolean apply()
      throws IOException
  {
    if (hasChanges()) {
      final ArrayList<String> entries = new ArrayList<String>(writablePrefixSource.readEntries());
      entries.removeAll(toBeRemoved);
      entries.addAll(toBeAdded);
      final ArrayListPrefixSource newEntries = new ArrayListPrefixSource(entries);
      writablePrefixSource.writeEntries(newEntries);
      reset(newEntries.readEntries());
      return true;
    }
    return false;
  }

  /**
   * Resets this instance, by purging all the cached changes and reloads backing {@link WritablePrefixSource} that
   * remained unchanged. Returns {@code true} if there were cached changes.
   *
   * @return {@code true} if there were cached changes.
   */
  public boolean reset()
      throws IOException
  {
    if (hasChanges()) {
      reset(writablePrefixSource.readEntries());
      return true;
    }
    return false;
  }

  // ==

  protected void reset(final List<String> entries) {
    this.toBeAdded.clear();
    this.toBeRemoved.clear();
    this.prefixSourceEntries = entries;
    this.pathMatcher = new PathMatcher(entries);
  }
}
