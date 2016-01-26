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

import javax.annotation.Nullable;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Supplied-value {@link MBeanAttribute}.
 *
 * @since 3.0
 */
public class SuppliedMBeanAttribute
  extends ComponentSupport
  implements MBeanAttribute
{
  private final MBeanAttributeInfo info;

  private final String name;

  private final Supplier supplier;

  public SuppliedMBeanAttribute(final MBeanAttributeInfo info,
                                final Supplier supplier)
  {
    this.info = checkNotNull(info);
    this.name = info.getName();
    this.supplier = checkNotNull(supplier);
  }

  @Override
  public MBeanAttributeInfo getInfo() {
    return info;
  }

  @Override
  public String getName() {
    return name;
  }

  // TODO: coercion for non-open types?

  @Override
  @Nullable
  public Object getValue() {
    return supplier.get();
  }

  @Override
  public void setValue(final Object value) throws Exception {
    throw new AttributeNotFoundException("Attribute is read-only: " + name);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", supplier=" + supplier +
        '}';
  }

  //
  // Builder
  //

  /**
   * {@link SuppliedMBeanAttribute} builder.
   */
  public static class Builder
    extends ComponentSupport
  {
    private String name;

    private String description;

    private String type;

    private Supplier supplier;

    private Descriptor descriptor;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    public Builder type(final String type) {
      this.type = type;
      return this;
    }

    public Builder type(final Class<?> type) {
      this.type = type.getName();
      return this;
    }

    public Builder supplier(final Supplier supplier) {
      this.supplier = supplier;
      return this;
    }

    public Builder value(@Nullable final Object value) {
      this.supplier = Suppliers.ofInstance(value);

      // auto-set type if we can
      if (value != null) {
        type(value.getClass());
      }
      return this;
    }

    public Builder descriptor(final Descriptor descriptor) {
      this.descriptor = descriptor;
      return this;
    }

    public SuppliedMBeanAttribute build() {
      checkState(name != null);
      checkState(supplier != null);

      // Determine type name, from configuration or resolve supplied object and use its type
      if (type == null) {
        Object value = supplier.get();
        checkState(value != null, "Can not resolve type from supplier, value is null;  Configure type specifically");
        type = value.getClass().getName();
      }

      MBeanAttributeInfo info = new MBeanAttributeInfo(
          name,
          type,
          description,
          true, // readable
          false, // writable
          false, // is-form
          descriptor
      );

      log.trace("Building attribute with info: {}", info);
      return new SuppliedMBeanAttribute(info, supplier);
    }
  }
}
