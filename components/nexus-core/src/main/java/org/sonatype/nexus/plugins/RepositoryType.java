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
package org.sonatype.nexus.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface (must extend org.sonatype.nexus.proxy.repository.Repository) as new repository type to be handled
 * by Nexus.
 *
 * @author cstamas
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface RepositoryType
{
  /**
   * The constant denoting unlimited count of instances.
   */
  int UNLIMITED_INSTANCES = -1;

  /**
   * The path prefix to "mount" under content URL.
   */
  String pathPrefix();

  /**
   * The "hard" limit of maximal instance count for this repository. Default is unlimited. See NexusConfiguration
   * iface for details.
   */
  int repositoryMaxInstanceCount() default org.sonatype.nexus.plugins.RepositoryType.UNLIMITED_INSTANCES; // fully qualify for http://bugs.sun.com/view_bug.do?bug_id=6512707

}
