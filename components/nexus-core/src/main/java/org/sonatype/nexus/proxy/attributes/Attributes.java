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
package org.sonatype.nexus.proxy.attributes;

import java.util.Map;

/**
 * Attributes are simply a String key-value pairs with some type-safe getters and setters for keys known and used in
 * core.
 * 
 * @author cstamas
 * @since 2.0
 */
public interface Attributes
{
  /**
   * Returns true if this instance has a value for given key set.
   */
  boolean containsKey(final String key);

  /**
   * Gets the value for given key.
   */
  String get(final String key);

  /**
   * Puts the value for given key, returning any previous values bound. Note: Attributes does not allow {@code null} for
   * key neither values!
   */
  String put(final String key, final String value);

  /**
   * Removes the value with given key from this instance.
   */
  String remove(final String key);

  /**
   * Puts all the entries of the given map into this instance.
   */
  void putAll(Map<? extends String, ? extends String> map);

  // ===

  /**
   * Performs an "overlay" with given attributes: only those values are changed that are not set by some setter
   * method
   * or by {@link #put(String, String)} or by {@link #putAll(Map)}. Core internal use only!
   */
  void overlayAttributes(final Attributes repositoryItemAttributes);

  /**
   * Returns the generation of Attributes. Core internal use only!
   */
  int getGeneration();

  /**
   * Sets the generation of Attributes. Core internal use only!
   */
  void setGeneration(final int value);

  /**
   * Setps the generation of this instance. Core internal use only!
   */
  void incrementGeneration();

  /**
   * Returns the path attribute.
   */
  String getPath();

  /**
   * Sets the path attribute.
   */
  void setPath(final String value);

  /**
   * Returns the readable attribute.
   */
  boolean isReadable();

  /**
   * Sets the readable attribute.
   */
  void setReadable(final boolean value);

  /**
   * Returns the writable attribute.
   */
  boolean isWritable();

  /**
   * Sets the writable attribute.
   */
  void setWritable(final boolean value);

  /**
   * Returns the repositoryId attribute.
   */
  String getRepositoryId();

  /**
   * Sets the repositoryId attribute.
   */
  void setRepositoryId(final String value);

  /**
   * Returns the created attribute.
   */
  long getCreated();

  /**
   * Sets the created attribute.
   */
  void setCreated(final long value);

  /**
   * Returns the modified attribute.
   */
  long getModified();

  /**
   * Sets the modified attribute.
   */
  void setModified(final long value);

  /**
   * Returns the storedLocally attribute.
   */
  long getStoredLocally();

  /**
   * Sets the storedLocally attribute.
   */
  void setStoredLocally(final long value);

  /**
   * Returns the checkedRemotely attribute.
   */
  long getCheckedRemotely();

  /**
   * Sets the checkedRemotely attribute.
   */
  void setCheckedRemotely(final long value);

  /**
   * Returns the lastRequest attribute.
   */
  long getLastRequested();

  /**
   * Sets the lastRequested attribute.
   */
  void setLastRequested(final long value);

  /**
   * Returns the expired attribute.
   */
  boolean isExpired();

  /**
   * Sets the expired attribute.
   */
  void setExpired(final boolean value);

  /**
   * Returns the remoteUrl attribute.
   */
  String getRemoteUrl();

  /**
   * Sets the remoteUrl attribute.
   */
  void setRemoteUrl(final String value);

  // ==

  /**
   * Returns an unmodifiable "snapshot" of this instance as {@code Map<String, String>}.
   */
  Map<String, String> asMap();
}
