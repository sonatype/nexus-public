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
package org.sonatype.nexus.repository.cocoapods.internal.proxy;

import java.net.URI;

import org.sonatype.nexus.repository.cocoapods.internal.pod.PodPathProvider;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @since 3.19
 */
public class SpecTransformerTest
{
  private PodPathProvider podPathProvider =
      new PodPathProvider("https://api.github.com", "https://bitbucket.org", "https://gitlab.com");

  @Test
  public void gitHubToProxiedSpecPositiveTest() throws Exception {
    String spec = "{" +
        "\"name\": \"MasonryHidden\"," +
        "\"version\": \"1.0.0\"," +
        "\"source\": {\n" +
        "\"git\": \"https://github.com/SunnySunning/MasonryHidden.git\"," +
        "\"tag\": \"0.5.0\"" +
        "}" +
        "}";

    String transformedSpec = "{\n" +
        "  \"name\" : \"MasonryHidden\",\n" +
        "  \"version\" : \"1.0.0\",\n" +
        "  \"source\" : {\n" +
        "    \"http\" : \"http://repouri/pods/MasonryHidden/1.0.0/https/api.github.com/repos/SunnySunning/MasonryHidden/tarball/0.5.0.tar.gz\"\n" +
        "  }\n" +
        "}";

    URI repoUri = URI.create("http://repouri/");

    String res = new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
    assertThat(res, is(transformedSpec));
  }

  @Test
  public void gitHubToProxiedSpecInvalidTagTest() throws Exception {
    String spec = "{" +
        "\"name\": \"MasonryHidden\"," +
        "\"version\": \"1.0.0\"," +
        "\"source\": {\n" +
        "\"git\": \"https://github.com/SunnySunning/MasonryHidden.git\"," +
        "\"tag\": 0.01" +
        "}" +
        "}";

    String transformedSpec = "{\n" +
        "  \"name\" : \"MasonryHidden\",\n" +
        "  \"version\" : \"1.0.0\",\n" +
        "  \"source\" : {\n" +
        "    \"http\" : \"http://repouri/pods/MasonryHidden/1.0.0/https/api.github.com/repos/SunnySunning/MasonryHidden/tarball/.tar.gz\"\n" +
        "  }\n" +
        "}";

    URI repoUri = URI.create("http://repouri/");

    String res = new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
    assertThat(res, is(transformedSpec));
  }

  @Test
  public void httpToProxiedSpecTest() throws Exception {
    String spec = "{" +
        "\"name\": \"AppSpectorTVSDK\"," +
        "\"version\": \"1.0.0\"," +
        "\"source\": {\n" +
        "\"http\": \"https://github.com/appspector/ios-sdk/blob/master/AppSpectorTVSDK.zip?raw=true\"" +
        "}" +
        "}";

    String transformedSpec = "{\n" +
        "  \"name\" : \"AppSpectorTVSDK\",\n" +
        "  \"version\" : \"1.0.0\",\n" +
        "  \"source\" : {\n" +
        "    \"http\" : \"http://repouri/pods/AppSpectorTVSDK/1.0.0/https/github.com/appspector/ios-sdk/blob/master/AppSpectorTVSDK.zip?raw=true\"\n" +
        "  }\n" +
        "}";

    URI repoUri = URI.create("http://repouri/");

    String res = new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
    assertThat(res, is(transformedSpec));
  }

  @Test(expected = InvalidSpecFileException.class)
  public void gitHubToProxiedSpecInvalidGitUriTest() throws Exception {
    String spec = "{" +
        "\"name\": \"MasonryHidden\"," +
        "\"version\": \"1.0.0\"," +
        "\"source\": {\n" +
        "\"git\": \"git@gitlab.gbksoft.net:gbksoft-mobile-department/ios/gbkslidemenu.git\"," +
        "\"tag\": \"0.5.0\"" +
        "}" +
        "}";

    URI repoUri = URI.create("http://repouri/");

    new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
  }

  @Test(expected = InvalidSpecFileException.class)
  public void testInvalidJson() throws Exception {
    new SpecTransformer(podPathProvider).toProxiedSpec("invalid_json", URI.create("http://repouri/"));
  }

  @Test(expected = InvalidSpecFileException.class)
  public void gitHubToProxiedSpecNoSourceTest() throws Exception {
    String spec = "{\"name\": \"MasonryHidden\",\"version\": \"1.0.0\"}";

    URI repoUri = URI.create("http://repouri/");

    new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
  }

  @Test(expected = InvalidSpecFileException.class)
  public void gitHubToProxiedSpecNoNameTest() throws Exception {
    String spec = "{" +
        "\"version\": \"1.0.0\"," +
        "\"source\": {\n" +
        "\"git\": \"git@gitlab.gbksoft.net:gbksoft-mobile-department/ios/gbkslidemenu.git\"," +
        "\"tag\": \"0.5.0\"" +
        "}" +
        "}";

    URI repoUri = URI.create("http://repouri/");

    new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
  }

  @Test(expected = InvalidSpecFileException.class)
  public void gitHubToProxiedSpecNoVersionTest() throws Exception {
    String spec = "{" +
        "\"name\": \"MasonryHidden\"," +
        "\"source\": {\n" +
        "\"git\": \"git@gitlab.gbksoft.net:gbksoft-mobile-department/ios/gbkslidemenu.git\"," +
        "\"tag\": \"0.5.0\"" +
        "}" +
        "}";

    URI repoUri = URI.create("http://repouri/");

    new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
  }


  @Test
  public void gitHubToProxiedSpecVersionIncludeNonPrintableCharactersTest() throws Exception {
    String spec = "{\n" +
      "  \"name\": \"Realm\",\n" +
      "  \"version\": \"0.92.3\\n\",\n" +
      "  \"source\": {\n" +
      "    \"git\": \"https://github.com/realm/realm-cocoa.git\",\n" +
      "    \"tag\": \"v0.92.3\"\n" +
      "  }\n" +
      "}";

    URI repoUri = URI.create("http://repouri/");

    String res = new SpecTransformer(podPathProvider).toProxiedSpec(spec, repoUri);
    assertThat(res.contains("/Realm/0.92.3/"), is(true));
  }
}
