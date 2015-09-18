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
package org.sonatype.nexus.jmx;

import java.util.Iterator;
import java.util.List;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MBean operation (method) key.
 *
 * @since 3.0
 */
public class OperationKey
{
  private final String name;

  private final List<String> types;

  public OperationKey(final String name, final String[] types) {
    this.name = checkNotNull(name);
    checkNotNull(types);
    this.types = ImmutableList.copyOf(types);
  }

  public OperationKey(final MBeanOperationInfo info) {
    checkNotNull(info);
    this.name = info.getName();
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (MBeanParameterInfo pinfo : info.getSignature()) {
      builder.add(pinfo.getType());
    }
    this.types = builder.build();
  }

  public String getName() {
    return name;
  }

  public List<String> getTypes() {
    return types;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OperationKey operationKey = (OperationKey) o;

    if (name != null ? !name.equals(operationKey.name) : operationKey.name != null) {
      return false;
    }
    if (types != null ? !types.equals(operationKey.types) : operationKey.types != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (types != null ? types.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(name).append('(');
    Iterator<String> iter = types.iterator();
    while (iter.hasNext()) {
      buff.append(iter.next());
      if (iter.hasNext()) {
        buff.append(',');
      }
    }
    buff.append(')');
    return buff.toString();
  }
}
