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
package org.sonatype.nexus.features.internal;

import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import static java.lang.System.getProperty;

/**
 * Installs the fast {@link FeaturesService} wrapper.
 *
 * @since 3.19
 */
public class Activator
    implements BundleActivator
{
  private FeaturesWrapper wrapper;

  @Override
  public void start(final BundleContext context) throws Exception {
    // set this property to true in system.properties to turn this feature off
    if (!"true".equalsIgnoreCase(getProperty("karaf.disableFastFeatures"))) {
      wrapper = new FeaturesWrapper(context);
      wrapper.open();
    }
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    if (wrapper != null) {
      wrapper.close();
      wrapper = null;
    }
  }
}
