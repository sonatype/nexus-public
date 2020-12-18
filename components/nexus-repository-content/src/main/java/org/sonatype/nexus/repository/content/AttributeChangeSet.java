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
package org.sonatype.nexus.repository.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.content.fluent.FluentAttributes;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.29
 */
public class AttributeChangeSet
    implements FluentAttributes<AttributeChangeSet>
{
  private final List<AttributeChange> changes = new ArrayList<>();

  public AttributeChangeSet(final AttributeOperation operation, final String key, final Object value) {
    changes.add(new AttributeChange(operation, key, value));
  }

  public AttributeChangeSet() {
    // do nothing
  }

  @Override
  public AttributeChangeSet attributes(final AttributeOperation change, final String key, final Object value) {
    changes.add(new AttributeChange(change, key, value));

    return this;
  }

  public List<AttributeChange> getChanges() {
    return Collections.unmodifiableList(changes);
  }

  public static class AttributeChange
  {
    private final AttributeOperation operation;

    private final String key;

    private final Object value;

    private AttributeChange(final AttributeOperation operation, final String key, @Nullable final Object value) {
      this.operation = checkNotNull(operation);
      this.key = checkNotNull(key);
      this.value = value;
    }

    public AttributeOperation getOperation() {
      return operation;
    }

    public String getKey() {
      return key;
    }

    @Nullable
    public Object getValue() {
      return value;
    }

    @Override
    public String toString() {
      return "AttributeChange{" +
          "operation=" + operation +
          ", key='" + key + '\'' +
          ", value=" + value +
          "} ";
    }
  }
}
