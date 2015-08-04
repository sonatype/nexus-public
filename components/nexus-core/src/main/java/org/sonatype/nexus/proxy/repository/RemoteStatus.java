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
package org.sonatype.nexus.proxy.repository;

import static com.google.common.base.Preconditions.checkNotNull;

public class RemoteStatus
{

  public static final RemoteStatus UNKNOWN = new RemoteStatus(Type.UNKNOWN);

  public static final RemoteStatus AVAILABLE = new RemoteStatus(Type.AVAILABLE);

  public static final RemoteStatus UNAVAILABLE = new RemoteStatus(Type.UNAVAILABLE);

  private final Type type;

  private final String reason;

  public RemoteStatus(final Type type) {
    this.type = checkNotNull(type);
    this.reason = null;
  }

  public RemoteStatus(final Type type, final String reason) {
    this.type = checkNotNull(type);
    this.reason = reason;
  }

  public Type getType() {
    return type;
  }

  public String getReason() {
    return reason;
  }

  public String name() {
    return type.name();
  }

  public static RemoteStatus valueOf(final String name) {
    return new RemoteStatus(Type.valueOf(name));
  }

  public enum Type
  {
    UNKNOWN, AVAILABLE, UNAVAILABLE
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RemoteStatus)) {
      return false;
    }

    final RemoteStatus that = (RemoteStatus) o;

    if (type != that.type) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public String toString() {
    return type.toString();
  }

}
