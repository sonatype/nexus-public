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
package org.sonatype.nexus.plexusplugin.rest.dto;

import java.util.List;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("plexus-plugin-response")
public class PlexusPluginResponse
{
  private final List<String> repositoryIds;

  private final List<String> repositoryTypes;

  private final List<String> contentClasses;

  private final List<String> scheduledTaskNames;

  private final int eventsReceived;

  public PlexusPluginResponse(final int eventsReceived) {
    this.eventsReceived = eventsReceived;
    this.repositoryIds = Lists.newArrayList();
    this.repositoryTypes = Lists.newArrayList();
    this.contentClasses = Lists.newArrayList();
    this.scheduledTaskNames = Lists.newArrayList();
  }

  public int getEventsReceived() {
    return eventsReceived;
  }

  public List<String> getRepositoryIds() {
    return repositoryIds;
  }

  public List<String> getRepositoryTypes() {
    return repositoryTypes;
  }

  public List<String> getContentClasses() {
    return contentClasses;
  }

  public List<String> getScheduledTaskNames() {
    return scheduledTaskNames;
  }
}
