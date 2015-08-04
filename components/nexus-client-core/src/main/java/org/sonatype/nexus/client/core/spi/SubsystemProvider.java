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
package org.sonatype.nexus.client.core.spi;

import java.util.Map;

/**
 * Subsystems factory. It creates subsystems of given type in case that it knows how to do it.
 * If it does not know how to create such a subsystem it should return null, case when the remaining
 * {@link SubsystemProvider}s will be used to create, until one will be able to do it.
 *
 * @since 2.7
 */
public interface SubsystemProvider
{

  /**
   * Creates an instance of the subsystem for given type.
   *
   * @param type    of subsystem to be created (never null)
   * @param context provides access to client specific components
   * @return an instance of subsystem or null if it is not able to create a subsystem of specified type
   */
  Object get(Class type, Map<Object, Object> context);

}
