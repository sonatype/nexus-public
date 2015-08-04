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
package org.sonatype.nexus.proxy.item;

import java.io.InputStream;
import java.security.MessageDigest;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.util.DigesterUtils;

import com.google.common.io.ByteStreams;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ChecksummingContentLocatorTest
{
  private static final String CONTENT = "This is a test that checksumming content locators can be re-used";
  
  private static final Object DIGEST = DigesterUtils.getSha1Digest(CONTENT);

  @Test
  public void testCanReuseChecksummingContentLocator() throws Exception {

    final ContentLocator content = new StringContentLocator(CONTENT);
    final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
    final String contextKey = StorageFileItem.DIGEST_SHA1_KEY;
    final RequestContext context = new RequestContext();

    ContentLocator contentLocator = new ChecksummingContentLocator(content, messageDigest, contextKey, context);

    try (InputStream is = contentLocator.getContent()) {
      is.read(new byte[8]); // partial read
    }

    try (InputStream is = contentLocator.getContent()) {
      is.read(new byte[8]); // partial read
    }

    try (InputStream is = contentLocator.getContent()) {
      ByteStreams.copy(is, ByteStreams.nullOutputStream());
    }

    assertThat(context.get(contextKey), equalTo(DIGEST));
  }
}