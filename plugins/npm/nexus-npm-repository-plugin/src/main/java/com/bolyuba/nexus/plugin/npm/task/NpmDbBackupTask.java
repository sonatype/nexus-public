/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.NexusConfiguration;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.internal.orient.OrientMetadataStore;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * NX Task that backs up NPM MetadataStore.
 *
 * @since 2.11
 */
@Named(NpmDbBackupTaskDescriptor.ID)
@Singleton
public class NpmDbBackupTask
    extends NpmTaskSupport<NpmRepository>
{
  private final OrientMetadataStore orientMetadataStore;

  @Inject
  public NpmDbBackupTask(final NexusConfiguration nexusConfiguration, final OrientMetadataStore orientMetadataStore) {
    super(nexusConfiguration);
    this.orientMetadataStore = checkNotNull(orientMetadataStore);
  }

  @Override
  protected void doExecute(final List<NpmRepository> repositories) throws Exception {
    orientMetadataStore.backupDatabase();
  }

  @Override
  protected List<NpmRepository> getAffectedRepositories() {
    return getRepositoryRegistry().getRepositoriesWithFacet(NpmRepository.class);
  }

  @Override
  protected String getAction() {
    return "NPM-DB-BACKUP";
  }

  @Override
  protected String getMessage() {
    return "Backing up npm metadata database.";
  }
}
