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
package org.sonatype.nexus.plugins.capabilities.internal.validator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.Validator;

import com.google.inject.assistedinject.Assisted;

/**
 * A {@link Validator} that ensures that only one capability of specified type and set of properties can be created,
 * excluding itself.
 *
 * @since capabilities 2.0
 */
@Named
public class PrimaryKeyExcludingSelfValidator
    extends PrimaryKeyValidator
    implements Validator
{

  @Inject
  PrimaryKeyExcludingSelfValidator(final CapabilityRegistry capabilityRegistry,
                                   final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                                   final @Assisted CapabilityIdentity selfId,
                                   final @Assisted CapabilityType type,
                                   final @Assisted String... propertyKeys)
  {
    super(capabilityRegistry, capabilityDescriptorRegistryProvider, selfId, type, propertyKeys);
  }

}
