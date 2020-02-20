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
package org.sonatype.nexus.repository.config;

import org.sonatype.nexus.repository.config.internal.ConfigurationObjectMapperProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Customizes {@link ObjectMapper} for repository configuration.
 *
 * @since 3.0
 * @see ConfigurationObjectMapperProvider
 */
public interface ConfigurationObjectMapperCustomizer
{
  /**
   * Applies any customization to the passed in {@link ObjectMapper}, is never {@code null}.
   */
  void customize(ObjectMapper objectMapper);
}
