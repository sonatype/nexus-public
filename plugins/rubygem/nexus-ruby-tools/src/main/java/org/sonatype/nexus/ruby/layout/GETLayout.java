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
package org.sonatype.nexus.ruby.layout;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.sonatype.nexus.ruby.ApiV1File;
import org.sonatype.nexus.ruby.BundlerApiFile;
import org.sonatype.nexus.ruby.DependencyData;
import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.DependencyHelper;
import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.GemArtifactIdDirectory;
import org.sonatype.nexus.ruby.GemFile;
import org.sonatype.nexus.ruby.GemspecFile;
import org.sonatype.nexus.ruby.MavenMetadataFile;
import org.sonatype.nexus.ruby.MavenMetadataSnapshotFile;
import org.sonatype.nexus.ruby.MetadataBuilder;
import org.sonatype.nexus.ruby.MetadataSnapshotBuilder;
import org.sonatype.nexus.ruby.PomFile;
import org.sonatype.nexus.ruby.RubygemsDirectory;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.Sha1Digest;
import org.sonatype.nexus.ruby.Sha1File;
import org.sonatype.nexus.ruby.SpecsIndexFile;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;

/**
 * a base layout for HTTP GET requests.
 *
 * <li>ensure the zipped specs.4.8 are in place before retrieving the specs.4.8 itself</li>
 * <li>generate the pom.xml from the associated gemspec file</li>
 * <li>attach the gem with the right platform the <code>GemArtifactFile</code>s</li>
 * <li>collect all <code>DependencyFile</code>s and merge them for the <code>BundlerApiFile</code> payload</li>
 * <li>generate the <code>Sha1File<code>s and <code>MavenMetadataFile</code>s on the fly</li>
 * <li>generate directory listing for some "virtual" directories</li>
 *
 * @author christian
 */
public class GETLayout
    extends DefaultLayout
    implements Layout
{
  protected final RubygemsGateway gateway;

  protected final Storage store;

  public GETLayout(RubygemsGateway gateway, Storage store) {
    this.gateway = gateway;
    this.store = store;
  }

  /**
   * this allows sub-classes to add more functionality, like creating an
   * empty SpecsIndexZippedFile.
   */
  protected void retrieveZipped(SpecsIndexZippedFile specs) {
    store.retrieve(specs);
  }

  @Override
  public SpecsIndexFile specsIndexFile(String name) {
    SpecsIndexFile specs = super.specsIndexFile(name);
    // just make sure we have a zipped file, i.e. create an empty one
    retrieveZipped(specs.zippedSpecsIndexFile());
    // now retrieve the unzipped one
    store.retrieve(specs);
    return specs;
  }

  @Override
  public SpecsIndexZippedFile specsIndexZippedFile(String name) {
    SpecsIndexZippedFile specs = super.specsIndexZippedFile(name);
    retrieveZipped(specs);
    return specs;
  }

  /**
   * subclasses can overwrite this, to collect the dependencies files differently. i.e.
   * a proxy might want to load only the missing or expired dependency files.
   *
   * @param file with the list of gem-names
   * @param deps the result set of <code>InputStream<code>s to all the <code>DependencyFile</code> of the
   *             given list of gem-names
   */
  protected void retrieveAll(BundlerApiFile file, DependencyHelper deps) throws IOException {
    for (String name : file.gemnames()) {
      try(InputStream is = store.getInputStream(dependencyFile(name))) {
        deps.add(is);
      }
    }
  }

  @Override
  public BundlerApiFile bundlerApiFile(String namesCommaSeparated) {
    BundlerApiFile file = super.bundlerApiFile(namesCommaSeparated);

    DependencyHelper deps = gateway.newDependencyHelper();
    try {
      retrieveAll(file, deps);
      if (!file.hasException()) {
        store.memory(deps.getInputStream(false), file);
      }
    }
    catch (IOException e) {
      file.setException(e);
    }
    return file;
  }

  @Override
  public RubygemsDirectory rubygemsDirectory(String path) {
    RubygemsDirectory dir = super.rubygemsDirectory(path);
    Directory d = directory("/api/v1/dependencies/");
    dir.setItems(store.listDirectory(d));
    // copy the error over to the original directory
    if (d.hasException()) {
      dir.setException(d.getException());
    }
    return dir;
  }

  @Override
  public GemArtifactIdDirectory gemArtifactIdDirectory(String path, String name,
                                                       boolean prereleases)
  {
    GemArtifactIdDirectory dir = super.gemArtifactIdDirectory(path, name, prereleases);
    try {
      dir.setItems(newDependencyData(dir.dependency()));
    }
    catch (IOException e) {
      dir.setException(e);
    }
    return dir;
  }

  @Override
  public MavenMetadataFile mavenMetadata(String name, boolean prereleased) {
    MavenMetadataFile file = super.mavenMetadata(name, prereleased);
    try {
      MetadataBuilder meta = new MetadataBuilder(newDependencyData(file.dependency()));
      meta.appendVersions(file.isPrerelease());
      store.memory(meta.toString(), file);
    }
    catch (IOException e) {
      file.setException(e);
    }

    return file;
  }

  @Override
  public MavenMetadataSnapshotFile mavenMetadataSnapshot(String name, String version) {
    MavenMetadataSnapshotFile file = super.mavenMetadataSnapshot(name, version);
    MetadataSnapshotBuilder meta = new MetadataSnapshotBuilder(name, version, store.getModified(file.dependency()));
    store.memory(meta.toString(), file);
    return file;
  }

  /**
   * generate the pom.xml and set it as payload to the given <code>PomFile</code>
   */
  protected void setPomPayload(PomFile file, boolean snapshot) {
    try {
      DependencyData dependencies = newDependencyData(file.dependency());
      if ("java".equals(dependencies.platform(file.version()))) {
        pomFromGem(file, snapshot, dependencies);
      }
      else {
        pomFromGemspec(file, snapshot, dependencies);
      }
    }
    catch (IOException e) {
      file.setException(e);
    }
  }

  private void pomFromGemspec(PomFile file, boolean snapshot, DependencyData dependencies) throws IOException {
    GemspecFile gemspec = file.gemspec(dependencies);
    if (gemspec.notExists()) {
      file.markAsNotExists();
    }
    else {
      try(InputStream is = store.getInputStream(gemspec)) {
        store.memory(gateway.newGemspecHelper(is).pom(snapshot), file);
      }
    }
  }

  private void pomFromGem(PomFile file, boolean snapshot, DependencyData dependencies) throws IOException {
    GemFile gem = file.gem(dependencies);
    if (gem.notExists()) {
      file.markAsNotExists();
    }
    else {
      try(InputStream is = store.getInputStream(gem)) {
        store.memory(gateway.newGemspecHelperFromGem(is).pom(snapshot), file);
      }
    }
  }

  /**
   * retrieve the gem with the right platform and attach it to the <code>GemArtifactFile</code>
   */
  protected void setGemArtifactPayload(GemArtifactFile file) {
    try {
      // the dependency-data is needed to find out
      // whether the gem has the default platform or the java platform
      GemFile gem = file.gem(newDependencyData(file.dependency()));
      if (gem == null) {
        file.markAsNotExists();
      }
      else {
        // retrieve the gem and set it as payload
        store.retrieve(gem);
        file.set(gem.get());
      }
    }
    catch (IOException e) {
      file.setException(e);
    }
  }

  @Override
  public PomFile pomSnapshot(String name, String version, String timestamp) {
    PomFile file = super.pomSnapshot(name, version, timestamp);
    setPomPayload(file, true);
    return file;
  }

  @Override
  public PomFile pom(String name, String version) {
    PomFile file = super.pom(name, version);
    setPomPayload(file, false);
    return file;
  }

  @Override
  public GemArtifactFile gemArtifactSnapshot(String name, String version, String timestamp) {
    GemArtifactFile file = super.gemArtifactSnapshot(name, version, timestamp);
    setGemArtifactPayload(file);
    return file;
  }

  @Override
  public GemArtifactFile gemArtifact(String name, String version) {
    GemArtifactFile file = super.gemArtifact(name, version);
    setGemArtifactPayload(file);
    return file;
  }

  @Override
  public Sha1File sha1(RubygemsFile file) {
    Sha1File sha = super.sha1(file);
    if (sha.notExists()) {
      return sha;
    }
    try (InputStream is = store.getInputStream(file)) {
      Sha1Digest digest = new Sha1Digest();
      int i = is.read();
      while (i != -1) {
        digest.update((byte) i);
        i = is.read();
      }
      store.memory(digest.hexDigest(), sha);
    }
    catch (IOException e) {
      sha.setException(e);
    }
    return sha;
  }

  /**
   * load all the dependency data into an object.
   */
  protected DependencyData newDependencyData(DependencyFile file) throws IOException {
    try(InputStream is = store.getInputStream(file)) {
      return gateway.newDependencyData(is, file.name(), store.getModified(file));
    }
  }

  @Override
  public GemFile gemFile(String name, String version, String platform) {
    GemFile gem = super.gemFile(name, version, platform);
    store.retrieve(gem);
    return gem;
  }

  @Override
  public GemFile gemFile(String filename) {
    GemFile gem = super.gemFile(filename);
    store.retrieve(gem);
    return gem;
  }

  @Override
  public GemspecFile gemspecFile(String name, String version, String platform) {
    GemspecFile gemspec = super.gemspecFile(name, version, platform);
    store.retrieve(gemspec);
    return gemspec;
  }

  @Override
  public GemspecFile gemspecFile(String filename) {
    GemspecFile gemspec = super.gemspecFile(filename);
    store.retrieve(gemspec);
    return gemspec;
  }

  @Override
  public ApiV1File apiV1File(String name) {
    ApiV1File file = super.apiV1File(name);
    if (!"api_key".equals(name)) {
      file.markAsForbidden();
    }
    return file;
  }
}
