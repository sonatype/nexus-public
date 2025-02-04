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
package org.sonatype.nexus.common.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.TypeConverter;
import org.eclipse.sisu.inject.TypeArguments;

/**
 * Support for {@link TypeConverter} implementations.
 *
 * @since 3.0
 */
public abstract class TypeConverterSupport<T>
    extends AbstractModule
    implements TypeConverter
{
  private boolean bound;

  @Override
  public void configure() {
    if (!bound) {
      // Explicitly bind module instance under a specific sub-type (not Module as Guice forbids that)
      bind(Key.get(TypeConverterSupport.class, Names.named(getClass().getName()))).toInstance(this);
      bound = true;
    }

    // make sure we pick up the right super type argument, i.e. Foo from TypeConverterSupport<Foo>
    final TypeLiteral<?> superType = TypeLiteral.get(getClass()).getSupertype(TypeConverterSupport.class);
    convertToTypes(Matchers.only(TypeArguments.get(superType, 0)), this);
  }

  public Object convert(final String value, final TypeLiteral<?> toType) {
    try {
      return doConvert(value, toType);
    }
    catch (Exception e) {
      throw new ProvisionException(String.format("Unable to convert value: %s due to: %s", value, e)); // NON-NLS
    }
  }

  protected abstract Object doConvert(String value, TypeLiteral<?> toType) throws Exception;
}
