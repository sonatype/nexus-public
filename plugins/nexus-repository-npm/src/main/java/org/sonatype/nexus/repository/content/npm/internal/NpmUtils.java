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
package org.sonatype.nexus.repository.content.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.npm.internal.NpmJsonUtils;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Shared code for npm.
 *
 * @since 3.30
 */
public final class NpmUtils
{
  private NpmUtils() {
    // nop
  }

  /**
   * Returns a new {@link InputStream} that returns an error object. Mostly useful for NPM Responses that have already
   * been written with a successful status (like a 200) but just before streaming out content found an issue preventing
   * the intended content to be streamed out.
   *
   * @return InputStream
   */
  public static InputStream errorInputStream(final String message) {
    NestedAttributesMap errorObject = new NestedAttributesMap("error", newHashMap());
    errorObject.set("success", false);
    errorObject.set("error", "Failed to stream response due to: " + message);
    return new ByteArrayInputStream(NpmJsonUtils.bytes(errorObject));
  }
}
