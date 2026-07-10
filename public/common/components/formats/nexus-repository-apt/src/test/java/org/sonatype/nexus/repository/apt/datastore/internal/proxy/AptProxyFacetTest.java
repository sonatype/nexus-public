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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import java.io.UnsupportedEncodingException;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.template.EscapeHelper;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.proxy.EncodingHelper;
import org.sonatype.nexus.repository.view.Context;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AptProxyFacet}
 */
public class AptProxyFacetTest
{
  private TestableAptProxyFacet underTest;

  private AttributesMap attributes;

  private Context context;

  /**
   * Test-friendly subclass that exposes protected initialization methods
   */
  private static class TestableAptProxyFacet
      extends AptProxyFacet
  {
    private EncodingHelper testEncodingHelper;

    public void initializeForTest() {
      // Call the protected method from ProxyFacetSupport to initialize escapeHelper
      configureUrlEscapeRules(null);
    }

    public void initializeWithEncodingHelper(EncodingHelper encodingHelper) {
      configureUrlEscapeRules(null);
      this.testEncodingHelper = encodingHelper;
    }

    @Override
    protected EncodingHelper getEncodingHelper() {
      return testEncodingHelper;
    }

    public String testEncodeUrl(String url) throws UnsupportedEncodingException {
      return encodeUrl(url);
    }
  }

  @Before
  public void setup() {
    underTest = new TestableAptProxyFacet();
    underTest.initializeForTest();

    attributes = new AttributesMap();
    context = mock(Context.class);
    when(context.getAttributes()).thenReturn(attributes);
  }

  @Test
  public void testGetUrl_returnsRawPathWithSpaces() {
    // getUrl() returns raw path
    attributes.set(AptSnapshotHandler.State.class, new AptSnapshotHandler.State("dists/path with spaces/Release"));

    String url = underTest.getUrl(context);

    // Should return raw path
    assertThat(url).isEqualTo("dists/path with spaces/Release");
  }

  @Test
  public void testGetUrl_returnsRawPathWithPercent() {
    // getUrl() returns raw path
    attributes.set(AptSnapshotHandler.State.class, new AptSnapshotHandler.State("dists/path%test/Release"));

    String url = underTest.getUrl(context);

    // Should return raw path
    assertThat(url).isEqualTo("dists/path%test/Release");
  }

  @Test
  public void testGetUrl_normalPath() {
    // Test normal path without special characters
    attributes.set(AptSnapshotHandler.State.class,
        new AptSnapshotHandler.State("dists/xenial/main/binary-amd64/Packages"));

    String url = underTest.getUrl(context);

    // Should remain unchanged
    assertThat(url).isEqualTo("dists/xenial/main/binary-amd64/Packages");
  }

  @Test
  public void testGetUrl_kubernetesStylePath() {
    // Real-world example from Kubernetes repos (pkgs.k8s.io)
    attributes.set(AptSnapshotHandler.State.class,
        new AptSnapshotHandler.State("dists/core:/stable:/v1.31/main/binary-amd64/Packages.gz"));

    String url = underTest.getUrl(context);

    // Returns raw path
    assertThat(url).isEqualTo("dists/core:/stable:/v1.31/main/binary-amd64/Packages.gz");
  }

  @Test
  public void testGetUrl_openSUSEStylePath() {
    // OpenSUSE also uses colons in paths
    attributes.set(AptSnapshotHandler.State.class, new AptSnapshotHandler.State("dists/15.5:/main/Release"));

    String url = underTest.getUrl(context);

    // Returns raw path
    assertThat(url).isEqualTo("dists/15.5:/main/Release");
  }

  @Test
  public void testGetUrl_doesNotEncodeSlashes() {
    // Slashes are path separators and must NOT be encoded
    attributes.set(AptSnapshotHandler.State.class,
        new AptSnapshotHandler.State("dists/path/to/package/Release"));

    String url = underTest.getUrl(context);

    // Slashes should remain as /
    assertThat(url).isEqualTo("dists/path/to/package/Release");
  }

  @Test
  public void testGetUrl_multipleConsecutiveSpecialChars() {
    // Multiple consecutive colons
    attributes.set(AptSnapshotHandler.State.class,
        new AptSnapshotHandler.State("dists/path::with:::colons/Release"));

    String url = underTest.getUrl(context);

    // getUrl() now returns raw path - encoding happens in encodeUrl()
    assertThat(url).isEqualTo("dists/path::with:::colons/Release");
  }

  @Test
  public void testEncodeUrl_encodesColonsInLegacyMode() throws UnsupportedEncodingException {
    // In legacy mode (EncodingHelper is null), encodeUrl() should encode
    String encoded = underTest.testEncodeUrl("dists/core:/stable:/v1.31/Release");

    // Colons should be encoded as %3A
    assertThat(encoded).isEqualTo("dists/core%3A/stable%3A/v1.31/Release");
  }

  @Test
  public void testEncodeUrl_encodesSpacesInLegacyMode() throws UnsupportedEncodingException {
    // In legacy mode, spaces should be encoded
    String encoded = underTest.testEncodeUrl("dists/path with spaces/Release");

    // Spaces should be encoded as %20
    assertThat(encoded).isEqualTo("dists/path%20with%20spaces/Release");
  }

  @Test
  public void testEncodeUrl_encodesPercentInLegacyMode() throws UnsupportedEncodingException {
    // In legacy mode, percent signs should be encoded
    String encoded = underTest.testEncodeUrl("dists/path%test/Release");

    // Percent should be encoded as %25
    assertThat(encoded).isEqualTo("dists/path%25test/Release");
  }

  @Test
  public void testEncodeUrl_returnsRawPathInNewMode() throws UnsupportedEncodingException {
    // Test the critical new-mode path: when EncodingHelper is active,
    // encodeUrl() must return the URL unchanged to prevent double-encoding
    EscapeHelper escapeHelper = new EscapeHelper();
    EncodingHelper encodingHelper = new EncodingHelper(escapeHelper);

    TestableAptProxyFacet facetWithEncodingHelper = new TestableAptProxyFacet();
    facetWithEncodingHelper.initializeWithEncodingHelper(encodingHelper);

    // When EncodingHelper is active, encodeUrl() should NOT encode
    String result = facetWithEncodingHelper.testEncodeUrl("dists/core:/stable:/v1.31/Release");

    // Should return raw path unchanged (EncodingHelper already encoded in Stage 1)
    assertThat(result).isEqualTo("dists/core:/stable:/v1.31/Release");
  }

  @Test
  public void testEncodeUrl_returnsRawPathInNewModePreserveEncoded() throws UnsupportedEncodingException {
    // Test new mode — EncodingHelper always preserves encoded characters
    EscapeHelper escapeHelper = new EscapeHelper();
    EncodingHelper encodingHelper = new EncodingHelper(escapeHelper);

    TestableAptProxyFacet facetWithEncodingHelper = new TestableAptProxyFacet();
    facetWithEncodingHelper.initializeWithEncodingHelper(encodingHelper);

    // When EncodingHelper is active (regardless of preserveEncodedCharacters setting),
    // encodeUrl() should NOT encode
    String result = facetWithEncodingHelper.testEncodeUrl("dists/core:/stable:/v1.31/Release");

    // Should return raw path unchanged
    assertThat(result).isEqualTo("dists/core:/stable:/v1.31/Release");
  }

  @Test
  public void testGetUrl_withLeadingSlash() {
    // Test path with leading slash
    attributes.set(AptSnapshotHandler.State.class, new AptSnapshotHandler.State("/dists/xenial/Release"));

    String url = underTest.getUrl(context);

    // Should preserve leading slash (URI.resolve() will treat as absolute path)
    assertThat(url).isEqualTo("/dists/xenial/Release");
  }
}
