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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;

import com.google.common.collect.ImmutableList;

/**
 * NX Task that deletes all the metadata from npm repository (hosted or proxy).
 *
 * @since 2.11.3
 */
@Named(NpmMetadataDeleteTaskDescriptor.ID)
@Singleton
public class NpmMetadataDeleteTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
  public static final String ID = "NpmMetadataDeleteTask";

  static final String FLD_REPOSITORY_ID = "repositoryId";

  private final List<FormField> fields = ImmutableList.<FormField>of(
      new RepoOrGroupComboFormField(FLD_REPOSITORY_ID,
          FormField.MANDATORY)
  );

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Delete npm metadata";
  }

  public List<FormField> formFields() {
    return fields;
  }

}
