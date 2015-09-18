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

import javax.validation.ValidationException;

import org.sonatype.nexus.repository.Facet;

/**
 * Configuration {@link Facet}.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface ConfigurationFacet
  extends Facet
{
  /**
   * Persist configuration.
   */
  void save() throws Exception;

  /**
   * Convert value to given type.
   */
  <T> T convert(Object value, Class<T> type);

  /**
   * Read object of given type from named configuration section.
   */
  <T> T readSection(Configuration configuration, String section, Class<T> type);

  /**
   * Validate given object.
   *
   * @throws ValidationException
   */
  void validate(Object value, Class<?>... groups);

  /**
   * Read and validate object from named configuration section.
   */
  <T> T validateSection(Configuration configuration, String section, Class<T> type, Class<?>... groups);
}
