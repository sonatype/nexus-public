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
package org.sonatype.nexus.ruby;

import java.security.SecureRandom;

import org.sonatype.nexus.ruby.cuba.RootCuba;
import org.sonatype.nexus.ruby.cuba.api.ApiCuba;
import org.sonatype.nexus.ruby.cuba.api.ApiV1Cuba;
import org.sonatype.nexus.ruby.cuba.api.ApiV1DependenciesCuba;
import org.sonatype.nexus.ruby.cuba.gems.GemsCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenPrereleasesRubygemsArtifactIdCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenReleasesCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenReleasesRubygemsArtifactIdCuba;
import org.sonatype.nexus.ruby.cuba.quick.QuickCuba;
import org.sonatype.nexus.ruby.cuba.quick.QuickMarshalCuba;

public class DefaultRubygemsFileFactory
    implements RubygemsFileFactory
{
  public static final String ID = "DefaultRubygemsFileFactory";

  private static final String SEPARATOR = "/";

  private static final String GEMS = "/" + RootCuba.GEMS;

  private static final String QUICK_MARSHAL = "/" + RootCuba.QUICK + "/" + QuickCuba.MARSHAL_4_8;

  private static final String API_V1 = "/" + RootCuba.API + "/" + ApiCuba.V1;

  private static final String API_V1_DEPS = API_V1 + "/" + ApiV1Cuba.DEPENDENCIES;

  private static final String MAVEN_PRERELEASED_RUBYGEMS =
      "/" + RootCuba.MAVEN + "/" + MavenCuba.PRERELEASES + "/" + MavenReleasesCuba.RUBYGEMS;

  private static final String MAVEN_RELEASED_RUBYGEMS =
      "/" + RootCuba.MAVEN + "/" + MavenCuba.RELEASES + "/" + MavenReleasesCuba.RUBYGEMS;

  private final static SecureRandom random = new SecureRandom();

  static {
    random.setSeed(System.currentTimeMillis());
  }

  private String join(String... parts) {
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      builder.append(part);
    }
    return builder.toString();
  }

  private String toPath(String name, String version, String timestamp, boolean snapshot) {
    String v1 = snapshot ? version + "-" + timestamp : version;
    String v2 = snapshot ? version + MavenPrereleasesRubygemsArtifactIdCuba.SNAPSHOT : version;
    return join(snapshot ? MAVEN_PRERELEASED_RUBYGEMS : MAVEN_RELEASED_RUBYGEMS,
        SEPARATOR, name, SEPARATOR, v2, SEPARATOR, name + '-' + v1);
  }

  @Override
  public Sha1File sha1(RubygemsFile file) {
    return new Sha1File(this, file.storagePath() + ".sha1", file.remotePath() + ".sha1", file);
  }

  @Override
  public NoContentFile noContent(final String path) {
    return new NoContentFile(this, path);
  }

  @Override
  public NotFoundFile notFound(String path) {
    return new NotFoundFile(this, path);
  }

  @Override
  public PomFile pomSnapshot(String name, String version, String timestamp) {
    return new PomFile(this, toPath(name, version, timestamp, true) + ".pom", name, version, true);
  }

  @Override
  public GemArtifactFile gemArtifactSnapshot(String name, String version, String timestamp) {
    return new GemArtifactFile(this, toPath(name, version, timestamp, true) + ".gem", name, version, true);
  }

  @Override
  public PomFile pom(String name, String version) {
    return new PomFile(this, toPath(name, version, null, false) + ".pom", name, version, false);
  }

  @Override
  public GemArtifactFile gemArtifact(String name, String version) {
    return new GemArtifactFile(this, toPath(name, version, null, false) + ".gem", name, version, false);
  }

  @Override
  public MavenMetadataSnapshotFile mavenMetadataSnapshot(String name, String version) {
    String path = join(MAVEN_PRERELEASED_RUBYGEMS, SEPARATOR, name, SEPARATOR,
        version + MavenPrereleasesRubygemsArtifactIdCuba.SNAPSHOT,
        SEPARATOR, MavenReleasesRubygemsArtifactIdCuba.MAVEN_METADATA_XML);
    return new MavenMetadataSnapshotFile(this, path, name, version);
  }

  @Override
  public MavenMetadataFile mavenMetadata(String name, boolean prereleased) {
    String path = join(prereleased ? MAVEN_PRERELEASED_RUBYGEMS : MAVEN_RELEASED_RUBYGEMS,
        SEPARATOR, name, SEPARATOR, MavenReleasesRubygemsArtifactIdCuba.MAVEN_METADATA_XML);
    return new MavenMetadataFile(this, path, name, prereleased);
  }

  @Override
  public Directory directory(String path, String... items) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    return new Directory(this, path,
        // that is the name
        path.substring(0, path.length() - 1).replaceFirst(".*\\/", ""),
        items);
  }

  @Override
  public RubygemsDirectory rubygemsDirectory(String path) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    return new RubygemsDirectory(this, path);
  }

  @Override
  public GemArtifactIdDirectory gemArtifactIdDirectory(String path, String name, boolean prereleases) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    return new GemArtifactIdDirectory(this, path, name, prereleases);
  }

  @Override
  public Directory gemArtifactIdVersionDirectory(String path, String name, String version, boolean prerelease) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    return new GemArtifactIdVersionDirectory(this, path, name, version, prerelease);
  }

  @Override
  public GemFile gemFile(String name, String version, String platform) {
    String filename = BaseGemFile.toFilename(name, version, platform);
    return new GemFile(this,
        join(GEMS, SEPARATOR, name.substring(0, 1), SEPARATOR, filename, GemsCuba.GEM),
        join(GEMS, SEPARATOR, filename, GemsCuba.GEM),
        name, version, platform);
  }

  @Override
  public GemFile gemFile(String name) {
    return new GemFile(this,
        join(GEMS, SEPARATOR, name.substring(0, 1), SEPARATOR, name, GemsCuba.GEM),
        join(GEMS, SEPARATOR, name, GemsCuba.GEM),
        name);
  }

  @Override
  public GemspecFile gemspecFile(String name, String version, String platform) {
    String filename = BaseGemFile.toFilename(name, version, platform);
    return new GemspecFile(this,
        join(QUICK_MARSHAL, SEPARATOR, name.substring(0, 1), SEPARATOR, filename, QuickMarshalCuba.GEMSPEC_RZ),
        join(QUICK_MARSHAL, SEPARATOR, filename, QuickMarshalCuba.GEMSPEC_RZ),
        name, version, platform);
  }

  @Override
  public GemspecFile gemspecFile(String name) {
    return new GemspecFile(this,
        join(QUICK_MARSHAL, SEPARATOR, name.substring(0, 1), SEPARATOR, name, QuickMarshalCuba.GEMSPEC_RZ),
        join(QUICK_MARSHAL, SEPARATOR, name, QuickMarshalCuba.GEMSPEC_RZ),
        name);
  }

  @Override
  public DependencyFile dependencyFile(String name) {
    return new DependencyFile(this,
        join(API_V1_DEPS, SEPARATOR, name, ApiV1DependenciesCuba.RUBY),
        join(API_V1_DEPS, "?gems=" + name),
        name);
  }

  @Override
  public BundlerApiFile bundlerApiFile(String names) {
    // normalize query string first
    names = names.replaceAll("%2C", ",")
            .replaceAll(",,", ",")
            .replaceAll("\\s+", "")
            .replaceAll(",\\s*$", "");
    return new BundlerApiFile(this,
        join(API_V1_DEPS, "?gems=" + names),
        names.split(","));
  }

  @Override
  public BundlerApiFile bundlerApiFile(String... names) {
    StringBuilder gems = new StringBuilder("?gems=");
    boolean first = true;
    for (String name : names) {
      if (first) {
        first = false;
      }
      else {
        gems.append(",");
      }
      gems.append(name);
    }
    return new BundlerApiFile(this,
        join(API_V1_DEPS, gems.toString()),
        names);
  }

  @Override
  public ApiV1File apiV1File(String name) {
    return new ApiV1File(this,
        join(API_V1, SEPARATOR, Long.toString(Math.abs(random.nextLong())), ".", name),
        join(API_V1, SEPARATOR, name),
        name);
  }

  @Override
  public SpecsIndexFile specsIndexFile(SpecsIndexType type) {
    return this.specsIndexFile(type.filename().replace(RootCuba._4_8, ""));
  }

  @Override
  public SpecsIndexFile specsIndexFile(String name) {
    return new SpecsIndexFile(this, join(SEPARATOR, name, RootCuba._4_8), name);
  }

  @Override
  public SpecsIndexZippedFile specsIndexZippedFile(String name) {
    return new SpecsIndexZippedFile(this, join(SEPARATOR, name, RootCuba._4_8, RootCuba.GZ), name);
  }

  @Override
  public SpecsIndexZippedFile specsIndexZippedFile(SpecsIndexType type) {
    return this.specsIndexZippedFile(type.filename().replace(RootCuba._4_8, ""));
  }
}
