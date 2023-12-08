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
package org.sonatype.nexus.coreui.internal.capability;

import java.util.List;
import javax.validation.constraints.NotEmpty;

import org.sonatype.nexus.coreui.FormFieldXO;

public class CapabilityTypeXO
{
  @NotEmpty
  private String id;

  @NotEmpty
  private String name;

  private String about;

  private List<FormFieldXO> formFields;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getAbout() {
    return about;
  }

  public void setAbout(final String about) {
    this.about = about;
  }

  public List<FormFieldXO> getFormFields() {
    return formFields;
  }

  public void setFormFields(final List<FormFieldXO> formFields) {
    this.formFields = formFields;
  }

  @Override
  public String toString() {
    return "CapabilityTypeXO(" +
        "id:" + id +
        ", name:" + name +
        ", about:" + about +
        ", formFields:" + formFields +
        ")";
  }
}
