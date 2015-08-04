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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;

/**
 * NX Task that backs up NPM MetadataStore.
 *
 * @since 2.11
 */
@Named(NpmDbBackupTaskDescriptor.ID)
@Singleton
public class NpmDbBackupTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
  public static final String ID = "NpmDbBackupTask";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Backup npm metadata database";
  }
}
