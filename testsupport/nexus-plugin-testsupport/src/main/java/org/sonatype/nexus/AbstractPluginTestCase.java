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
package org.sonatype.nexus;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.guice.NexusTypeBinder;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.eclipse.sisu.plexus.PlexusAnnotatedBeanModule;
import org.eclipse.sisu.plexus.PlexusBeanModule;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.URLClassSpace;
import org.junit.Assert;

/**
 * Base class to be extended by Nexus plugins tests. Beside the standard {@link NexusAppTestSupport} functionality
 * will scan additional paths for components, such as "target/classes", "target/test-classes", or ant-like classpath
 * entries.
 *
 * @author ...
 * @author Alin Dreghiciu
 */
public abstract class AbstractPluginTestCase
    extends NexusAppTestSupport
{
  protected String[] sourceDirectories = {"target/classes", "target/test-classes"};

  @Override
  protected void setupContainer() {
    super.setupContainer();

    try {
      final List<URL> scanList = new ArrayList<URL>();

      final String[] sourceDirs = getSourceDirectories();
      for (String sourceDir : sourceDirs) {
        scanList.add(getTestFile(sourceDir).toURI().toURL());
      }

      final ClassSpace annSpace =
          new URLClassSpace(getContainer().getContainerRealm(), scanList.toArray(new URL[scanList.size()]));
      final PlexusBeanModule nexusPluginModule =
          new PlexusAnnotatedBeanModule(annSpace, new HashMap<String, String>()).with(NexusTypeBinder.STRATEGY);
      final List<PlexusBeanModule> modules = Arrays.<PlexusBeanModule>asList(nexusPluginModule);

      // register new injector
      ((DefaultPlexusContainer) getContainer()).addPlexusInjector(modules);
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Failed to create plexus container: " + e.getMessage());
    }
  }

  /**
   * Returns a list of source directories to be scanned for components. The list is composed from
   * {@link #getDefaultSourceDirectories()}, {@link #getAdditionalSourceDirectories()} and the dependent plugins
   * directories.
   *
   * @return list of source directories (should not be null)
   */
  protected String[] getSourceDirectories() {
    final List<String> directories = new ArrayList<String>();
    final String[] defaultDirs = getDefaultSourceDirectories();
    if (defaultDirs != null && defaultDirs.length > 0) {
      directories.addAll(Arrays.asList(defaultDirs));
    }
    final String[] additionalDirs = getAdditionalSourceDirectories();
    if (additionalDirs != null && additionalDirs.length > 0) {
      directories.addAll(Arrays.asList(additionalDirs));
    }

    return directories.toArray(new String[directories.size()]);
  }

  /**
   * Returns a list of default directories to be scanned for components.
   *
   * @return list of source directories (should not be null)
   */
  protected String[] getDefaultSourceDirectories() {
    return sourceDirectories;
  }

  /**
   * Returns a list of additional directories to be scanned for components beside default ones. By default the list
   * is
   * empty but can be overridden by tests in order to add additional directories.
   *
   * @return list of source directories (should not be null)
   */
  protected String[] getAdditionalSourceDirectories() {
    return new String[0];
  }

  /**
   * Returns a list of claspath entry paths to be scanned.
   *
   * @return list of classpath entry paths (should not be null)
   */
  protected String[] getClasspathEntries() {
    return new String[0];
  }

}
