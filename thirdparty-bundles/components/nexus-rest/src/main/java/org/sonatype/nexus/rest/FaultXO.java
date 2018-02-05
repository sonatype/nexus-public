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
package org.sonatype.nexus.rest;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Fault exchange object.
 *
 * @since 3.0
 */
@XmlRootElement(name = "fault")
public class FaultXO
{
  @JsonProperty
  private String id;

  @JsonProperty
  private String message;

  public FaultXO() {
    // empty
  }

  public FaultXO(final String id, final String message) {
    this.id = id;
    this.message = message;
  }

  public FaultXO(final String id, final Throwable cause) {
    this(id, cause.toString());
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        ", message='" + message + '\'' +
        '}';
  }
}
