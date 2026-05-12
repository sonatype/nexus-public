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
package org.sonatype.nexus.repository.content.error;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class MissingAssetExceptionTest
{
  @Test
  public void testBasicMessageContainsPath() {
    MissingAssetException underTest =
        new MissingAssetException("/path/to/asset.jar", "maven2", false, "my-repo", "my-component");

    assertThat(underTest.getMessage(), containsString("missing asset with path '/path/to/asset.jar'"));
    assertThat(underTest.getMessage(), not(containsString("staging moves")));
  }

  @Test
  public void testDockerWritableMemberIncludesExtraMessage() {
    MissingAssetException underTest =
        new MissingAssetException("/v2/image/manifests/latest", "docker", true, "docker-hosted", "image");

    assertThat(underTest.getMessage(), containsString("missing asset with path '/v2/image/manifests/latest'"));
    assertThat(underTest.getMessage(), containsString("staging moves are not supported"));
    assertThat(underTest.getMessage(), containsString("docker group repository"));
  }

  @Test
  public void testNonDockerWritableMemberDoesNotIncludeExtraMessage() {
    MissingAssetException underTest =
        new MissingAssetException("/path/asset.jar", "maven2", true, "maven-hosted", "my-component");

    assertThat(underTest.getMessage(), not(containsString("staging moves")));
  }

  @Test
  public void testDockerNonWritableMemberDoesNotIncludeExtraMessage() {
    MissingAssetException underTest =
        new MissingAssetException("/v2/image/manifests/latest", "docker", false, "docker-hosted", "image");

    assertThat(underTest.getMessage(), not(containsString("staging moves")));
  }

  @Test
  public void testGetters() {
    MissingAssetException underTest =
        new MissingAssetException("/path/to/asset", "raw", false, "my-repo", "my-comp");

    assertThat(underTest.getAssetPath(), is("/path/to/asset"));
    assertThat(underTest.getRepositoryName(), is("my-repo"));
    assertThat(underTest.getComponentName(), is("my-comp"));
  }

  @Test
  public void testGetDataReturnsMap() {
    MissingAssetException underTest =
        new MissingAssetException("/path/to/asset", "raw", false, "my-repo", "my-comp");

    Map<String, String> data = underTest.getData();
    assertThat(data, is(notNullValue()));
    assertThat(data.get("path"), is("/path/to/asset"));
    assertThat(data.get("component"), is("my-comp"));
    assertThat(data.get("repository"), is("my-repo"));
  }

  @Test
  public void testIsRuntimeException() {
    MissingAssetException underTest =
        new MissingAssetException("/path", "raw", false, "repo", "comp");

    assertThat(underTest instanceof RuntimeException, is(true));
  }
}
