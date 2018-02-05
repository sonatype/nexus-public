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
package org.sonatype.nexus.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonatype.nexus.repository.config.Configuration;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Repository facet.
 *
 * @since 3.0
 */
public interface Facet
{
  /**
   * Attach repository to facet.
   *
   * @see Repository#attach(Facet)
   */
  void attach(Repository repository) throws Exception;

  /**
   * Validate configuration.
   */
  void validate(Configuration configuration) throws Exception;

  /**
   * Initialize facet.
   */
  void init() throws Exception;

  /**
   * Update facet.
   *
   * Called when repository configuration has changed.
   */
  void update() throws Exception;

  /**
   * Start facet.
   *
   * Facet has been previously initialized or updated.
   */
  void start() throws Exception;

  /**
   * Stop facet.
   *
   * Facet was previously started.  Facet is stopped before applying {@link #update}.
   */
  void stop() throws Exception;

  /**
   * Delete facet.
   *
   * Allows facet to cope with contained repository being deleted and clean up persistent knowledge about the
   * repository or its contents.
   *
   * Facet must have been previously stopped.
   */
  void delete() throws Exception;

  /**
   * Destroy facet.
   *
   * Allows facet to clean up resources.  This is different than {@link #delete}.
   *
   * Facet is stopped before destruction.
   */
  void destroy() throws Exception;

  /**
   * Marks {@link Facet} interface and/or implementation as exposed.
   *
   * Exposed types can be accessed via {@link Repository#facet}.
   */
  @Documented
  @Target(TYPE)
  @Retention(RUNTIME)
  @interface Exposed
  {
    // marker
  }
}
