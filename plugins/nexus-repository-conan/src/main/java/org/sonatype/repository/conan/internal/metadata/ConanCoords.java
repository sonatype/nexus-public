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
package org.sonatype.repository.conan.internal.metadata;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Each project consists of these element. They are grouped here for easier access throughout the code base
 *
 * @since 3.next
 */
public class ConanCoords
{
  final private String path;

  final private String group;

  final private String project;

  final private String version;

  final private String channel;

  final private String sha;

  public ConanCoords(final String path,
                     final String group,
                     final String project,
                     final String version,
                     final String channel,
                     @Nullable final String sha) {
    this.path = checkNotNull(path);
    this.group = checkNotNull(group);
    this.project = checkNotNull(project);
    this.version = checkNotNull(version);
    this.channel = checkNotNull(channel);
    this.sha = sha;
  }

  public String getPath() { return path; }

  public String getGroup() {
    return group;
  }

  public String getProject() {
    return project;
  }

  public String getVersion() {
    return version;
  }

  public String getChannel() {
    return channel;
  }

  public String getSha() {
    return sha;
  }
}
