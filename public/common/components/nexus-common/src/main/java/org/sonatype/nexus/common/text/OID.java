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
package org.sonatype.nexus.common.text;

import java.util.Collection;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a general identifier for objects.
 *
 * String representation should follow the default behavior of {@link Object#toString}.
 * {@link #hashCode}/{@link #equals} behavior differs to make the OID unique based on
 * {@link #type} and {@link #hash}. When constructing from an object the hash is always
 * the {@link System#identityHashCode}.
 *
 */
public class OID
{
  public static final OID NULL = new OID();

  public static final String SEPARATOR = "@";

  private final String type;

  private final int hash;

  private OID(final String type, final int hash) {
    this.type = checkNotNull(type);
    this.hash = hash;
  }

  private OID() {
    type = null;
    hash = System.identityHashCode(this);
  }

  public String getType() {
    return type;
  }

  public int getHash() {
    return hash;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OID)) {
      return false;
    }

    OID that = (OID) obj;
    return Objects.equal(this.type, that.type)
        && Objects.equal(this.hash, that.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, hash);
  }

  @Override
  public String toString() {
    if (this == NULL) {
      return "null";
    }
    return String.format("%s%s%x", type, SEPARATOR, hash);
  }

  public static OID get(final Object obj) {
    if (obj == null) {
      return NULL;
    }
    return new OID(obj.getClass().getName(), System.identityHashCode(obj));
  }

  /**
   * @see #get
   */
  public static OID oid(final Object obj) {
    return get(obj);
  }

  public static OID parse(final String spec) {
    checkNotNull(spec);
    String[] items = spec.split(SEPARATOR);
    if (items.length != 2) {
      throw new IllegalArgumentException();
    }
    return new OID(items[0], Integer.parseInt(items[1], 16));
  }

  public static String render(final Object obj) {
    checkNotNull(obj);
    return get(obj).toString();
  }

  public static <T> T find(final Collection<T> items, final String id) {
    checkNotNull(items);
    checkNotNull(id);

    for (T item : items) {
      if (OID.render(item).equals(id)) {
        return item;
      }
    }

    return null;
  }

  public static <T> T find(final Collection<T> items, final OID id) {
    checkNotNull(id);
    return find(items, id.toString());
  }
}
