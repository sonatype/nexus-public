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
package org.sonatype.nexus.repository.content.store;

import java.util.Arrays;

import org.sonatype.nexus.blobstore.api.BlobRef;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.goodies.testsupport.hamcrest.DiffMatchers.equalTo;

@RunWith(Parameterized.class)
public class BlobRefTypeHandlerTest
{
  @Parameter
  public String blobRef;

  @Parameter(1)
  public String expectedStore;

  @Parameter(2)
  public String expectedBlob;

  @Parameter(3)
  public String expectedNode;

  @Parameter(4)
  public Class<? extends Exception> expectedException;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Parameters(name = "parseTest: blobRef={0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"default:50bc2227-b63b-4faa-85e1-b3dd1eee9afe@042D35AD-6E5E3FF2-C2326B6D-18C20A57-CAB2540C",
         "default", "50bc2227-b63b-4faa-85e1-b3dd1eee9afe", null, null},
        {":blobid@node", "N/A", "N/A", null, IllegalArgumentException.class},
        {"default:@node", "N/A", "N/A", null, IllegalArgumentException.class},
        {"default:blobid@", "N/A", "N/A", null, IllegalArgumentException.class},
        {":@", "N/A", "N/A", null, IllegalArgumentException.class},
        {"", "N/A", "N/A", null, IllegalArgumentException.class},
        });
  }

  @Test
  public void parseTest() {
    if (expectedException != null) {
      thrown.expect(expectedException);
    }
    BlobRef ref = BlobRefTypeHandler.parsePersistableFormat(blobRef);
    assertThat(ref.getStore(), equalTo(expectedStore));
    assertThat(ref.getBlob(), equalTo(expectedBlob));
    assertThat(ref.getNode(), equalTo(expectedNode));
  }
}
