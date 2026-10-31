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
package org.sonatype.nexus.api.rest.selfhosted.telemetry.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The mandatory telemetry acknowledgement record, shared by the GET response and the POST request
 * body, so both sides speak the same {@code {"acknowledgement": true}} shape.
 * <p>
 * The record holds a single flag. Who acknowledged, when, and at which version are deliberately not
 * stored, so the property name here matches the stored field name exactly.
 */
public class MandatoryTelemetryAcknowledgementStatus
{
  @Schema(description = "Whether an administrator has acknowledged that telemetry is mandatory")
  private boolean acknowledgement;

  public boolean isAcknowledgement() {
    return acknowledgement;
  }

  public void setAcknowledgement(final boolean acknowledgement) {
    this.acknowledgement = acknowledgement;
  }
}
