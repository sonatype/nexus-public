package org.sonatype.nexus.repository.search.normalize;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionNumberExpanderTest
{
  @Test
  public void testBlank() {
    assertEquals("", VersionNumberExpander.expand(null));
    assertEquals("", VersionNumberExpander.expand(""));
    assertEquals("", VersionNumberExpander.expand(" "));
  }

  @Test
  public void noExpansion() {
    assertEquals("alpha", VersionNumberExpander.expand("alpha"));
    assertEquals("beta release", VersionNumberExpander.expand("beta release"));
  }

  @Test
  public void numbers() {
    assertEquals("000000001", VersionNumberExpander.expand("1"));
    assertEquals("000023007", VersionNumberExpander.expand("23007"));
    assertEquals("000000001.000000000.000000002", VersionNumberExpander.expand("1.0.2"));
    assertEquals("000000001.000000023.000000004", VersionNumberExpander.expand("1.23.4"));
  }

  @Test
  public void mixedText() {
    assertEquals("000000001alpha", VersionNumberExpander.expand("1alpha"));
    assertEquals("beta-000000002", VersionNumberExpander.expand("beta-2"));
    assertEquals("000000001.000000000a000000004", VersionNumberExpander.expand("1.0a4"));
    assertEquals("beta-000000001.000000023-alpha000000004-snapshot", VersionNumberExpander.expand("beta-1.23-alpha4-snapshot"));
  }
}
