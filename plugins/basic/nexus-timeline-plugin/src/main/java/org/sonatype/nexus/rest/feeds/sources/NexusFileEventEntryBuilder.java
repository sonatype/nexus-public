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
package org.sonatype.nexus.rest.feeds.sources;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.maven.gav.Gav;

/**
 * Build feeds entry based on files
 *
 * @author Juven Xu
 */
@Named("file")
@Singleton
public class NexusFileEventEntryBuilder
    extends AbstractNexusItemEventEntryBuilder
{

  @Override
  protected String buildTitle(NexusArtifactEvent event) {
    return buildFileName(event);
  }

  private String buildFileName(NexusArtifactEvent event) {
    return buildFilePath(event).substring(buildFilePath(event).lastIndexOf("/") + 1);
  }

  private String buildFilePath(NexusArtifactEvent event) {
    return event.getNexusItemInfo().getPath();
  }

  @Override
  protected String buildDescriptionMsgItem(NexusArtifactEvent event) {
    StringBuilder msg = new StringBuilder();

    msg.append("The file '");

    msg.append(buildFileName(event));

    msg.append("' in repository '");

    msg.append(getRepositoryName(event));

    msg.append("' with path '");

    msg.append(buildFilePath(event));

    msg.append("'");

    return msg.toString();
  }

  @Override
  public boolean shouldBuildEntry(final NexusArtifactEvent event) {
    if (!super.shouldBuildEntry(event)) {
      return false;
    }

    final Gav gav = buildGAV(event);

    if (gav != null) {
      if (gav.isHash() || gav.isSignature()) {
        return false;
      }
    }

    final String path = event.getNexusItemInfo().getPath();

    if (path.contains("maven-metadata.xml")) {
      return false;
    }

    return true;
  }

}
