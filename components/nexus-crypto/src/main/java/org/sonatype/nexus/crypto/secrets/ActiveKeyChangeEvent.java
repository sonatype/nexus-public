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
package org.sonatype.nexus.crypto.secrets;

import org.sonatype.nexus.common.event.EventWithSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event fired when the active secret encryption key is changed.
 */
public class ActiveKeyChangeEvent
    extends EventWithSource
{
  private final String newKeyId;

  private final String previousKeyId;

  private final String userId;

  @JsonCreator
  public ActiveKeyChangeEvent(
      @JsonProperty("newKeyId") final String newKeyId,
      @JsonProperty("previousKeyId") final String previousKeyId,
      @JsonProperty("userId") final String userId)
  {
    this.newKeyId = checkNotNull(newKeyId);
    this.previousKeyId = previousKeyId;
    this.userId = userId;
  }

  public String getNewKeyId() {
    return newKeyId;
  }

  public String getPreviousKeyId() {
    return previousKeyId;
  }

  public String getUserId() {
    return userId;
  }
}
