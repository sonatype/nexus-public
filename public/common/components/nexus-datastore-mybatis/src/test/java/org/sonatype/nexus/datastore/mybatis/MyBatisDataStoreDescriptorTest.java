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
package org.sonatype.nexus.datastore.mybatis;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.ADVANCED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MyBatisDataStoreDescriptorTest
{
  private MyBatisDataStoreDescriptor descriptor;

  @BeforeEach
  void setUp() {
    descriptor = new MyBatisDataStoreDescriptor();
  }

  @Test
  void nullAdvancedPassesSilently() {
    assertDoesNotThrow(() -> descriptor.validate(configWithAdvanced(null)));
  }

  @Test
  void emptyAdvancedPassesSilently() {
    assertDoesNotThrow(() -> descriptor.validate(configWithAdvanced("")));
  }

  @Test
  void blankAdvancedPassesSilently() {
    assertDoesNotThrow(() -> descriptor.validate(configWithAdvanced("   \n  \t  ")));
  }

  @Test
  void everyAllowedKeyPasses() {
    String allAllowed = String.join("\n",
        "maximumPoolSize=20",
        "minimumIdle=5",
        "connectionTimeout=30000",
        "idleTimeout=600000",
        "maxLifetime=1800000",
        "leakDetectionThreshold=0",
        "validationTimeout=5000",
        "autoCommit=true",
        "transactionIsolation=TRANSACTION_READ_COMMITTED",
        "poolName=my-pool",
        "keepaliveTime=30000",
        "initializationFailTimeout=120000");
    assertDoesNotThrow(() -> descriptor.validate(configWithAdvanced(allAllowed)));
  }

  @Test
  void connectionInitSqlIsRejected() {
    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("connectionInitSql=SELECT 1")));

    assertThat(thrown.getValidationErrors(), hasSize(1));
    ValidationErrorXO error = thrown.getValidationErrors().get(0);
    assertThat(error.getId(), equalTo(ADVANCED));
    assertThat(error.getMessage(), containsString("invalid or contains unknown"));
    assertThat(error.getMessage(), containsString("connectionInitSql"));
  }

  @Test
  void h2CreateAliasAttackPayloadIsRejected() {
    // This is the shape of the SVH-296 / H1 #3738993 exploit — a multi-line advanced value
    // that abuses HikariCP's connectionInitSql to run a CREATE ALIAS on the default H2 backend,
    // yielding OS-level code execution. Every disallowed key must show up in the rejection.
    String attack = "connectionInitSql=CREATE ALIAS EXEC AS 'String shellcode(String cmd) throws Exception "
        + "{ Runtime.getRuntime().exec(cmd); return cmd; }'\n"
        + "dataSourceClassName=org.h2.jdbcx.JdbcDataSource";

    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced(attack)));

    String message = thrown.getValidationErrors().get(0).getMessage();
    assertThat(message, containsString("invalid or contains unknown"));
    assertThat(message, containsString("connectionInitSql"));
    assertThat(message, containsString("dataSourceClassName"));
  }

  @Test
  void dataSourcePropertiesIsRejected() {
    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("dataSourceProperties=foo")));

    String message = thrown.getValidationErrors().get(0).getMessage();
    assertThat(message, containsString("invalid or contains unknown"));
    assertThat(message, containsString("dataSourceProperties"));
  }

  @Test
  void allowlistIsCaseSensitive() {
    // HikariCP itself is case-sensitive — a case-insensitive match here would open a bypass.
    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("ConnectionInitSql=SELECT 1")));

    String message = thrown.getValidationErrors().get(0).getMessage();
    assertThat(message, containsString("ConnectionInitSql"));
  }

  @Test
  void mixedInputRejectsOnlyTheDisallowedKey() {
    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("maximumPoolSize=20\nconnectionInitSql=SELECT 1")));

    String message = thrown.getValidationErrors().get(0).getMessage();
    assertThat(message, containsString("connectionInitSql"));
    assertThat(message, not(containsString("maximumPoolSize")));
  }

  @Test
  void driverClassNameIsRejected() {
    // driverClassName is one of the keys the descriptor's Javadoc calls out as specifically dangerous;
    // giving it its own regression test guards against a future edit that silently drops it from the deny path.
    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("driverClassName=org.h2.Driver")));

    String message = thrown.getValidationErrors().get(0).getMessage();
    assertThat(message, containsString("invalid or contains unknown"));
    assertThat(message, containsString("driverClassName"));
  }

  @Test
  void disallowedKeyWithColonSeparatorIsRejected() {
    // KEY_VALUE accepts both `=` and `:` as separators. The runtime path treats them equivalently,
    // so a disallowed key with `:` must be rejected the same way one with `=` is — otherwise the
    // colon syntax would be a bypass.
    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("connectionInitSql:SELECT 1")));

    String message = thrown.getValidationErrors().get(0).getMessage();
    assertThat(message, containsString("connectionInitSql"));
  }

  @Test
  void valueContainingEqualsSignParsesAsKeyPlusRestOfLine() {
    // KEY_VALUE is limit(2), so `k=v=x` splits into (k, "v=x") — the second `=` stays in the
    // value. If the splitter ever loses that limit, a value like `connectionInitSql=SELECT 1=2`
    // could confuse validation. Here we simultaneously prove:
    // (a) the allowlisted key with an equals-in-value is accepted (no false positive), and
    // (b) the disallowed key with an equals-in-value is still rejected (no bypass via extra `=`).
    assertDoesNotThrow(
        () -> descriptor.validate(configWithAdvanced("transactionIsolation=TRANSACTION_READ_COMMITTED=ignored")));

    ValidationErrorsException thrown = assertThrows(ValidationErrorsException.class,
        () -> descriptor.validate(configWithAdvanced("connectionInitSql=SELECT 1=2")));
    assertThat(thrown.getValidationErrors().get(0).getMessage(), containsString("connectionInitSql"));
  }

  @Test
  void parserAcceptsColonSeparatorWhitespaceAndBlankLinesIdenticallyToRuntime() {
    // MyBatisDataStore#configureHikari feeds advanced through the same TO_MAP splitter, which
    // accepts `=` or `:` as separators, trims whitespace, and drops blank lines. The validator
    // MUST see the same key set the runtime would inject; anything else is a parser-differential
    // bypass. Every key below is in the allowlist, so this call must not throw.
    String mixed = "\n"
        + "  maximumPoolSize = 20  \n"
        + "\n"
        + "minimumIdle : 5\n"
        + "  \n"
        + "poolName=my-pool\n";
    assertDoesNotThrow(() -> descriptor.validate(configWithAdvanced(mixed)));
  }

  private static DataStoreConfiguration configWithAdvanced(final String advanced) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    Map<String, String> attributes = new HashMap<>();
    if (advanced != null) {
      attributes.put(ADVANCED, advanced);
    }
    config.setAttributes(attributes);
    return config;
  }
}
