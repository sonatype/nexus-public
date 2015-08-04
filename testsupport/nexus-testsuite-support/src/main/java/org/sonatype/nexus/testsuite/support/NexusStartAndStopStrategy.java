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
package org.sonatype.nexus.testsuite.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate test classes for customizing when Nexus is tarted and stopped. Possible options are each
 * method (default) and once per test class.
 *
 * @since 2.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface NexusStartAndStopStrategy
{

  // fully qualify due to http://bugs.sun.com/view_bug.do?bug_id=6512707
  NexusStartAndStopStrategy.Strategy value() default NexusStartAndStopStrategy.Strategy.EACH_METHOD;

  public static enum Strategy
  {
    /**
     * Strategy to be used when you want Nexus to be started before first test method is invoked and stopped after
     * last test method was executed.
     */
    EACH_TEST,
    /**
     * Strategy to be used when you want Nexus to be started before each test method and stopped after the method
     * was executed.
     */
    EACH_METHOD;
  }

}
