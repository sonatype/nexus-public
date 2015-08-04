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
package org.sonatype.nexus.ruby;

/**
 * abstract the info of ONE gem which is delivered by
 * bundler API via /api/v1/dependencies?gems=n1,n2
 *
 * all the versions collected are jruby compatible.
 *
 * retrieve the right <b>java</b> compatible platform
 * for a gem version.
 *
 * with an extra modified attribute to build the right timestamp.
 *
 * @author christian
 */
public interface DependencyData
{
  /**
   * all available versions of the a gem
   *
   * @return String[] all JRuby compatible versions
   */
  String[] versions(boolean prereleased);

  /**
   * retrieve the rubygems platform for a given version
   *
   * @return either the platform of the null
   */
  String platform(String version);

  /**
   * the name of the gem
   */
  String name();

  /**
   * when was the version data last modified.
   */
  long modified();
}