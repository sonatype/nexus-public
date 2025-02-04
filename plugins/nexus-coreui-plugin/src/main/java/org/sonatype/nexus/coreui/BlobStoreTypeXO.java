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
package org.sonatype.nexus.coreui;

import java.util.List;
import javax.validation.constraints.NotBlank;

/**
 * BlobStore Type exchange object.
 *
 * @since 3.6
 */
public class BlobStoreTypeXO
{
  @NotBlank
  private String id;

  @NotBlank
  private String name;

  private List<FormFieldXO> formFields;

  private String customFormName;

  private boolean isModifiable;

  private boolean isEnabled;

  private boolean isConnectionTestable;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FormFieldXO> getFormFields() {
    return formFields;
  }

  public void setFormFields(List<FormFieldXO> formFields) {
    this.formFields = formFields;
  }

  public String getCustomFormName() {
    return customFormName;
  }

  public void setCustomFormName(String customFormName) {
    this.customFormName = customFormName;
  }

  public boolean isModifiable() {
    return isModifiable;
  }

  public void setIsModifiable(boolean isModifiable) {
    this.isModifiable = isModifiable;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setIsEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public boolean isConnectionTestable() {
    return isConnectionTestable;
  }

  public void setConnectionTestable(boolean isConnectionTestable) {
    this.isConnectionTestable = isConnectionTestable;
  }

  @Override
  public String toString() {
    return "BlobStoreTypeXO{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", formFields=" + formFields +
        ", customFormName='" + customFormName + '\'' +
        ", isModifiable=" + isModifiable +
        ", isEnabled=" + isEnabled +
        ", isConnectionTestable=" + isConnectionTestable +
        '}';
  }
}
