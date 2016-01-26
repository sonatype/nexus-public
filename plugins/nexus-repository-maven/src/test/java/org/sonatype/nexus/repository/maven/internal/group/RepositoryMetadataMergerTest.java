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
package org.sonatype.nexus.repository.maven.internal.group;

import java.util.Arrays;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.internal.group.RepositoryMetadataMerger.Envelope;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.fest.util.Strings;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * UT for {@link RepositoryMetadataMerger}
 *
 * @since 3.0
 */
public class RepositoryMetadataMergerTest
    extends TestSupport
{
  private final RepositoryMetadataMerger merger = new RepositoryMetadataMerger();

  private Plugin plugin(String name) {
    final Plugin p = new Plugin();
    p.setPrefix(name);
    p.setArtifactId(name + "-maven-plugin");
    p.setName("The " + name + " plugin");
    return p;
  }

  private Metadata g(final String... pluginNames)
  {
    final Metadata m = new Metadata();
    for (String pluginName : pluginNames) {
      m.addPlugin(plugin(pluginName));
    }
    return m;
  }

  private Metadata a(final String groupId,
                     final String artifactId,
                     final String lastUpdated,
                     final String latest,
                     final String release,
                     final String... versions)
  {
    final Metadata m = new Metadata();
    m.setGroupId(groupId);
    m.setArtifactId(artifactId);
    m.setVersioning(new Versioning());
    final Versioning mv = m.getVersioning();
    if (!Strings.isNullOrEmpty(lastUpdated)) {
      mv.setLastUpdated(lastUpdated);
    }
    if (!Strings.isNullOrEmpty(latest)) {
      mv.setLatest(latest);
    }
    if (!Strings.isNullOrEmpty(release)) {
      mv.setRelease(release);
    }
    mv.getVersions().addAll(Arrays.asList(versions));
    return m;
  }

  private Metadata v(final String groupId,
                     final String artifactId,
                     final String versionPrefix,
                     final String timestamp,
                     final int buildNumber)
  {
    final Metadata m = new Metadata();
    m.setGroupId(groupId);
    m.setArtifactId(artifactId);
    m.setVersion(versionPrefix + "-SNAPSHOT");
    m.setVersioning(new Versioning());
    final Versioning mv = m.getVersioning();
    mv.setLastUpdated(timestamp.replace(".", ""));
    final Snapshot snapshot = new Snapshot();
    snapshot.setTimestamp(timestamp);
    snapshot.setBuildNumber(buildNumber);
    mv.setSnapshot(snapshot);
    final SnapshotVersion pom = new SnapshotVersion();
    pom.setExtension("pom");
    pom.setVersion(versionPrefix + "-" + timestamp + "-" + buildNumber);
    pom.setUpdated(timestamp);
    mv.getSnapshotVersions().add(pom);

    final SnapshotVersion jar = new SnapshotVersion();
    jar.setExtension("jar");
    jar.setVersion(versionPrefix + "-" + timestamp + "-" + buildNumber);
    jar.setUpdated(timestamp);
    mv.getSnapshotVersions().add(jar);

    final SnapshotVersion sources = new SnapshotVersion();
    sources.setExtension("jar");
    sources.setClassifier("sources");
    sources.setVersion(versionPrefix + "-" + timestamp + "-" + buildNumber);
    sources.setUpdated(timestamp);
    mv.getSnapshotVersions().add(sources);

    return m;
  }

  @Test
  public void groupLevelMd() throws Exception {
    final Metadata m1 = g("foo");
    final Metadata m2 = g("foo", "bar");
    final Metadata m3 = g("baz");

    final Metadata m = merger.merge(
        ImmutableList.of(new Envelope("1", m1), new Envelope("2", m2), new Envelope("3", m3))
    );
    assertThat(m, notNullValue());
    assertThat(m.getModelVersion(), equalTo("1.1.0"));
    assertThat(m.getPlugins(), hasSize(3));
    final List<String> prefixes = Lists.newArrayList(Iterables.transform(m.getPlugins(), new Function<Plugin, String>()
    {
      @Override
      public String apply(final Plugin input) {
        return input.getArtifactId();
      }
    }));
    assertThat(prefixes, containsInAnyOrder("foo-maven-plugin", "bar-maven-plugin", "baz-maven-plugin"));
  }

  @Test
  public void artifactLevelMd() throws Exception {
    final Metadata m1 = a("org.foo", "some-project", "20150324121500", "1.0.1", "1.0.1", "1.0.0", "1.0.1");
    final Metadata m2 = a("org.foo", "some-project", "20150324121700", "1.0.2", "1.0.2", "1.0.2");
    final Metadata m3 = a("org.foo", "some-project", "20150324121600", "1.1.0-SNAPSHOT", null, "1.1.0-SNAPSHOT");

    final Metadata m = merger.merge(
        ImmutableList.of(new Envelope("1", m1), new Envelope("2", m2), new Envelope("3", m3))
    );
    assertThat(m, notNullValue());
    assertThat(m.getModelVersion(), equalTo("1.1.0"));
    assertThat(m.getGroupId(), equalTo("org.foo"));
    assertThat(m.getArtifactId(), equalTo("some-project"));
    assertThat(m.getVersioning().getLastUpdated(), equalTo("20150324121700"));
    assertThat(m.getVersioning().getSnapshot(), nullValue());
    assertThat(m.getVersioning().getRelease(), equalTo("1.0.2"));
    assertThat(m.getVersioning().getLatest(), equalTo("1.1.0-SNAPSHOT"));
    assertThat(m.getVersioning().getVersions(), contains("1.0.0", "1.0.1", "1.0.2", "1.1.0-SNAPSHOT"));
  }

  @Test
  public void versionLevelMd() throws Exception {
    final Metadata m1 = v("org.foo", "some-project", "1.0.0", "20150324.121500", 3);
    final Metadata m2 = v("org.foo", "some-project", "1.0.0", "20150323.121500", 2);
    final Metadata m3 = v("org.foo", "some-project", "1.0.0", "20150322.121500", 1);

    final Metadata m = merger.merge(
        ImmutableList.of(new Envelope("1", m1), new Envelope("2", m2), new Envelope("3", m3))
    );
    assertThat(m, notNullValue());
    assertThat(m.getModelVersion(), equalTo("1.1.0"));
    assertThat(m.getGroupId(), equalTo("org.foo"));
    assertThat(m.getArtifactId(), equalTo("some-project"));
    assertThat(m.getVersioning().getLastUpdated(), equalTo("20150324121500"));
    assertThat(m.getVersioning().getSnapshot(), notNullValue());
    assertThat(m.getVersioning().getSnapshot().getTimestamp(), equalTo("20150324.121500"));
    assertThat(m.getVersioning().getSnapshot().getBuildNumber(), equalTo(3));
  }

  @Test
  public void mixedLevelMd() throws Exception {
    final Metadata m1 = a("org.foo", "some-project", "20150324121500", "1.0.1", "1.0.1", "1.0.0", "1.0.1");
    final Metadata m2 = g("foo", "bar");
    final Metadata m3 = v("org.foo", "some-project", "1.1.0", "20150322.121500", 3);

    final Metadata m = merger.merge(
        ImmutableList.of(new Envelope("1", m1), new Envelope("2", m2), new Envelope("3", m3))
    );
    assertThat(m, notNullValue());
    assertThat(m.getModelVersion(), equalTo("1.1.0"));
    assertThat(m.getGroupId(), equalTo("org.foo"));
    assertThat(m.getArtifactId(), equalTo("some-project"));
    assertThat(m.getVersion(), equalTo("1.1.0-SNAPSHOT"));
    assertThat(m.getVersioning().getLastUpdated(), equalTo("20150324121500"));
    assertThat(m.getVersioning().getSnapshot(), notNullValue());
    assertThat(m.getVersioning().getSnapshot().getTimestamp(), equalTo("20150322.121500"));
    assertThat(m.getVersioning().getSnapshot().getBuildNumber(), equalTo(3));
    assertThat(m.getVersioning().getRelease(), equalTo("1.0.1"));
    assertThat(m.getVersioning().getLatest(), equalTo("1.0.1"));
    assertThat(m.getVersioning().getVersions(), contains("1.0.0", "1.0.1"));
    assertThat(m.getPlugins(), hasSize(2));
    final List<String> prefixes = Lists.newArrayList(Iterables.transform(m.getPlugins(), new Function<Plugin, String>()
    {
      @Override
      public String apply(final Plugin input) {
        return input.getArtifactId();
      }
    }));
    assertThat(prefixes, containsInAnyOrder("foo-maven-plugin", "bar-maven-plugin"));
  }
}
