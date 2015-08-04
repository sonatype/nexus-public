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
package org.sonatype.nexus.testsuite.ruby.trials;

import java.io.File;

/**
 * The "hola roundtrip" scenario trial.
 */
public class HolaTrial
    extends TrialSupport
{
  public HolaTrial(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected ScenarioSupport createScenario() {
    final File workdir = util.createTempDir();
    // TODO: uses the fact the goodies util makes random file names. Any other random means is OK to create isolated env
    return new HolaScenario(workdir.getName(), workdir, client());
  }
}
