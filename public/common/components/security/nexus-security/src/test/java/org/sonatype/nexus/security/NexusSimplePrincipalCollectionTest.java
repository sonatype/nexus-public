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
package org.sonatype.nexus.security;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NexusSimplePrincipalCollectionTest
{
  private static final String PRINCIPAL = "bob";

  private static final String REALM = "ldap";

  private static NexusSimplePrincipalCollection legacy(final String principal, final String realm) {
    return new NexusSimplePrincipalCollection(principal, new RealmCaseMapping(realm, true));
  }

  @Test
  public void twoEquivalentInstancesAreEqualAndAgreeOnHashCode() {
    assertThat(legacy(PRINCIPAL, REALM), is(legacy(PRINCIPAL, REALM)));
    assertThat(legacy(PRINCIPAL, REALM).hashCode(), is(legacy(PRINCIPAL, REALM).hashCode()));
  }

  @Test
  public void equalsIsSymmetricWithSimplePrincipalCollection() {
    NexusSimplePrincipalCollection stored = legacy(PRINCIPAL, REALM);
    SimplePrincipalCollection sought = new SimplePrincipalCollection(PRINCIPAL, REALM);

    // both directions are asserted deliberately: the defect was an asymmetric equals
    assertTrue(stored.equals(sought));
    assertTrue(sought.equals(stored));
    assertThat(stored.hashCode(), is(sought.hashCode()));
  }

  @Test
  public void worksAsAHashMapKeyAcrossImplementations() {
    Map<PrincipalCollection, String> tokensByPrincipal = new HashMap<>();
    tokensByPrincipal.put(legacy(PRINCIPAL, REALM), "a-token");

    assertThat(tokensByPrincipal.get(new SimplePrincipalCollection(PRINCIPAL, REALM)), is("a-token"));
  }

  @Test
  public void differentRealmIsNotEqual() {
    assertThat(legacy(PRINCIPAL, REALM), is(not(legacy(PRINCIPAL, "local"))));
  }

  @Test
  public void differentPrincipalIsNotEqual() {
    assertThat(legacy(PRINCIPAL, REALM), is(not(legacy("alice", REALM))));
  }

  @Test
  public void caseMappingIsStillReadable() {
    NexusSimplePrincipalCollection caseInsensitive =
        new NexusSimplePrincipalCollection(PRINCIPAL, new RealmCaseMapping(REALM, false));

    assertThat(caseInsensitive.getRealmName(), is(REALM));
    assertFalse(caseInsensitive.isRealmCaseSensitive());
  }

  /**
   * Guards stream compatibility with {@code principals} BLOBs already in customer databases. This class declares no
   * fields beyond {@code realmCaseMapping}, so the serialized state never changed - but the default serialVersionUID is
   * derived from method signatures, and removing the {@code equals}/{@code hashCode} overrides moved it from
   * 6270148609559168170 to 4750169585322874540. That made every stored collection fail to deserialize with
   * {@code InvalidClassException}, breaking every user token on upgrade (NEXUS-54691). A round-trip test cannot catch
   * this, because it writes and reads with the same class version.
   */
  @Test
  public void serialVersionUidIsPinnedForPreExistingBlobs() {
    assertThat(ObjectStreamClass.lookup(NexusSimplePrincipalCollection.class).getSerialVersionUID(),
        is(6270148609559168170L));
  }

  /**
   * Bytes produced by serializing {@code new NexusSimplePrincipalCollection("p1", new RealmCaseMapping("r1", true))}
   * with the released, pre-NEXUS-54691 class - the one with {@code equals}/{@code hashCode} overrides and no explicit
   * {@code serialVersionUID} - to stand in for a {@code principals} BLOB already sitting in a customer database.
   */
  private static final String LEGACY_BLOB_BASE64 =
      "rO0ABXNyADpvcmcuc29uYXR5cGUubmV4dXMuc2VjdXJpdHkuTmV4dXNTaW1wbGVQcmluY2lwYWxDb2xsZWN0aW9uVwQK8JGvBKoCAAFMABBy"
          + "ZWFsbUNhc2VNYXBwaW5ndAAuTG9yZy9zb25hdHlwZS9uZXh1cy9zZWN1cml0eS9SZWFsbUNhc2VNYXBwaW5nO3hyADJvcmcuYXBhY2hl"
          + "LnNoaXJvLnN1YmplY3QuU2ltcGxlUHJpbmNpcGFsQ29sbGVjdGlvbqh/WCXGowhKAwABTAAPcmVhbG1QcmluY2lwYWxzdAAPTGphdmEv"
          + "dXRpbC9NYXA7eHBzcgAXamF2YS51dGlsLkxpbmtlZEhhc2hNYXA0wE5cEGzA+wIAAVoAC2FjY2Vzc09yZGVyeHIAEWphdmEudXRpbC5I"
          + "YXNoTWFwBQfawcMWYNEDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNob2xkeHA/QAAAAAAADHcIAAAAEAAAAAF0AAJyMXNyABdqYXZhLnV0"
          + "aWwuTGlua2VkSGFzaFNldNhs11qV3SoeAgAAeHIAEWphdmEudXRpbC5IYXNoU2V0ukSFlZa4tzQDAAB4cHcMAAAAED9AAAAAAAABdAAC"
          + "cDF4eAB3AQFxAH4AB3hzcgAsb3JnLnNvbmF0eXBlLm5leHVzLnNlY3VyaXR5LlJlYWxtQ2FzZU1hcHBpbmcxB5hpOXk+8QIAAloAD2lz"
          + "Q2FzZVNlbnNpdGl2ZUwACXJlYWxtTmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwAXEAfgAI";

  @Test
  public void deserializesABlobWrittenByTheReleasedPreChangeClass() throws Exception {
    byte[] bytes = Base64.getDecoder().decode(LEGACY_BLOB_BASE64);

    Object result;
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      result = in.readObject();
    }

    assertThat(result, is(legacy("p1", "r1")));
    // and it is interchangeable with what a modern login produces, per equalsIsSymmetricWithSimplePrincipalCollection
    assertThat(result, is(new SimplePrincipalCollection("p1", "r1")));
  }
}
