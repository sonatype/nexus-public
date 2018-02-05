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
package org.sonatype.nexus.quartz.internal.orient;

import com.orientechnologies.orient.core.metadata.schema.OType;

/**
 * Provides the minimal marshalling functionality needed by {@link MarshalledEntityAdapter}.
 *
 * @see MarshalledEntityAdapter
 * @since 3.0
 */
public interface Marshaller
{
  /**
   * The orient type this marshaller requires to persist object data.
   */
  OType getType();

  /**
   * Marshal given value.
   *
   * Return value must be compatible with {@link #getType()}.
   */
  Object marshall(Object value) throws Exception;

  /**
   * Unmarshall data into object of given type.
   *
   * Marshalled data must be compatible with {@link #getType()}.
   */
  <T> T unmarshall(Object marshalled, Class<T> type) throws Exception;
}
