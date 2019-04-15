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
package org.sonatype.nexus.common.app;

import javax.inject.Provider;

import org.sonatype.goodies.lifecycle.Lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.eclipse.sisu.inject.TypeArguments;

import static com.google.inject.name.Names.named;

/**
 * Utility class to help bind {@link Provider} components as managed {@link Lifecycle}s.
 *
 * Provider implementations are not automatically exposed under additional interfaces.
 * This small module is a workaround to expose this provider as a (managed) lifecycle.
 *
 * @since 3.16
 */
public class BindAsLifecycleSupport<T extends Lifecycle & Provider<?>>
    extends AbstractModule
{
  @Override
  @SuppressWarnings("unchecked")
  protected void configure() {
    // make sure we pick up the right (super) type argument, i.e. Foo from BindAsLifecycleSupport<Foo>
    TypeLiteral<?> superType = TypeLiteral.get(getClass()).getSupertype(BindAsLifecycleSupport.class);
    TypeLiteral<T> typeArgument = (TypeLiteral<T>) TypeArguments.get(superType, 0);

    // bind using a unique key to avoid clashing with any other 'BindAsLifecycle' in the same bundle
    bind(Lifecycle.class).annotatedWith(named(typeArgument.getRawType().getName())).to(typeArgument);
  }
}
