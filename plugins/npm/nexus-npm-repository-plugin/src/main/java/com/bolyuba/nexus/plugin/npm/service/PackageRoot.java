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
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class PackageRoot
    extends NpmJson
{
  public static final String PROP_CACHED = "remote.cached";

  public static final String PROP_EXPIRED = "remote.expired";

  public static final String PROP_ETAG = "remote.etag";

  private final Map<String, String> properties;

  private final Map<String, PackageVersion> wrappedVersions;

  private final Map<String, NpmBlob> attachments;

  public PackageRoot(final String repositoryId, final Map<String, Object> raw) {
    super(repositoryId, raw);
    this.properties = Maps.newHashMap();
    this.wrappedVersions = Maps.newHashMap();
    this.wrappedVersions.putAll(wrapVersions(raw));
    this.attachments = Maps.newHashMap();
  }

  public Map<String, String> getProperties() { return properties; }

  public String getComponentId() {return getRepositoryId() + ":" + getName(); }

  public String getName() {
    return (String) getRaw().get("name");
  }

  public String getDescription() {
    return (String) getRaw().get("description");
  }

  public Map<String, PackageVersion> getVersions() {
    return wrappedVersions;
  }

  public Map<String, NpmBlob> getAttachments() { return attachments; }

  /**
   * Maintains the "time" object of this document. This method depends on {@link #wrappedVersions} variable and
   * assumes is up to date. Hence, this method should be invoked only AFTER it's ensured that wrapped versions
   * are updated.
   * Rules applied:
   * <ul>
   * <li>if no "created" key found, add it with "now" value</li>
   * <li>add "modified" key with "now" value</li>
   * <li>scan all versions, and any version not found in time map, add with "now"</li>
   * </ul>
   * Note: this method modifies the document, hence, should be used ONLY when it's okay to do so, as in hosted
   * repositories. Proxied documents should be stored as-is, hence, should NOT be stored once modified with this
   * method!
   */
  public void maintainTime() {
    if (!getRaw().containsKey("time")) {
      getRaw().put("time", Maps.newHashMap());
    }
    final Map<String, String> time = (Map<String, String>) getRaw().get("time");
    final String now =
        new DateTime(DateTimeZone.UTC).toString(); // TZ must be UTC! Hence, Goodies' ISO8601 does not work
    if (!time.containsKey("created")) {
      time.put("created", now);
    }
    time.put("modified", now);
    for (String version : wrappedVersions.keySet()) {
      if (!time.containsKey(version)) {
        time.put(version, now);
      }
    }
  }

  /**
   * Reduces the document backing map to a "shrinked" form where versions map contains only version to tag (or version)
   * mapping (basically dithching the package version object, making the versions map string-string map). This method
   * and format is usable only in registry root metadata and nowhere else should be used. Document modified like this
   * should NOT be persisted back into metadata store.
   */
  public void shrinkPackageVersions() {
    if (isUnpublished()) {
      return;
    }
    // replace versions map(versionString, packageVersionObject) with map(versionString, tagString)
    final Map<String, String> shrinkedVersions = Maps.newHashMap();
    for (Entry<String, PackageVersion> version : wrappedVersions.entrySet()) {
      shrinkedVersions.put(version.getKey(), resolveVersionToTag(version.getKey()));
    }
    ((Map<String, Object>) getRaw().get("versions")).clear();
    ((Map<String, Object>) getRaw().get("versions")).putAll(shrinkedVersions);
    wrappedVersions.clear();
  }

  /**
   * Returns {@code true} if the package is "unreleased" from registry, basically is deleted. In this case
   * the registry retains it's metadata but enlists new versions at all.
   */
  public boolean isUnpublished() {
    return wrappedVersions.isEmpty();
  }

  /**
   * Returns {@code true} if any of the versions in this package root are incomplete. Applies to proxied metadata only.
   *
   * @see PackageVersion#isIncomplete()
   */
  public boolean isIncomplete() {
    if (isUnpublished()) {
      return false;
    }
    for (PackageVersion pv : getVersions().values()) {
      if (pv.isIncomplete()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Overlays given package root onto this package root, probably changing the mappings, or merging some maps. It also
   * maintains inner state of this document (wrappedVersions and attachments). This method also enforces that only
   * same origin roots may be overlaid by {@link #getComponentId()} equality check.
   *
   * @see #overlayIgnoringOrigin(PackageRoot)
   */
  public void overlay(final PackageRoot packageRoot) {
    checkArgument(getComponentId().equals(packageRoot.getComponentId()), "Cannot overlay different package roots!");
    overlayIgnoringOrigin(packageRoot);
  }

  /**
   * Overlays given package root onto this package root, probably changing the mappings, or merging some maps. It also
   * maintains inner state of this document (wrappedVersions and attachments). This method ignores package origin.
   */
  public void overlayIgnoringOrigin(final PackageRoot packageRoot) {
    overlay(getRaw(), packageRoot.getRaw()); // this changes underlying raw map directly
    getProperties().putAll(packageRoot.getProperties()); // this is "shallow" string-string map
    this.wrappedVersions.clear();
    this.wrappedVersions.putAll(wrapVersions(getRaw()));
    this.attachments.clear(); // TODO: is clear needed?
    this.attachments.putAll(packageRoot.getAttachments());
  }

  private Map<String, Object> overlay(Map<String, Object> me, Map<String, Object> him) {
    for (String key : him.keySet()) {
      if (him.get(key) instanceof Map && me.get(key) instanceof Map) {
        Map myChild = (Map) me.get(key);
        Map hisChild = (Map) him.get(key);
        me.put(key, overlay(myChild, hisChild));
      }
      else if (him.get(key) instanceof String && me.get(key) instanceof Map) {
        continue; // skip, this is usually versions : { "x.x.x" : "latest" } on incomplete documents
      }
      else {
        me.put(key, him.get(key));
      }
    }
    return me;
  }

  // ==

  @Override
  protected void validate(final Map<String, Object> raw) {
    checkNotNull(raw);
    checkArgument(raw.containsKey("name"), "No mapping for 'name'");
    checkArgument(raw.get("name") instanceof String, "Mapping for 'name' is not a string");
    if (raw.containsKey("versions")) { // unpublished pkg roots have no this node
      checkArgument(raw.containsKey("versions"), "No mapping for 'versions'");
      checkArgument(raw.get("versions") instanceof Map, "'versions' is not an object hash");
    }
  }

  /**
   * Builds a map mapping versionString to {@link PackageVersion}. Always returns new instance of a map.
   */
  private Map<String, PackageVersion> wrapVersions(final Map<String, Object> raw) {
    final Map<String, PackageVersion> wrappedVersions = Maps.newHashMap();
    final Map<String, Object> versions = (Map<String, Object>) raw.get("versions");
    if (versions != null) {
      for (Entry<String, Object> versionsEntry : versions.entrySet()) {
        if (versionsEntry.getValue() instanceof Map) {
          wrappedVersions.put(versionsEntry.getKey(),
              new PackageVersion(this, getRepositoryId(), (Map<String, Object>) versionsEntry.getValue()));
        }
        else if (versionsEntry.getValue() instanceof String) {
          // create an "incomplete" document
          // we do not care about value that is either a tag or version, since key is always a version
          final Map<String, Object> latestVersion = Maps.newHashMap();
          latestVersion.put("name", raw.get("name"));
          if (raw.containsKey("description")) {
            latestVersion.put("description", raw.get("description"));
          }
          latestVersion.put("version", versionsEntry.getKey());
          final Map<String, String> dist = Maps.newHashMap();
          dist.put("tarball", "unknown");
          latestVersion.put("dist", dist);
          wrappedVersions.put(versionsEntry.getKey(), new PackageVersion(this, getRepositoryId(), latestVersion));
        }
      }
    }
    return wrappedVersions;
  }

  /**
   * Resolves a version to "tag" using "dist-tags" entry in package root document. If no tag found or dist-tags
   * section is completely missing, passed in version is returned as-is.
   */
  private String resolveVersionToTag(final String version) {
    if (getRaw().containsKey("dist-tags")) {
      final Map<String, String> distTags = (Map<String, String>) getRaw().get("dist-tags");
      for (Entry<String, String> distTag : distTags.entrySet()) {
        if (version.equals(distTag.getValue())) {
          return distTag.getKey();
        }
      }
    }
    return version;
  }
}
