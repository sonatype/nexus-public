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
package org.sonatype.nexus.proxy.targets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.maven.maven1.Maven1ContentClass;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;

/**
 * Support class for DefaultTargetRegistry testing
 *
 * @author Marvin Froeder ( velo at sonatype.com )
 */
public abstract class AbstractDefaultTargetRegistryTest
    extends NexusAppTestSupport
{

  protected ApplicationConfiguration applicationConfiguration;

  protected TargetRegistry targetRegistry;

  protected ContentClass maven1;

  protected ContentClass maven2;

  public AbstractDefaultTargetRegistryTest() {
    super();
  }

  protected void setUp()
      throws Exception
  {
    super.setUp();

    applicationConfiguration = lookup(ApplicationConfiguration.class);

    maven1 = new Maven1ContentClass();

    maven2 = new Maven2ContentClass();

    targetRegistry = lookup(TargetRegistry.class);

    // shave off defaults
    final Collection<Target> targets = new ArrayList<Target>(targetRegistry.getRepositoryTargets());
    for (Target t : targets) {
      targetRegistry.removeRepositoryTarget(t.getId());
    }

    // adding two targets
    Target t1 =
        new Target("maven2-public", "Maven2 (public)", maven2,
            Arrays.asList(new String[]{"/org/apache/maven/((?!sources\\.).)*"}));

    targetRegistry.addRepositoryTarget(t1);

    Target t2 =
        new Target("maven2-with-sources", "Maven2 sources", maven2,
            Arrays.asList(new String[]{"/org/apache/maven/.*"}));

    targetRegistry.addRepositoryTarget(t2);

    Target t3 =
        new Target("maven1", "Maven1", maven1, Arrays.asList(new String[]{"/org\\.apache\\.maven.*"}));

    targetRegistry.addRepositoryTarget(t3);

    applicationConfiguration.saveConfiguration();
  }

}