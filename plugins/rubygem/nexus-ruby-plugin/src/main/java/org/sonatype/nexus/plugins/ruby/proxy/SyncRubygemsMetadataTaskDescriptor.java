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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepoComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;

/**
 * {@link SyncRubygemsMetadataTask} descriptor.
 *
 * @since 2.11
 */
@Singleton
@Named("SyncRubygemsMetadata")
public class SyncRubygemsMetadataTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
  public static final String ID = "SyncRubygemsMetadataTask";

  public static final String REPO_FIELD_ID = "repositoryId";

  private final RepoComboFormField repoField = new RepoComboFormField(REPO_FIELD_ID, FormField.MANDATORY);

  public String getId() {
    return ID;
  }

  public String getName() {
    return "Rubygems: Synchronize Proxied Index Files";
  }

  @SuppressWarnings("rawtypes")
  public List<FormField> formFields() {
    List<FormField> fields = new ArrayList<FormField>();
    fields.add(repoField);
    return fields;
  }
}