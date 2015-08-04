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
package org.sonatype.nexus.maven.tasks;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.targets.Target;

/**
 * Actual component performing the release removal.
 *
 * @since 2.11.3
 */
public interface ReleaseRemoverBackend
{
  String WALKER = "walker";

  String INDEX = "index";

  /**
   * Invoked to actually perform release removal.
   */
  void removeReleases(ReleaseRemovalRequest request,
                      ReleaseRemovalResult result,
                      MavenRepository repository,
                      @Nullable Target target) throws IOException;
}
