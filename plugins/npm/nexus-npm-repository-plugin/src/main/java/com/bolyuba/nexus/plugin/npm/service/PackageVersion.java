/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class PackageVersion
    extends NpmJson
{
  /**
   * Key used in PackageRoot properties to place NX calculated SHA1 sum of tarball.
   */
  public static final String TARBALL_NX_SHASUM = "nx.shasum";

  /**
   * Creates a version specific key for SHA1, to be store in packageRoot properties (parent of all versions).
   */
  public static String createShasumVersionKey(final String version) {
    checkArgument(!Strings.isNullOrEmpty(version), "Version cannot be null or empty");
    return TARBALL_NX_SHASUM + "@" + version;
  }

  private final PackageRoot root;

  public PackageVersion(final PackageRoot root, final String repositoryId, final Map<String, Object> raw) {
    super(repositoryId, raw);
    this.root = checkNotNull(root);
  }

  public PackageRoot getRoot() { return root; }

  public String getName() {
    return (String) getRaw().get("name");
  }

  public String getDescription() {
    return (String) getRaw().get("description");
  }

  public String getVersion() {
    return (String) getRaw().get("version");
  }

  public String getDistTarball() {
    return (String) ((Map) getRaw().get("dist")).get("tarball");
  }

  @Nullable
  public String getDistShasum() {
    // TODO: shasum is not even mentioned by spec, but is mostly present?
    return (String) ((Map) getRaw().get("dist")).get("shasum");
  }

  public String getDistTarballFilename() {
    String tarballUrl = getDistTarball();
    int idx = tarballUrl.lastIndexOf("/");
    if (idx != -1) {
      return tarballUrl.substring(idx + 1);
    }
    else {
      //Unknown tarball, construct default
      return getName() + "-" + getVersion() + ".tgz";
    }
  }

  public void setDistTarball(String tarball) {
    checkNotNull(tarball);
    ((Map) getRaw().get("dist")).put("tarball", tarball);
  }

  public void setDistShasum(String shasum) {
    // TODO: shasum is not even mentioned by spec, but is mostly present?
    checkNotNull(shasum);
    ((Map) getRaw().get("dist")).put("shasum", shasum);
  }

  /**
   * Returns {@code true} if the {@code dist.tarball} property of this version is unknown. This means, that this
   * document originates most probably from a registry root listing, where version documents are not enlisted. To
   * make a version complete, it needs to be fetched from registry directly, or it's enclosing package root should be
   * fetched. Applies to proxied metadata only.
   */
  public boolean isIncomplete() {
    return "unknown".equals(getDistTarball());
  }

  // ==

  @Override
  protected void validate(final Map<String, Object> raw) {
    checkNotNull(raw);
    checkArgument(raw.containsKey("name"), "No mapping for 'name'");
    checkArgument(raw.get("name") instanceof String, "Mapping for 'name' is not a string");
    checkArgument(raw.containsKey("version"), "No mapping for 'version'");
    checkArgument(raw.get("version") instanceof String, "Mapping for 'version' is not a string");
    checkArgument(raw.containsKey("dist"), "No mapping for 'dist'");
    checkArgument(raw.get("dist") instanceof Map, "'dist' is not an object hash");
    checkArgument(((Map) raw.get("dist")).containsKey("tarball"), "No mapping for 'dist.tarball'");
    checkArgument(((Map) raw.get("dist")).get("tarball") instanceof String,
        "Mapping for 'dist.tarball' is not a string");
  }
}
