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

import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.capability.CapabilityTypeExists;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

public class CapabilityXO
{
  @NotEmpty(groups = Update.class)
  private String id;

  @NotBlank(groups = Create.class)
  @CapabilityTypeExists(groups = Create.class)
  private String typeId;

  @NotNull
  private Boolean enabled;

  private String notes;

  private Map<String, String> properties;

  private Boolean active;

  private Boolean error;

  private String description;

  private String state;

  private String stateDescription;

  private String status;

  private String typeName;

  private Map<String, String> tags;

  private String disableWarningMessage;

  private String deleteWarningMessage;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(final String typeId) {
    this.typeId = typeId;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(final Boolean active) {
    this.active = active;
  }

  public Boolean getError() {
    return error;
  }

  public void setError(final Boolean error) {
    this.error = error;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getState() {
    return state;
  }

  public void setState(final String state) {
    this.state = state;
  }

  public String getStateDescription() {
    return stateDescription;
  }

  public void setStateDescription(final String stateDescription) {
    this.stateDescription = stateDescription;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(final Map<String, String> tags) {
    this.tags = tags;
  }

  public String getDisableWarningMessage() {
    return disableWarningMessage;
  }

  public void setDisableWarningMessage(final String disableWarningMessage) {
    this.disableWarningMessage = disableWarningMessage;
  }

  public String getDeleteWarningMessage() {
    return deleteWarningMessage;
  }

  public void setDeleteWarningMessage(final String deleteWarningMessage) {
    this.deleteWarningMessage = deleteWarningMessage;
  }

  @Override
  public String toString() {
    return "CapabilityXO(" +
        "id:" + id + '\'' +
        ", typeId:" + typeId +
        ", enabled:" + enabled +
        ", notes:" + notes +
        ", properties:" + properties +
        ", active:" + active +
        ", error:" + error +
        ", description:" + description +
        ", state:" + state +
        ", stateDescription:" + stateDescription +
        ", status:" + status +
        ", typeName:" + typeName +
        ", tags:" + tags +
        ", disableWarningMessage:" + disableWarningMessage +
        ", deleteWarningMessage:" + deleteWarningMessage +
        ")";
  }
}
