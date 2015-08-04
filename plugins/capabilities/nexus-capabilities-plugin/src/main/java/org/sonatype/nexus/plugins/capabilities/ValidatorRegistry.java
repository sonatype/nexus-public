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

import java.util.Collection;

/**
 * Registry of capability validators.
 *
 * @since capabilities 1.10
 */
public interface ValidatorRegistry
{

  /**
   * Returns the validators that applies to specified capability type.
   *
   * @param type capability type to get validators for
   * @return validators or an empty collection if no validators applies to specified type
   */
  Collection<Validator> get(CapabilityType type);

  /**
   * Returns the validators that applies to specified capability instance.
   *
   * @param id id of capability to get validators for
   * @return validators or an empty collection if no validators applies to specified instance
   */
  Collection<Validator> get(CapabilityIdentity id);

}
