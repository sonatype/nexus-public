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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class EncryptedSecretTest
{
  @Test
  public void testCreatesParsePhcSecretWithVersion() {
    EncryptedSecret
        secret = new EncryptedSecret("test", "1.0" ,"test-salt", "value", ImmutableMap.of("key1", "val1", "key2", "val2"));

    String phcString = secret.toPhcString();

    assertNotNull(phcString);
    assertThat(EncryptedSecret.parse(phcString), equalTo(secret));
  }

  @Test
  public void testCreatesParsePhcSecretWithoutVersion() {
    EncryptedSecret
        secret = new EncryptedSecret("test2", null ,"test-salt2", "value2", ImmutableMap.of("kv1", "v1", "kv2", "v2"));

    String phcString = secret.toPhcString();

    assertNotNull(phcString);
    assertThat(EncryptedSecret.parse(phcString), equalTo(secret));
  }

  @Test
  public void testParseInvalidPhcString() {
    String phcInvalid1 = "$AES$key1=val1,key2=val2,key3=val3$encryptedValue";
    String phcInvalid2 = "$this$is$a$test$failure";
    String phcInvalid3 = "";

    assertThrows(IllegalArgumentException.class, () -> EncryptedSecret.parse(phcInvalid1));
    assertThrows(IllegalArgumentException.class, () -> EncryptedSecret.parse(phcInvalid2));
    assertThrows(IllegalArgumentException.class, () -> EncryptedSecret.parse(phcInvalid3));

    //null phc string
    assertThrows(NullPointerException.class, () -> EncryptedSecret.parse(null));
  }
}
