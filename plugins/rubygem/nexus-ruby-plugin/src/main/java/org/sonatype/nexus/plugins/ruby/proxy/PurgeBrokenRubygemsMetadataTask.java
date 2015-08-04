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
package org.sonatype.nexus.plugins.ruby.proxy;

import javax.inject.Named;

/**
 * Purge broken rubygems metadata task.
 *
 * @since 2.11
 */
@Named(PurgeBrokenRubygemsMetadataTaskDescriptor.ID)
public class PurgeBrokenRubygemsMetadataTask extends AbstractProxyScheduledTask {
  public static final String ACTION = "PURGEBROKENRUBYGEMSMETADATA";

  @Override
  protected String getRepositoryFieldId() {
    return PurgeBrokenRubygemsMetadataTaskDescriptor.REPO_FIELD_ID;
  }

  @Override
  public void doRun(ProxyRubyRepository repository) throws Exception {
    repository.purgeBrokenMetadataFiles();
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Purge broken metadata files of repository " + getRepositoryName();
    }
    else {
      return "Purge broken metadata files of all registered rubygems proxy repositories";
    }
  }
}