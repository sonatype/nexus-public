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
package org.sonatype.nexus.plugins.p2.repository.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.metadata.Artifacts.Artifact;
import org.sonatype.nexus.plugins.p2.repository.metadata.Content.Unit;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactsMerge
{
  private final Logger logger = LoggerFactory.getLogger(ArtifactsMerge.class);

  protected Logger getLogger() {
    return logger;
  }

  /**
   * Merges artifacts from the other repository. Current implementation requires both repositories to have identical
   * mapping and properties.
   */
  public Artifacts mergeArtifactsMetadata(final String name, final List<StorageFileItem> items)
      throws P2MetadataMergeException
  {
    final Artifacts result = new Artifacts(name);

    if (items == null || items.size() <= 0) {
      return result; // nothing to merge
    }

    if (items.size() == 1) {
      // TODO do we need/want to handle this specially?
    }

    final List<Artifacts.Artifact> mergedArtifacts = new ArrayList<Artifacts.Artifact>();

    final LinkedHashMap<String, String> mergedProperties = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> mergedMappingsMap = new LinkedHashMap<String, String>();
    final Set<String> keys = new HashSet<String>();
    for (StorageFileItem fileItem : items) {
      try {
        final Artifacts repo = new Artifacts(MetadataUtils.getMetadataXpp3Dom(fileItem));
        // mergedProperties = mergeProperties( mergedProperties, repo );
        mergeMappings(mergedMappingsMap, repo);
        for (final Artifacts.Artifact artifact : repo.getArtifacts()) {
          if (keys.add(getArtifactKey(artifact))) {
            mergedArtifacts.add(new Artifacts.Artifact(artifact));
          }
          // first repo wins
        }
      }
      catch (IOException e) {
        getLogger().warn(
            "Could not retrieve {} from {} due to {}. Skipping it from aggregation into {}",
            new Object[]{
                P2Constants.ARTIFACTS_XML,
                RepositoryStringUtils.getHumanizedNameString(fileItem.getRepositoryItemUid().getRepository()),
                e.getMessage(), name
            });

      }
      catch (XmlPullParserException e) {
        getLogger().warn(
            "Could not retrieve {} from {} due to {}. Skipping it from aggregation into {}",
            new Object[]{
                P2Constants.ARTIFACTS_XML,
                RepositoryStringUtils.getHumanizedNameString(fileItem.getRepositoryItemUid().getRepository()),
                e.getMessage(), name
            });
      }
    }

    // handle rule ordering (this potentially creates new map instance, but only if needed)
    mergedMappingsMap = orderMappings(mergedMappingsMap);

    final Xpp3Dom mergedMappings = createMappingsDom(mergedMappingsMap);

    mergedProperties.put(P2Constants.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
    mergedProperties.put("publishPackFilesAsSiblings", "true");

    final boolean compressed = P2Constants.ARTIFACTS_PATH.equals(P2Constants.ARTIFACTS_JAR);
    mergedProperties.put(P2Constants.PROP_COMPRESSED, Boolean.toString(compressed));

    result.setArtifacts(mergedArtifacts);
    result.setProperties(mergedProperties);
    setMappings(result.getDom(), mergedMappings);

    return result;
  }

  private Xpp3Dom createMappingsDom(final Map<String, String> mappingsMap) {
    final Xpp3Dom mappingsDom = new Xpp3Dom("mappings");
    mappingsDom.setAttribute("size", Integer.toString(mappingsMap.size()));

    for (final String filter : mappingsMap.keySet()) {
      final Xpp3Dom ruleDom = new Xpp3Dom("rule");
      ruleDom.setAttribute("filter", filter);
      ruleDom.setAttribute("output", mappingsMap.get(filter));
      mappingsDom.addChild(ruleDom);
    }

    return mappingsDom;
  }

  private LinkedHashMap<String, String> orderMappings(final LinkedHashMap<String, String> mergedMappingsMap)
      throws P2MetadataMergeException
  {
    // detect the presence of format=packed rules having filter attributes as:
    // "(classifier=osgi.bundle) (format=packed)"
    // "(classifier=osgi.bundle)"
    // Note: this is not limited to bundles, features and who knows what else can be packed too
    // IF present, reshuffle the map, otherwise just return the passed in mergedMappingsMap instance

    final String packedFilter = "(format=packed)";
    boolean hasPackedRule = false;

    for (final String filter : mergedMappingsMap.keySet()) {
      if (filter.contains(packedFilter)) {
        hasPackedRule = true;
        break;
      }
    }

    if (!hasPackedRule) {
      return mergedMappingsMap;
    }

    final LinkedHashMap<String, String> ordered = new LinkedHashMap<String, String>(mergedMappingsMap.size());

    for (final Map.Entry<String, String> entry : mergedMappingsMap.entrySet()) {
      // add all with "(format=packed)" first (bundles, features, etc)
      if (entry.getKey().contains(packedFilter)) {
        ordered.put(entry.getKey(), entry.getValue());
      }
    }
    for (final Map.Entry<String, String> entry : mergedMappingsMap.entrySet()) {
      // add all the rest, without "(format=packed)" after
      if (!entry.getKey().contains(packedFilter)) {
        // check is non-packed rule already present
        ordered.put(entry.getKey(), entry.getValue());
      }
    }

    return ordered;
  }

  private void mergeMappings(final LinkedHashMap<String, String> mergedMappingsMap, final Artifacts repo)
      throws P2MetadataMergeException
  {
    final Xpp3Dom repoMappingsDom = repo.getDom().getChild("mappings");
    if (repoMappingsDom == null) {
      // Nothing to merge
      return;
    }

    final Xpp3Dom[] repoMappingRules = repoMappingsDom.getChildren("rule");
    for (final Xpp3Dom repoMappingRule : repoMappingRules) {
      final String filter = repoMappingRule.getAttribute("filter");
      final String output = repoMappingRule.getAttribute("output");

      if (mergedMappingsMap.containsKey(filter)) {
        // Known rule
        if (!output.equals(mergedMappingsMap.get(filter))) {
          throw new P2MetadataMergeException("Incompatible artifact repository mapping rules: filter="
              + filter + ", output1=" + output + ", output2=" + mergedMappingsMap.get(filter));
        }
      }
      else {
        // New rule
        mergedMappingsMap.put(filter, output);
      }
    }
  }

  private void setMappings(final Xpp3Dom dom, final Xpp3Dom mergedMappings) {
    AbstractMetadata.removeChild(dom, "mappings");
    dom.addChild(mergedMappings);
  }

  private String getArtifactKey(final Artifact artifact) {
    final String format = artifact.getFormat();

    if (format != null && format.trim().length() > 0) {
      return artifact.getClassifier() + ":" + artifact.getId() + ":" + artifact.getVersion() + ":" + format;
    }
    else {
      return artifact.getClassifier() + ":" + artifact.getId() + ":" + artifact.getVersion();
    }
  }

  public Content mergeContentMetadata(final String name, final List<StorageFileItem> items)
      throws P2MetadataMergeException
  {
    final Content result = new Content(name);

    if (items == null || items.size() <= 0) {
      return result; // nothing to merge
    }

    if (items.size() == 1) {
      // TODO do we need/want to handle this specially?
    }

    final List<Content.Unit> mergedUnits = new ArrayList<Content.Unit>();

    final LinkedHashMap<String, String> mergedProperties = new LinkedHashMap<String, String>();

    final Set<String> keys = new HashSet<String>();
    for (final StorageFileItem fileItem : items) {
      try {
        final Content repo = new Content(MetadataUtils.getMetadataXpp3Dom(fileItem));
        // mergedProperties = mergeProperties( mergedProperties, repo );
        for (final Content.Unit unit : repo.getUnits()) {
          if (keys.add(getUnitKey(unit))) {
            mergedUnits.add(new Content.Unit(unit));
          }
          // first repo wins
        }
      }
      catch (IOException e) {
        getLogger().warn(
            "Could not retrieve {} from {} due to {}. Skipping it from aggregation into {}",
            new Object[]{
                P2Constants.CONTENT_XML,
                RepositoryStringUtils.getHumanizedNameString(fileItem.getRepositoryItemUid().getRepository()),
                e.getMessage(), name
            });

      }
      catch (XmlPullParserException e) {
        getLogger().warn(
            "Could not retrieve {} from {} due to {}. Skipping it from aggregation into {}",
            new Object[]{
                P2Constants.CONTENT_XML,
                RepositoryStringUtils.getHumanizedNameString(fileItem.getRepositoryItemUid().getRepository()),
                e.getMessage(), name
            });
      }
    }

    mergedProperties.put(P2Constants.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));

    final boolean compressed = P2Constants.ARTIFACTS_PATH.equals(P2Constants.ARTIFACTS_JAR);
    mergedProperties.put(P2Constants.PROP_COMPRESSED, Boolean.toString(compressed));

    result.setUnits(mergedUnits);
    result.setProperties(mergedProperties);

    return result;
  }

  private String getUnitKey(final Unit unit) {
    return unit.getId() + ":" + unit.getVersion();
  }

}
