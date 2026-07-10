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
package org.sonatype.nexus.cleanup.internal.rest;

public class CleanupExecutionStatusXO
{
  public enum Status
  {
    RUNNING,
    COMPLETED,
    FAILED
  }

  private String id;

  private String repository;

  private String policy;

  private Status status;

  private Long componentsDeleted;

  private Long componentCount;

  private Long durationMs;

  private String error;

  private boolean dryRun;

  public CleanupExecutionStatusXO() {
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(final String repository) {
    this.repository = repository;
  }

  public String getPolicy() {
    return policy;
  }

  public void setPolicy(final String policy) {
    this.policy = policy;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(final Status status) {
    this.status = status;
  }

  public Long getComponentsDeleted() {
    return componentsDeleted;
  }

  public void setComponentsDeleted(final Long componentsDeleted) {
    this.componentsDeleted = componentsDeleted;
  }

  public Long getComponentCount() {
    return componentCount;
  }

  public void setComponentCount(final Long componentCount) {
    this.componentCount = componentCount;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(final Long durationMs) {
    this.durationMs = durationMs;
  }

  public String getError() {
    return error;
  }

  public void setError(final String error) {
    this.error = error;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(final boolean dryRun) {
    this.dryRun = dryRun;
  }
}
