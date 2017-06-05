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
package org.sonatype.nexus.plugins.capabilities.internal.guice;

import javax.inject.Named;

import org.sonatype.nexus.plugins.capabilities.internal.ActivationConditionHandlerFactory;
import org.sonatype.nexus.plugins.capabilities.internal.ValidityConditionHandlerFactory;
import org.sonatype.nexus.plugins.capabilities.internal.storage.CapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.internal.storage.XmlCapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.internal.validator.ValidatorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Capabilities plugin Guice module.
 *
 * @since capabilities 1.0
 */
@Named
public class GuiceModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(CapabilityStorage.class).to(XmlCapabilityStorage.class);

    install(new FactoryModuleBuilder().build(ActivationConditionHandlerFactory.class));
    install(new FactoryModuleBuilder().build(ValidityConditionHandlerFactory.class));
    install(new FactoryModuleBuilder().build(ValidatorFactory.class));
  }
}
