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
package org.sonatype.nexus.proxy.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.util.StringUtils;

/**
 * The mapping.
 *
 * @author cstamas
 */
public class RepositoryPathMapping
{
  public enum MappingType
  {
    BLOCKING, INCLUSION, EXCLUSION;
  }

  ;

  private String id;

  private MappingType mappingType;

  private String groupId;

  private List<Pattern> patterns;

  private List<String> mappedRepositories;

  public RepositoryPathMapping(String id, MappingType mappingType, String groupId, List<String> regexps,
                               List<String> mappedRepositories)
      throws PatternSyntaxException
  {
    this.id = id;

    this.mappingType = mappingType;

    if (StringUtils.isBlank(groupId) || "*".equals(groupId)) {
      this.groupId = "*";
    }
    else {
      this.groupId = groupId;
    }

    patterns = new ArrayList<Pattern>(regexps.size());

    for (String regexp : regexps) {
      if (StringUtils.isNotEmpty(regexp)) {
        patterns.add(Pattern.compile(regexp));
      }
    }

    this.mappedRepositories = mappedRepositories;
  }

  public boolean isAllGroups() {
    return "*".equals(getGroupId());
  }

  public boolean matches(Repository repository, ResourceStoreRequest request) {
    if (isAllGroups()
        || (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class) && groupId.equals(repository
        .getId()))) {
      for (Pattern pattern : patterns) {
        if (pattern.matcher(request.getRequestPath()).matches()) {
          return true;
        }
      }

      return false;
    }
    else {
      return false;
    }
  }

  public String getId() {
    return id;
  }

  public MappingType getMappingType() {
    return mappingType;
  }

  public String getGroupId() {
    return groupId;
  }

  public List<Pattern> getPatterns() {
    return patterns;
  }

  public List<String> getMappedRepositories() {
    return mappedRepositories;
  }

  // ==

  public String toString() {
    StringBuilder sb = new StringBuilder(getId());
    sb.append("=[");
    sb.append("type=");
    sb.append(getMappingType().toString());
    sb.append(", groupId=");
    sb.append(getGroupId());
    sb.append(", patterns=");
    sb.append(getPatterns().toString());
    sb.append(", mappedRepositories=");
    sb.append(getMappedRepositories());
    sb.append("]");
    return sb.toString();
  }
}
