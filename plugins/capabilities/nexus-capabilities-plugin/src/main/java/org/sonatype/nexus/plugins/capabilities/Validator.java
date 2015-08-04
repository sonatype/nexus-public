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
package org.sonatype.nexus.plugins.capabilities;

import java.util.Map;

/**
 * Validates capability properties.
 *
 * The validators to be used are extracted as follows:<b/>
 * On create:<b/>
 * * Automatically created validators for all mandatory fields and fields supporting regexp validation<b/>
 * * Validators returned by {@link CapabilityDescriptor#validator()} method<b/>
 * * {@link CapabilityFactory}, if it implements {@link Validator}
 *
 * On update:<b/>
 * * Automatically created validators for all mandatory fields and fields supporting regexp validation<b/>
 * * Validators returned by {@link CapabilityDescriptor#validator(CapabilityIdentity)} method<b/>
 * * {@link Capability}, if it implements {@link Validator}
 *
 * @since capabilities 2.0
 */
public interface Validator
{

  /**
   * Validates capability properties before a capability is created/updated.
   *
   * @param properties capability properties that will be applied to capability
   * @return validation result
   */
  ValidationResult validate(Map<String, String> properties);

  /**
   * Describe when validation will pass.
   *
   * @return description
   */
  String explainValid();

  /**
   * Describe when validation will fail.
   *
   * @return description
   */
  String explainInvalid();

}
