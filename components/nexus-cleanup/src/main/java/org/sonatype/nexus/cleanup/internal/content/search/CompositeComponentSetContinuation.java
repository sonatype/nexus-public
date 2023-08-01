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
package org.sonatype.nexus.cleanup.internal.content.search;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import org.sonatype.nexus.common.entity.Continuation;

public class CompositeComponentSetContinuation<Component>
    extends ArrayList<Component>
    implements Continuation<Component>
{
  private static final String REGEX_SAFE_DELIMITER = "~";

  private String componentSetContinuationToken;

  private String componentContinuationToken;

  CompositeComponentSetContinuation() {
    this.componentSetContinuationToken = null;
    this.componentContinuationToken = null;
  }

  CompositeComponentSetContinuation(String continuationToken) {
    if (continuationToken == null) {
      componentSetContinuationToken = null;
      componentContinuationToken = null;
    }
    else {
      String[] tokens = continuationToken.split(REGEX_SAFE_DELIMITER);
      if (tokens.length != 2) {
        throw new IllegalArgumentException("Invalid continuation token: " + continuationToken);
      }
      componentSetContinuationToken = decode(tokens[0]);
      componentContinuationToken = decode(tokens[1]);
    }
  }

  public String getComponentSetContinuationToken() {
    return this.componentSetContinuationToken;
  }

  public void setComponentSetContinuationToken(final String setContinuationToken) {
    this.componentSetContinuationToken = setContinuationToken;
  }

  public String getComponentContinuationToken() {
    return this.componentContinuationToken;
  }

  public void setComponentContinuationToken(final String componentContinuationToken) {
    this.componentContinuationToken = componentContinuationToken;
  }

  private String encode(String original) {
    return original == null ? "" : Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));
  }

  private String decode(String original) {
    return original.isEmpty() ? null : new String(Base64.getDecoder().decode(original), StandardCharsets.UTF_8);
  }

  @Override
  public String nextContinuationToken() {
    if (this.componentSetContinuationToken == null && this.componentContinuationToken == null) {
      return null;
    }
    return encode(this.componentSetContinuationToken) + REGEX_SAFE_DELIMITER +
        encode(this.componentContinuationToken);
  }
}