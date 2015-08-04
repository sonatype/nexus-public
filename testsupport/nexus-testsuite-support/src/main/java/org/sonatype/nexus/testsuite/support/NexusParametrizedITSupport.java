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

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.runners.Parameterized.Parameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.firstAvailableTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.systemProperty;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.systemTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.testParameters;
import static org.sonatype.sisu.goodies.common.Varargs.$;

/**
 * Base class for parametrized Nexus integration tests.
 *
 * @since 2.2
 */
@RunWith(Parameterized.class)
public abstract class NexusParametrizedITSupport
    extends NexusITSupport
{

  @Parameters
  public static Collection<Object[]> data() {
    return firstAvailableTestParameters(
        systemTestParameters(),
        systemProperty("it.nexus.bundle"),
        testParameters(
            $("${it.nexus.bundle.groupId}:${it.nexus.bundle.artifactId}:zip:bundle")
        )
    ).load();
  }

  public NexusParametrizedITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

}
