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
package org.sonatype.nexus.repository.search.sql.store;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.search.sql.SearchAssetRecord;
import org.sonatype.nexus.repository.search.sql.SearchRecord;
import org.sonatype.nexus.repository.search.sql.index.SearchTokenizer;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableCollection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonatype.nexus.repository.search.sql.index.SearchTokenizer.tsEscape;

public class SearchRecordData
    implements SearchRecord
{
  private static final Logger log = LoggerFactory.getLogger(SearchRecordData.class);

  /**
   * PostgreSQL tsvector has a maximum word size limit of 2046 bytes.
   * Any word exceeding this limit will cause an error when inserted into the database.
   */
  private static final int MAX_TSVECTOR_WORD_BYTES = 2046;

  /**
   * Maximum total number of search parameters across all collections.
   * This limit prevents hitting PostgreSQL's PreparedStatement parameter limit (65,535).
   * Set to 60,000 to stay safely under the limit while allowing maximum data retention.
   *
   * With the to_tsvector(string_agg(...)) approach, we avoid PostgreSQL stack depth issues
   * that occurred with || concatenation. The VALUES clause provides parameters without
   * creating deeply nested expression trees, allowing us to use the full 60,000 param capacity.
   *
   * See NEXUS-50251 for details.
   */
  protected static final int MAX_TOTAL_SEARCH_PARAMS = 60000;

  /**
   * Maximum estimated byte size for each tsvector column (keywords, paths).
   * PostgreSQL enforces a hard 1MB (1,048,576 bytes) limit on tsvector columns.
   * We use 900,000 bytes (~86% of 1MB) as a safety margin to account for PostgreSQL's
   * internal tsvector overhead (position data, length prefixes) not tracked here.
   *
   * See NEXUS-52625 for details.
   */
  @VisibleForTesting
  static final int MAX_TSVECTOR_BYTES = 900_000;

  /**
   * Flag to track if we've already logged a warning about hitting the parameter limit.
   * Prevents log spam when the limit is exceeded.
   */
  private boolean paramLimitWarningLogged = false;

  /** Prevents duplicate log warnings when the keywords tsvector byte limit is reached. */
  private boolean keywordByteLimitWarningLogged = false;

  /** Prevents duplicate log warnings when the paths tsvector byte limit is reached. */
  private boolean pathByteLimitWarningLogged = false;

  /** Running estimate of bytes accumulated in the keywords tsvector column. */
  private int keywordsBytesEstimate = 0;

  /** Running estimate of bytes accumulated in the paths tsvector column. */
  private int pathsBytesEstimate = 0;

  /**
   * The repository ID from the repository record, it is part of the primary identifier of the record (PK).
   */
  private Integer repositoryId;

  /**
   * The component ID from the component record, it is part of the primary identifier of the record (PK).
   */
  private Integer componentId;

  /**
   * The component's format (e.g. raw, npm, maven2, etc.).
   */
  private String format;

  /**
   * The namespace of the component from the component record.
   */
  private String namespace;

  private Set<String> namespaceNames = new LinkedHashSet<>();

  /**
   * The component name from the component record.
   */
  private String componentName;

  private Set<String> aliasComponentNames = new LinkedHashSet<>();

  /**
   * The kind of the component from the component record (e.g. manifest, binary, etc.).
   */
  private String componentKind;

  /**
   * The version of the component from the component record.
   */
  private String version;

  private Set<String> versionNames = new LinkedHashSet<>();

  private String normalisedVersion;

  /**
   * The largest AssetBlob.created() from the component's assets.
   */
  private OffsetDateTime lastModified;

  /**
   * The repository name from the repository record.
   */
  private String repositoryName;

  /**
   * The uploaderBy property for asset blobs.
   */
  private final Set<String> uploaders = new HashSet<>();

  /**
   * The uploaderByIp property for asset blobs.
   */
  private final Set<String> uploaderIps = new HashSet<>();

  // asset paths
  private final Set<String> paths = new HashSet<>();

  private final Set<String> keywords = new LinkedHashSet<>();// Need to maintain order because of ranking

  private final Set<String> md5 = new HashSet<>();

  private final Set<String> sha1 = new HashSet<>();

  private final Set<String> sha256 = new HashSet<>();

  private final Set<String> sha512 = new HashSet<>();

  private final Set<String> formatFieldValues1 = new LinkedHashSet<>();

  private final Set<String> formatFieldValues2 = new LinkedHashSet<>();

  private final Set<String> formatFieldValues3 = new LinkedHashSet<>();

  private final Set<String> formatFieldValues4 = new LinkedHashSet<>();

  private final Set<String> formatFieldValues5 = new LinkedHashSet<>();

  private final Set<String> formatFieldValues6 = new LinkedHashSet<>();

  private final Set<String> formatFieldValues7 = new LinkedHashSet<>();

  private NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());

  private boolean prerelease;

  private Integer entityVersion;

  private final Set<String> tags = new HashSet<>();

  private final Set<SearchAssetRecord> searchAssetRecords = new LinkedHashSet<>();

  private final boolean usePostgreSQLFormat;

  public SearchRecordData() {
    this(true); // Default to PostgreSQL format for backward compatibility
  }

  public SearchRecordData(final boolean usePostgreSQLFormat) {
    this.usePostgreSQLFormat = usePostgreSQLFormat;
  }

  public SearchRecordData(final Integer repositoryId, final String format) {
    this(repositoryId, null, format, true);
  }

  public SearchRecordData(final Integer repositoryId, final Integer componentId, final String format) {
    this(repositoryId, componentId, format, true);
  }

  public SearchRecordData(
      final Integer repositoryId,
      final Integer componentId,
      final String format,
      final boolean usePostgreSQLFormat)
  {
    this.usePostgreSQLFormat = usePostgreSQLFormat;
    this.repositoryId = repositoryId;
    this.componentId = componentId;
    this.format = format;
  }

  public Integer getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final Integer repositoryId) {
    this.repositoryId = repositoryId;
  }

  @Override
  public Integer getComponentId() {
    return componentId;
  }

  public void setComponentId(final Integer componentId) {
    this.componentId = componentId;
  }

  public String getFormat() {
    return format;
  }

  @Override
  public void setFormat(final String format) {
    this.format = format;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  @Override
  public void addNamespaceNames(final String namespace) {
    if (isNotBlank(namespace)) {
      addTokens(namespace, namespaceNames, false);
    }
  }

  @Override
  public Collection<String> getNamespaceNames() {
    return unmodifiableCollection(namespaceNames);
  }

  @Override
  public String getComponentName() {
    return componentName;
  }

  @Override
  public void setComponentName(final String componentName) {
    this.componentName = componentName;
  }

  @Override
  public Collection<String> getAliasComponentNames() {
    return unmodifiableCollection(aliasComponentNames);
  }

  @Override
  public void addAliasComponentName(final String aliasComponentName) {
    if (isNotBlank(aliasComponentName)) {
      addTokens(aliasComponentName, aliasComponentNames, false);
    }
  }

  @Override
  public String getComponentKind() {
    return componentKind;
  }

  @Override
  public void setComponentKind(final String componentKind) {
    this.componentKind = componentKind;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public void setNormalisedVersion(final String normalisedVersion) {
    this.normalisedVersion = normalisedVersion;
  }

  @Override
  public String getNormalisedVersion() {
    return normalisedVersion;
  }

  @Override
  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public void addVersionNames(final String version) {
    if (isNotBlank(version)) {
      addTokens(version, versionNames, false);
    }
  }

  @Override
  public Collection<String> getVersionNames() {
    return unmodifiableCollection(versionNames);
  }

  @Override
  public SearchAssetRecord newAssetRecord() {
    final SearchAssetData searchAssetData = new SearchAssetData();
    searchAssetData.setRepositoryId(repositoryId);
    searchAssetData.setComponentId(componentId);
    searchAssetData.setFormat(format);
    addSearchAssetRecord(searchAssetData);
    return searchAssetData;
  }

  @Override
  public OffsetDateTime getLastModified() {
    return lastModified;
  }

  @Override
  public void setLastModified(final OffsetDateTime lastModified) {
    this.lastModified = lastModified;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public void addKeywords(final List<String> values) {
    if (values != null) {
      for (String value : values) {
        addKeyword(value);
      }
    }
  }

  @Override
  public void addKeyword(final String value) {
    if (isNotBlank(value) && !this.keywords.contains(value)) {
      addTokens(value, this.keywords, false);
    }
  }

  @Override
  public Collection<String> getKeywords() {
    return unmodifiableCollection(keywords);
  }

  @Override
  public void addMd5(final String md5) {
    if (isNotBlank(md5) && canAddSearchParam()) {
      this.md5.add(md5);
    }
  }

  @Override
  public void addSha1(final String sha1) {
    if (isNotBlank(sha1) && canAddSearchParam()) {
      this.sha1.add(sha1);
    }
  }

  @Override
  public void addSha256(final String sha256) {
    if (isNotBlank(sha256) && canAddSearchParam()) {
      this.sha256.add(sha256);
    }
  }

  @Override
  public void addSha512(final String sha512) {
    if (isNotBlank(sha512) && canAddSearchParam()) {
      this.sha512.add(sha512);
    }
  }

  @Override
  public Collection<String> getMd5() {
    return unmodifiableCollection(md5);
  }

  @Override
  public Collection<String> getSha1() {
    return unmodifiableCollection(sha1);
  }

  @Override
  public Collection<String> getSha256() {
    return unmodifiableCollection(sha256);
  }

  @Override
  public Collection<String> getSha512() {
    return unmodifiableCollection(sha512);
  }

  @Override
  public void addFormatFieldValue1(final String value, final boolean preventTokenization) {
    addTokens(value, formatFieldValues1, preventTokenization);
  }

  @Override
  public Collection<String> getFormatFieldValues1() {
    return unmodifiableCollection(formatFieldValues1);
  }

  @Override
  public void addFormatFieldValue2(final String value) {
    addTokens(value, formatFieldValues2, false);
  }

  @Override
  public Collection<String> getFormatFieldValues2() {
    return unmodifiableCollection(formatFieldValues2);
  }

  @Override
  public void addFormatFieldValue3(final String value) {
    addTokens(value, formatFieldValues3, false);
  }

  @Override
  public Collection<String> getFormatFieldValues3() {
    return unmodifiableCollection(formatFieldValues3);
  }

  @Override
  public void addFormatFieldValue4(final String value, final boolean preventTokenization) {
    addTokens(value, formatFieldValues4, preventTokenization);
  }

  @Override
  public Collection<SearchAssetRecord> getSearchAssetRecords() {
    return Collections.unmodifiableCollection(searchAssetRecords);
  }

  @Override
  public void addSearchAssetRecord(final SearchAssetRecord searchAssetRecord) {
    if (searchAssetRecord != null) {
      this.searchAssetRecords.add(searchAssetRecord);
    }
  }

  @Override
  public Collection<String> getFormatFieldValues4() {
    return unmodifiableCollection(formatFieldValues4);
  }

  @Override
  public void addFormatFieldValue5(final String value) {
    addTokens(value, formatFieldValues5, false);
  }

  @Override
  public Collection<String> getFormatFieldValues5() {
    return unmodifiableCollection(formatFieldValues5);
  }

  @Override
  public void addFormatFieldValue6(final String value, final boolean preventTokenization) {
    addTokens(value, formatFieldValues6, preventTokenization);
  }

  @Override
  public Collection<String> getFormatFieldValues6() {
    return unmodifiableCollection(formatFieldValues6);
  }

  @Override
  public void addFormatFieldValue7(final String value) {
    addTokens(value, formatFieldValues7, false);
  }

  @Override
  public Collection<String> getFormatFieldValues7() {
    return unmodifiableCollection(formatFieldValues7);
  }

  @Override
  public NestedAttributesMap attributes() {
    return attributes;
  }

  @Override
  public void setAttributes(final NestedAttributesMap attributes) {
    this.attributes = checkNotNull(attributes);
  }

  @Override
  public void addUploader(final String uploader) {
    if (isNotBlank(uploader)) {
      addTokens(uploader, uploaders, true);
    }
  }

  @Override
  public Collection<String> getUploaders() {
    return unmodifiableCollection(uploaders);
  }

  @Override
  public void addUploaderIp(final String uploaderIp) {
    if (isNotBlank(uploaderIp)) {
      addTokens(uploaderIp, uploaderIps, true);
    }
  }

  @Override
  public Collection<String> getUploaderIps() {
    return unmodifiableCollection(uploaderIps);
  }

  @Override
  public void addPath(final String path) {
    if (isNotBlank(path) && canAddSearchParam()) {
      String pathValue = usePostgreSQLFormat ? tsEscape(path) : path.toLowerCase();
      int byteLen = getUtf8ByteLength(pathValue);
      if (!canAddPathBytes(byteLen)) {
        return;
      }
      this.paths.add(pathValue);
      pathsBytesEstimate += byteLen;
    }
  }

  @Override
  public Collection<String> getPaths() {
    return unmodifiableCollection(paths);
  }

  @Override
  public boolean isPrerelease() {
    return prerelease;
  }

  @Override
  public void setPrerelease(final boolean prerelease) {
    this.prerelease = prerelease;
  }

  @Override
  public void setEntityVersion(final Integer entityVersion) {
    this.entityVersion = entityVersion;
  }

  @Override
  public Integer getEntityVersion() {
    return entityVersion;
  }

  @Override
  public void setTags(final Collection<String> values) {
    if (!values.isEmpty()) {
      for (String tag : values) {
        if (canAddSearchParam()) {
          tags.add(tag);
        }
        else {
          break;
        }
      }
    }
  }

  @Override
  public Collection<String> getTags() {
    return unmodifiableCollection(tags);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchRecordData tableData = (SearchRecordData) o;
    return Objects.equals(repositoryId, tableData.repositoryId) &&
        Objects.equals(componentId, tableData.componentId) &&
        Objects.equals(format, tableData.format) &&
        Objects.equals(namespace, tableData.namespace) &&
        Objects.equals(namespaceNames, tableData.namespaceNames) &&
        Objects.equals(componentName, tableData.componentName) &&
        Objects.equals(aliasComponentNames, tableData.aliasComponentNames) &&
        Objects.equals(componentKind, tableData.componentKind) &&
        Objects.equals(version, tableData.version) &&
        Objects.equals(versionNames, tableData.versionNames) &&
        Objects.equals(normalisedVersion, tableData.normalisedVersion) &&
        Objects.equals(lastModified, tableData.lastModified) &&
        Objects.equals(repositoryName, tableData.repositoryName) &&
        Objects.equals(prerelease, tableData.prerelease) &&
        Objects.equals(uploaders, tableData.uploaders) &&
        Objects.equals(uploaderIps, tableData.uploaderIps) &&
        Objects.equals(paths, tableData.paths) &&
        Objects.equals(keywords, tableData.keywords) &&
        Objects.equals(md5, tableData.md5) &&
        Objects.equals(sha1, tableData.sha1) &&
        Objects.equals(sha256, tableData.sha256) &&
        Objects.equals(sha512, tableData.sha512) &&
        Objects.equals(entityVersion, tableData.entityVersion) &&
        Objects.equals(formatFieldValues1, tableData.formatFieldValues1) &&
        Objects.equals(formatFieldValues2, tableData.formatFieldValues2) &&
        Objects.equals(formatFieldValues3, tableData.formatFieldValues3) &&
        Objects.equals(formatFieldValues4, tableData.formatFieldValues4) &&
        Objects.equals(formatFieldValues5, tableData.formatFieldValues5) &&
        Objects.equals(formatFieldValues6, tableData.formatFieldValues6) &&
        Objects.equals(formatFieldValues7, tableData.formatFieldValues7) &&
        Objects.equals(attributes, tableData.attributes) &&
        Objects.equals(tags, tableData.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryId, componentId, format, namespace, namespaceNames, componentName,
        aliasComponentNames, componentKind,
        version, versionNames, normalisedVersion, lastModified, repositoryName,
        prerelease, uploaders, uploaderIps, paths, keywords, md5, sha1, sha256, sha512, entityVersion,
        formatFieldValues1, formatFieldValues2, formatFieldValues3, formatFieldValues4, formatFieldValues5,
        formatFieldValues6, formatFieldValues7, attributes, tags);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SearchRecordData.class.getSimpleName() + "[", "]")
        .add("repositoryId=" + repositoryId)
        .add("componentId=" + componentId)
        .add("format='" + format + "'")
        .add("namespace='" + namespace + "'")
        .add("namespaceNames='" + namespaceNames + "'")
        .add("componentName='" + componentName + "'")
        .add("aliasComponentNames='" + aliasComponentNames + "'")
        .add("componentKind='" + componentKind + "'")
        .add("version='" + version + "'")
        .add("versionNames='" + versionNames + "'")
        .add("normalisedVersion='" + normalisedVersion + "'")
        .add("lastModified=" + lastModified)
        .add("repositoryName='" + repositoryName + "'")
        .add("prerelease=" + prerelease)
        .add("uploader='" + uploaders + "'")
        .add("uploaderIp='" + uploaderIps + "'")
        .add("paths='" + paths + "'")
        .add("keywords='" + keywords + "'")
        .add("md5='" + md5 + "'")
        .add("sha1='" + sha1 + "'")
        .add("sha256='" + sha256 + "'")
        .add("sha512='" + sha512 + "'")
        .add("entityVersion='" + entityVersion + "'")
        .add("formatFieldValues1='" + formatFieldValues1 + "'")
        .add("formatFieldValues2='" + formatFieldValues2 + "'")
        .add("formatFieldValues3='" + formatFieldValues3 + "'")
        .add("formatFieldValues4='" + formatFieldValues4 + "'")
        .add("formatFieldValues5='" + formatFieldValues5 + "'")
        .add("formatFieldValues6='" + formatFieldValues6 + "'")
        .add("formatFieldValues7='" + formatFieldValues7 + "'")
        .add("attributes='" + attributes + "'")
        .add("tags='" + tags + "'")
        .toString();
  }

  @VisibleForTesting
  void addTokens(
      final String phrase,
      final Collection<String> collection,
      final boolean preventTokenization)
  {
    if (Strings2.isBlank(phrase)) {
      return;
    }

    // Byte-size guard applies to the keywords collection here. The paths collection
    // is guarded separately in addPath(). Other collections (namespace, version,
    // uploaders, formatFields) are small and bounded, so no guard is needed.
    boolean isKeywords = (collection == this.keywords);

    // For H2, store plain values without PostgreSQL's tsEscape formatting
    if (usePostgreSQLFormat) {
      String escapedPhrase = tsEscape(phrase);
      int phraseBytes = getUtf8ByteLength(escapedPhrase);
      if (phraseBytes <= MAX_TSVECTOR_WORD_BYTES && canAddSearchParam() &&
          (!isKeywords || canAddKeywordBytes(phraseBytes))) {
        collection.add(escapedPhrase);
        if (isKeywords) {
          keywordsBytesEstimate += phraseBytes;
        }
      }
      else if (log.isDebugEnabled()) {
        log.debug("Phrase too long. preventTokenization={} phrase={}", preventTokenization, phrase);
      }

      if (!preventTokenization) {
        int i = 0;
        int accumulatedBytes = 0;
        StringBuilder sb = new StringBuilder();
        for (String token : SearchTokenizer.TOKENIZER.split(phrase)) {
          if (Strings2.notBlank(token)) {
            String escapedToken = tsEscape(token);
            String nextEntry = escapedToken + ":" + (i + 1) + " ";

            int nextBytes = getUtf8ByteLength(nextEntry);

            if (accumulatedBytes + nextBytes > MAX_TSVECTOR_WORD_BYTES) {
              break;
            }

            sb.append(escapedToken).append(':').append(++i).append(' ');
            accumulatedBytes += nextBytes;
          }
        }

        // Only add if there is more than one token and we haven't hit the limit
        if (i > 1 && canAddSearchParam() && (!isKeywords || canAddKeywordBytes(accumulatedBytes))) {
          sb.deleteCharAt(sb.length() - 1);
          collection.add(sb.toString());
          if (isKeywords) {
            keywordsBytesEstimate += accumulatedBytes;
          }
        }
      }
    }
    else {
      // H2 format - store plain value only, no tokenization
      if (canAddSearchParam()) {
        collection.add(phrase.toLowerCase());
      }
    }
  }

  /**
   * Checks if we can add another search parameter without exceeding the limit.
   * Logs a warning once when the limit is reached.
   *
   * Note: This calculates the actual SQL parameter count, not just the Java collection size.
   * MyBatis uses some collections multiple times in the SQL query (e.g., paths is used twice),
   * so we must account for that multiplier effect to stay under PostgreSQL's 65,535 limit.
   *
   * @return true if we can add another parameter, false otherwise
   */
  private boolean canAddSearchParam() {
    // Calculate actual SQL parameter count based on MyBatis XML mapping.
    // The SQL uses INSERT...ON CONFLICT...DO UPDATE, so PostgreSQL prepares parameters
    // for BOTH the INSERT and UPDATE clauses, doubling the parameter count.
    //
    // Per-clause usage (then multiply by 2 for INSERT + UPDATE):
    // - paths: 2× per clause (join + toQuotedTsVector) = 4× total
    // - All other collections: 1× per clause (toTsVector) = 2× total
    int insertUpdateParamCount = (paths.size() * 2) + // join + toQuotedTsVector
        uploaders.size() +
        uploaderIps.size() +
        keywords.size() +
        md5.size() +
        sha1.size() +
        sha256.size() +
        sha512.size() +
        namespaceNames.size() +
        versionNames.size() +
        aliasComponentNames.size() +
        formatFieldValues1.size() +
        formatFieldValues2.size() +
        formatFieldValues3.size() +
        formatFieldValues4.size() +
        formatFieldValues5.size() +
        formatFieldValues6.size() +
        formatFieldValues7.size() +
        tags.size();

    // Double for INSERT + UPDATE clauses
    int actualSqlParamCount = insertUpdateParamCount * 2;

    if (actualSqlParamCount < MAX_TOTAL_SEARCH_PARAMS) {
      return true;
    }
    if (!paramLimitWarningLogged) {
      log.warn(
          "Component {} has reached maximum search parameter limit of {} (actual SQL params: {}), additional items will not be indexed",
          componentId, MAX_TOTAL_SEARCH_PARAMS, actualSqlParamCount);
      paramLimitWarningLogged = true;
    }
    return false;
  }

  /**
   * Checks if adding bytes to the keywords tsvector would exceed the byte limit.
   */
  private boolean canAddKeywordBytes(final int additionalBytes) {
    if (keywordsBytesEstimate + additionalBytes < MAX_TSVECTOR_BYTES) {
      return true;
    }
    if (!keywordByteLimitWarningLogged) {
      log.warn(
          "Component {} has reached keywords tsvector byte size limit of {} bytes " +
              "(current: {} bytes), additional data will not be searchable by keyword",
          componentId, MAX_TSVECTOR_BYTES, keywordsBytesEstimate);
      keywordByteLimitWarningLogged = true;
    }
    return false;
  }

  /**
   * Checks if adding bytes to the paths tsvector would exceed the byte limit.
   */
  private boolean canAddPathBytes(final int additionalBytes) {
    if (pathsBytesEstimate + additionalBytes < MAX_TSVECTOR_BYTES) {
      return true;
    }
    if (!pathByteLimitWarningLogged) {
      log.warn(
          "Component {} has reached paths tsvector byte size limit of {} bytes " +
              "(current: {} bytes), additional paths will not be indexed",
          componentId, MAX_TSVECTOR_BYTES, pathsBytesEstimate);
      pathByteLimitWarningLogged = true;
    }
    return false;
  }

  private static int getUtf8ByteLength(final String text) {
    return text.getBytes(StandardCharsets.UTF_8).length;
  }
}
