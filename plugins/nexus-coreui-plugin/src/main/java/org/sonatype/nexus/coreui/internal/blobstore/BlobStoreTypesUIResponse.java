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
package org.sonatype.nexus.coreui.internal.blobstore;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.SelectOption;
import org.sonatype.nexus.formfields.FormField;

public class BlobStoreTypesUIResponse
{
  private final String id;

  private final String name;

  private final List<FormField> fields;

  private final String customSettingsForm;

  private final Map<String, List<SelectOption>> dropDownValues;

  public BlobStoreTypesUIResponse(final Map.Entry<String, BlobStoreDescriptor> entry) {
    BlobStoreDescriptor descriptor = entry.getValue();

    this.id = descriptor.getId();
    this.name = descriptor.getName();
    this.fields = descriptor.getFormFields();
    this.customSettingsForm = descriptor.customFormName();
    this.dropDownValues = descriptor.getDropDownValues();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<FormField> getFields() {
    return fields;
  }

  public String getCustomSettingsForm() {
    return customSettingsForm;
  }

  public Map<String, List<SelectOption>> getDropDownValues() {
    return dropDownValues;
  }
}
