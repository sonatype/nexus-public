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
package org.sonatype.nexus.repository.rest.api;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for creating {@link ComponentXO} instances.
 *
 * @since 3.8
 */
@Singleton
@Named
public class ComponentXOFactory
{
  private final Set<ComponentXODecorator> componentXODecorators;

  @Inject
  public ComponentXOFactory(final Set<ComponentXODecorator> componentXODecorators) {
    this.componentXODecorators = checkNotNull(componentXODecorators);
  }

  public ComponentXO createComponentXO() {
    ComponentXO componentXO = new DefaultComponentXO();
    for (ComponentXODecorator componentXODecorator : componentXODecorators) {
      componentXO = componentXODecorator.decorate(componentXO);
    }
    return componentXO;
  }
}
