package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenSearchComponentPathFilterTest
    extends TestSupport
{
  private MavenSearchComponentPathFilter underTest;

  @Before
  public void setup() {
    underTest = new MavenSearchComponentPathFilter();
  }

  @Test
  public void shouldFilterMavenUncommonType() {
    String path = "foo/bar/foobar.jar.sha1";
    assertTrue(underTest.shouldFilterPathExtension(path));
  }

  @Test
  public void shouldNotFilterMavenCommonTypes() {
    validMavenPaths().forEach(path -> assertFalse(underTest.shouldFilterPathExtension(path)));
  }

  private List<String> validMavenPaths() {
    return asList("foo/bar/foobar.jar",
        "foo/bar/foobar.war",
        "foo/bar/foobar.aar",
        "foo/bar/foobar.zip",
        "foo/bar/foobar.pom",
        "foo/bar/foobar.tar.gz");
  }

}
