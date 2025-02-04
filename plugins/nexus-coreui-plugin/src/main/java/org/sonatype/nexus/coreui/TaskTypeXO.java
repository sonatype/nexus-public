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
import javax.validation.constraints.NotNull;

/**
 * Task type exchange object.
 *
 * @since 3.0
 */
public class TaskTypeXO
{
  @NotBlank
  private String id;

  @NotBlank
  private String name;

  @NotNull
  private Boolean exposed;

  @NotNull
  private Boolean concurrentRun;

  private List<FormFieldXO> formFields;

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

  public Boolean getExposed() {
    return exposed;
  }

  public void setExposed(Boolean exposed) {
    this.exposed = exposed;
  }

  public Boolean getConcurrentRun() {
    return concurrentRun;
  }

  public void setConcurrentRun(Boolean concurrentRun) {
    this.concurrentRun = concurrentRun;
  }

  public List<FormFieldXO> getFormFields() {
    return formFields;
  }

  public void setFormFields(List<FormFieldXO> formFields) {
    this.formFields = formFields;
  }

  @Override
  public String toString() {
    return "TaskTypeXO{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", exposed=" + exposed +
        ", concurrentRun=" + concurrentRun +
        ", formFields=" + formFields +
        '}';
  }
}
