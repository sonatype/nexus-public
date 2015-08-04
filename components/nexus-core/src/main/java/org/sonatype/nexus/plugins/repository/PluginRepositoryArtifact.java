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
package org.sonatype.nexus.plugins.repository;

import java.io.File;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

/**
 * Represents a resolved artifact from a {@link NexusPluginRepository}.
 */
@Deprecated
public final class PluginRepositoryArtifact
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private GAVCoordinate gav;

  private File file;

  private NexusPluginRepository repo;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  public PluginRepositoryArtifact() {
    // legacy constructor
  }

  PluginRepositoryArtifact(final GAVCoordinate gav, final File file, final NexusPluginRepository repo) {
    this.gav = gav;
    this.file = file;
    this.repo = repo;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public void setCoordinate(final GAVCoordinate gav) {
    this.gav = gav;
  }

  public GAVCoordinate getCoordinate() {
    return gav;
  }

  public void setFile(final File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  public void setNexusPluginRepository(final NexusPluginRepository repo) {
    this.repo = repo;
  }

  public NexusPluginRepository getNexusPluginRepository() {
    return repo;
  }

  public PluginMetadata getPluginMetadata()
      throws NoSuchPluginRepositoryArtifactException
  {
    return repo.getPluginMetadata(gav);
  }
}