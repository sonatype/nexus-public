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
package org.sonatype.nexus.plugins.p2.repository.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.P2ContentClass;
import org.sonatype.nexus.plugins.p2.repository.P2ProxyRepository;
import org.sonatype.nexus.plugins.p2.repository.mappings.ArtifactMapping;
import org.sonatype.nexus.plugins.p2.repository.mappings.ArtifactPath;
import org.sonatype.nexus.plugins.p2.repository.metadata.P2MetadataSource;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteAccessDeniedException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteAuthenticationNeededException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Mirror;
import org.sonatype.nexus.proxy.repository.MutableProxyRepositoryKind;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.Description;

import static org.sonatype.nexus.util.DigesterUtils.getSha1Digest;

@Named(P2ProxyRepositoryImpl.ROLE_HINT)
@Description("Eclipse P2 Proxy Repository")
public class P2ProxyRepositoryImpl
    extends AbstractProxyRepository
    implements P2ProxyRepository, Repository
{
  public static final String ROLE_HINT = "p2";

  private static final String PRIVATE_MIRRORS_PATH = P2Constants.PRIVATE_ROOT + "/mirrors.xml";

  private final ContentClass contentClass;

  private final P2MetadataSource<P2ProxyRepository> metadataSource;

  private final P2ProxyRepositoryConfigurator p2ProxyRepositoryConfigurator;

  private volatile boolean mirrorsConfigured;

  private MutableProxyRepositoryKind repositoryKind;

  private final Map<String, Mirror> mirrorMap = new LinkedHashMap<>();

  @Inject
  public P2ProxyRepositoryImpl(final @Named(P2ContentClass.ID) ContentClass contentClass,
                               final @Named("proxy") P2MetadataSource<P2ProxyRepository> metadataSource,
                               final P2ProxyRepositoryConfigurator p2ProxyRepositoryConfigurator)
  {
    this.contentClass = contentClass;
    this.metadataSource = metadataSource;
    this.p2ProxyRepositoryConfigurator = p2ProxyRepositoryConfigurator;

    initArtifactMappingsAndMirrors();
  }

  @Override
  public P2MetadataSource<P2ProxyRepository> getMetadataSource() {
    return metadataSource;
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  /**
   * Override the "default" kind with Maven specifics.
   */
  @Override
  public RepositoryKind getRepositoryKind() {
    if (repositoryKind == null) {
      // is this class able to be hosted at all?
      repositoryKind =
          new MutableProxyRepositoryKind(this, null, new DefaultRepositoryKind(HostedRepository.class, null),
              new DefaultRepositoryKind(ProxyRepository.class,
                  Arrays.asList(new Class<?>[]{P2ProxyRepository.class})));
    }

    return repositoryKind;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<P2ProxyRepositoryConfiguration>()
    {
      @Override
      public P2ProxyRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new P2ProxyRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  protected Configurator getConfigurator() {
    return p2ProxyRepositoryConfigurator;
  }

  @Override
  protected P2ProxyRepositoryConfiguration getExternalConfiguration(final boolean forModification) {
    return (P2ProxyRepositoryConfiguration) super.getExternalConfiguration(forModification);
  }

  protected void configureMirrors(final ResourceStoreRequest incomingRequest) {
    log.debug("Repository " + getId() + ": configureMirrors: mirrorsConfigured=" + mirrorsConfigured);
    AbstractStorageItem mirrorsItem = null;

    // Try to get the mirrors from local storage
    try {
      final ResourceStoreRequest request = new ResourceStoreRequest(PRIVATE_MIRRORS_PATH);
      mirrorsItem = getLocalStorage().retrieveItem(this, request);
    }
    catch (final LocalStorageException e) {
      // fall through
    }
    catch (final ItemNotFoundException e) {
      // fall through
    }

    if (mirrorsConfigured && (mirrorsItem == null || !isOld(mirrorsItem))) {
      return;
    }

    try {
      // Try to get the mirrors from remote
      if (mirrorsItem == null || isOld(mirrorsItem)) {
        mirrorsURLsByRepositoryURL = null;

        log.debug("Repository " + getId() + ": configureMirrors: getting mirrors from remote");
        final ResourceStoreRequest request = new ResourceStoreRequest(P2Constants.ARTIFACTS_XML);
        request.getRequestContext().setParentContext(incomingRequest.getRequestContext());

        final StorageItem artifacts = retrieveItem(request);

        // The P2ProxyMetadataSource.ATTR_MIRRORS_URL attribute of the artifacts StorageItem
        // is set in the P2ProxyMetadataSource.doRetrieveArtifactsDom().
        // The attribute is set only if the remote repository is a SimpleArtifactRepository (i.e. it is not set
        // for CompositeArtifactRepositories)
        final String mirrorsURL =
            artifacts.getRepositoryItemAttributes().get(P2ProxyMetadataSource.ATTR_MIRRORS_URL);
        if (mirrorsURL != null) {
          // The remote repository is a SimpleArtifactRepository with mirrors configured
          log.debug(
              "Repository " + getId() + ": configureMirrors: found single mirrors URL=" + mirrorsURL);
          final StorageFileItem remoteMirrorsItem = getMirrorsItemRemote(mirrorsURL);
          final ContentLocator content =
              new PreparedContentLocator(((StorageFileItem) remoteMirrorsItem).getInputStream(),
                  "text/xml", remoteMirrorsItem.getLength());
          mirrorsItem =
              new DefaultStorageFileItem(this, new ResourceStoreRequest(PRIVATE_MIRRORS_PATH),
                  true /* isReadable */, false /* isWritable */, content);
          mirrorsItem = doCacheItem(mirrorsItem);
        }
        else {
          mirrorsItem = getMirrorsItemRemote();
          if (mirrorsItem == null) {
            mirrorsConfigured = true;
            return;
          }
        }
      }

      final Xpp3Dom mirrorsDom = getMirrorsDom((StorageFileItem) mirrorsItem);
      final Xpp3Dom[] repositoryDoms = mirrorsDom.getChildren("repository");
      if (repositoryDoms != null && repositoryDoms.length > 0) {
        for (final Xpp3Dom repositoryDom : repositoryDoms) {
          final String repositoryUrl = repositoryDom.getAttribute("uri");
          final Xpp3Dom[] mirrorsDoms = repositoryDom.getChildren("mirror");
          addMirrors(repositoryUrl, mirrorsDoms);
        }
      }
      else {
        log.debug("Repository " + getId() + ": configureMirrors: found flat list of mirrors");
        // There are no "repository" elements, so we only have a flat list of mirrors

        mirrorMap.clear();

        for (final Xpp3Dom mirrorDOM : mirrorsDom.getChildren("mirror")) {
          final String mirrorUrl = mirrorDOM.getAttribute("url");
          log.debug("Repository " + getId() + ": configureMirrors: found mirror URL=" + mirrorUrl);
          if (mirrorUrl != null) {
            // TODO: validate that this is valid way to generate id
            // or if should be pulled from xml
            final Mirror mirror = new Mirror(
                getSha1Digest(mirrorUrl), mirrorUrl, getRemoteUrl()
            );
            mirrorMap.put(mirror.getId(), mirror);
          }
        }
      }

      mirrorsConfigured = true;
    }
    catch (final Exception e) {
      if (log.isDebugEnabled()) {
        log.warn(
            "Could not retrieve list of repository mirrors. "
                + "All downloads will come from repository canonical URL.", e);
      }
      else {
        String message = e.getMessage();
        if (e.getCause() != null) {
          if (!message.contains(e.getCause().getMessage())) {
            message += " Reason: " + e.getCause().getMessage();
          }
        }
        log.warn(
            "Could not retrieve list of repository mirrors (" + message
                + "). All downloads will come from repository canonical URL.");
      }
    }
  }

  private void addMirrors(final String remoteRepoUrl, final Xpp3Dom[] mirrorsDoms) {
    if (mirrorsDoms != null) {
      for (final Xpp3Dom mirrorDOM : mirrorsDoms) {
        final String mirrorUrl = mirrorDOM.getAttribute("url");
        if (mirrorUrl != null) {
          // TODO: validate that this is valid way to generate id
          // or if should be pulled from xml
          final Mirror mirror = new Mirror(getSha1Digest(mirrorUrl), mirrorUrl, remoteRepoUrl);
          mirrorMap.put(mirror.getId(), mirror);
        }
      }
    }
    final Mirror mirror = new Mirror(getSha1Digest(remoteRepoUrl), remoteRepoUrl, remoteRepoUrl);
    mirrorMap.put(mirror.getId(), mirror);
  }

  private Xpp3Dom getMirrorsDom(final StorageFileItem mirrorsItem)
      throws IOException, XmlPullParserException
  {
    try (InputStream is = mirrorsItem.getInputStream()) {
      return Xpp3DomBuilder.build(new XmlStreamReader(is));
    }
  }

  private AbstractStorageItem getMirrorsItemRemote()
      throws IllegalOperationException, ItemNotFoundException, IOException, XmlPullParserException
  {
    final Map<String, String> mirrorsURLsMap = getMirrorsURLsByRepositoryURL();
    if (mirrorsURLsMap == null) {
      log.debug("getMirrorsItemRemote: mirrorsURLsMap is null");
      return null;
    }

    final Xpp3Dom mirrorsByRepositoryDom = new Xpp3Dom("mirrors");
    for (final String repositoryURL : mirrorsURLsMap.keySet()) {
      log.debug("getMirrorsItemRemote: repositoryURL=" + repositoryURL);
      final Xpp3Dom repositoryDom = new Xpp3Dom("repository");
      repositoryDom.setAttribute("uri", repositoryURL);
      mirrorsByRepositoryDom.addChild(repositoryDom);

      final String mirrorsURL = mirrorsURLsMap.get(repositoryURL);
      if (mirrorsURL == null) {
        continue;
      }

      final StorageFileItem mirrorsItem = getMirrorsItemRemote(mirrorsURL);
      final Xpp3Dom mirrorsDom = getMirrorsDom((StorageFileItem) mirrorsItem);
      for (final Xpp3Dom mirrorDOM : mirrorsDom.getChildren("mirror")) {
        log.debug("getMirrorsItemRemote: mirrorURL=" + mirrorDOM.getAttribute("url"));
        repositoryDom.addChild(mirrorDOM);
      }
    }

    final FileContentLocator fileContentLocator = new FileContentLocator("text/xml");
    try {
      try (OutputStream buffer = fileContentLocator.getOutputStream();) {
        final MXSerializer mx = new MXSerializer();
        mx.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  ");
        mx.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n");
        final String encoding = "UTF-8";
        mx.setOutput(buffer, encoding);
        mx.startDocument(encoding, null);
        mirrorsByRepositoryDom.writeToSerializer(null, mx);
        mx.flush();
      }

      final DefaultStorageFileItem result =
          new DefaultStorageFileItem(this, new ResourceStoreRequest(PRIVATE_MIRRORS_PATH),
              true /* isReadable */, false /* isWritable */, fileContentLocator);
      return doCacheItem(result);
    }
    finally {
      fileContentLocator.delete();
    }
  }

  private StorageFileItem getMirrorsItemRemote(final String mirrorsURL)
      throws MalformedURLException, RemoteAccessException, RemoteStorageException, ItemNotFoundException
  {
    final URL url = new URL(mirrorsURL);
    final ResourceStoreRequest request = new ResourceStoreRequest(url.getFile());
    final String baseUrl = getBaseMirrorsURL(url);
    final AbstractStorageItem mirrorsItem = getRemoteStorage().retrieveItem(this, request, baseUrl);
    if (mirrorsItem instanceof StorageFileItem) {
      return (StorageFileItem) mirrorsItem;
    }
    throw new ItemNotFoundException(
        ItemNotFoundException.reasonFor(request, this, "URL %s does not contain valid mirrors", mirrorsURL));
  }

  private String getBaseMirrorsURL(final URL mirrorsURL) {
    final StringBuilder baseUrl = new StringBuilder();
    baseUrl.append(mirrorsURL.getProtocol()).append("://");
    if (mirrorsURL.getUserInfo() != null) {
      baseUrl.append(mirrorsURL.getUserInfo()).append("@");
    }
    baseUrl.append(mirrorsURL.getHost());
    if (mirrorsURL.getPort() != -1) {
      baseUrl.append(":").append(mirrorsURL.getPort());
    }

    return baseUrl.toString();
  }

  @Override
  public StorageItem retrieveItem(final boolean fromTask, final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final RepositoryItemUid uid = createUid(P2Constants.METADATA_LOCK_PATH);
    final RepositoryItemUidLock lock = uid.getLock();

    try {
      // Lock the metadata do be sure that the artifacts are retrieved from consistent paths.
      lock.lock(Action.read);
      try {
        // NOTE - THIS CANNOT be a write action (create/delete/update) as that will force a write lock
        // thus serializing access to this p2 repo. Using a read action here, will block the file from
        // being deleted/updated while retrieving the remote item, and that is all we need.

        // NXCM-2499 temporarily we do put access serialization back here, to avoid all the deadlocks.
        lock.lock(Action.create);
        return super.retrieveItem(fromTask, request);
      }
      finally {
        lock.unlock();
      }
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  protected StorageItem doRetrieveItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final String requestPath = request.getRequestPath();
    log.debug("Repository " + getId() + ": doRetrieveItem:" + requestPath);

    if (P2Constants.ARTIFACT_MAPPINGS_XML.equals(requestPath)) {
      if (getLocalStorage() == null) {
        throw new ItemNotFoundException(request);
      }

      final StorageItem item = getLocalStorage().retrieveItem(this, request);
      return item;
    }

    final StorageItem item;
    try {
      item = metadataSource.doRetrieveItem(request, this);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
    if (item != null) {
      return item;
    }

    // note this method can potentially go retrieve new mirrors, but it is using locking, so no
    // need to worry about multiples getting in
    configureMirrors(request);
    return super.doRetrieveItem(request);
  }

  private volatile Map<String, String> mirrorsURLsByRepositoryURL;

  private Map<String, String> getMirrorsURLsByRepositoryURL()
      throws IllegalOperationException, StorageException
  {
    if (!hasArtifactMappings) {
      return null;
    }

    if (mirrorsURLsByRepositoryURL == null) {
      loadArtifactMappings();
    }
    return mirrorsURLsByRepositoryURL;
  }

  private volatile Map<String, ArtifactMapping> remoteArtifactMappings;

  private volatile boolean hasArtifactMappings;

  @Override
  public void initArtifactMappingsAndMirrors() {
    hasArtifactMappings = true;
    remoteArtifactMappings = null;
    mirrorsURLsByRepositoryURL = null;
    mirrorsConfigured = false;
  }

  @Override
  public Map<String, ArtifactMapping> getArtifactMappings()
      throws IllegalOperationException, StorageException
  {
    // cstamas: this method is called from other paths, not like getMirrorsURLsByRepositoryURL() above (called from
    // configureMirrors()), so the safest is to protect it with similar locking stuff even if we do suffer in
    // performance, but we avoiding potential deadlocks (this method was synchronized).
    final RepositoryItemUid uid = createUid(P2Constants.METADATA_LOCK_PATH);
    final RepositoryItemUidLock lock = uid.getLock();

    try {
      lock.lock(Action.create);
      if (!hasArtifactMappings) {
        return null;
      }

      if (remoteArtifactMappings == null) {
        loadArtifactMappings();
      }
      return remoteArtifactMappings;
    }
    finally {
      lock.unlock();
    }
  }

  private void loadArtifactMappings()
      throws StorageException, IllegalOperationException
  {
    StorageFileItem artifactMappingsItem;
    final ResourceStoreRequest req = new ResourceStoreRequest(P2Constants.ARTIFACT_MAPPINGS_XML);
    req.setRequestLocalOnly(true);
    try {
      artifactMappingsItem = (StorageFileItem) retrieveItem(true, req);
    }
    catch (final ItemNotFoundException e) {
      hasArtifactMappings = false;
      return;
    }

    final Map<String, ArtifactMapping> tempRemoteArtifactMappings = new LinkedHashMap<String, ArtifactMapping>();
    final Map<String, String> tempMirrorsURLsByRepositoryURL = new LinkedHashMap<String, String>();
    Xpp3Dom dom;
    try {
      dom = Xpp3DomBuilder.build(new XmlStreamReader(artifactMappingsItem.getInputStream()));
    }
    catch (final IOException e) {
      throw new LocalStorageException("Could not load artifact mappings", e);
    }
    catch (final XmlPullParserException e) {
      throw new LocalStorageException("Could not load artifact mappings", e);
    }
    final Xpp3Dom[] artifactRepositories = dom.getChildren("repository");
    for (final Xpp3Dom artifactRepositoryDom : artifactRepositories) {
      final String repositoryUri = artifactRepositoryDom.getAttribute("uri");

      final Map<String, ArtifactPath> artifactPaths = new LinkedHashMap<String, ArtifactPath>();
      final ArtifactMapping artifactMapping = new ArtifactMapping(repositoryUri, artifactPaths);
      for (final Xpp3Dom artifactDom : artifactRepositoryDom.getChildren("artifact")) {
        artifactPaths.put(artifactDom.getAttribute("remotePath"),
            new ArtifactPath(artifactDom.getAttribute("remotePath"), artifactDom.getAttribute("md5")));
      }
      tempRemoteArtifactMappings.put(repositoryUri, artifactMapping);

      final String mirrorsURL = artifactRepositoryDom.getAttribute(P2Constants.PROP_MIRRORS_URL);
      tempMirrorsURLsByRepositoryURL.put(repositoryUri, mirrorsURL);
    }

    remoteArtifactMappings = tempRemoteArtifactMappings;
    mirrorsURLsByRepositoryURL = tempMirrorsURLsByRepositoryURL;
  }

  @Override
  protected List<String> getRemoteUrls(final ResourceStoreRequest request) {
    final List<String> remoteUrls = Lists.newArrayList();

    String remoteUrl = getRemoteUrl();

    // lookup child from the map here then udpate the remote URL
    try {
      final Map<String, ArtifactMapping> artifactMappings = getArtifactMappings();
      if (artifactMappings != null) {
        for (final String remoteRepositoryURI : artifactMappings.keySet()) {
          if (artifactMappings.get(remoteRepositoryURI).getArtifactsPath().containsKey(
              request.getRequestPath())) {
            remoteUrl = remoteRepositoryURI;
            break;
          }
        }
      }
    }
    catch (final StorageException | IllegalOperationException e) {
      log.warn("Could not find artifact-mapping.", e);
    }

    for (final Mirror mirror : mirrorMap.values()) {
      if (StringUtils.equals(remoteUrl, mirror.getMirrorOfUrl())) {
        remoteUrls.add(mirror.getUrl());
      }
    }

    remoteUrls.add(getRemoteUrl());

    return remoteUrls;
  }

  @Override
  protected boolean isRemoteStorageReachable(final ResourceStoreRequest request)
      throws StorageException, RemoteAuthenticationNeededException, RemoteAccessDeniedException
  {
    // For p2 repositories, the root URL may not be reachable,
    // so we test if we can reach one of the "standard" p2 repository metadata files.
    for (final String metadataFilePath : P2Constants.METADATA_FILE_PATHS) {
      log.debug(
          "isRemoteStorageReachable: RepositoryId=" + getId() + ": Trying to access " + metadataFilePath);
      request.setRequestPath(metadataFilePath);
      try {
        // We cannot use getRemoteStorage().isReachable() here because that forces the request path to be "/"
        if (getRemoteStorage().containsItem(this, request)) {
          log.debug(
              "isRemoteStorageReachable: RepositoryId=" + getId() + ": Successfully accessed "
                  + metadataFilePath);
          return true;
        }
      }
      catch (final RemoteStorageException e) {
        log.debug(
            "isRemoteStorageReachable: RepositoryId=" + getId() + ": Caught exception while trying to access "
                + metadataFilePath, e);

        // rethrow and core will say _why_ it autoblocked too
        // everything else than RemoteStorageException will bubble up too
        throw e;
      }
    }

    return false;
  }

  @Override
  public boolean isMetadataOld(final StorageItem metadataItem) {
    return isOld(metadataItem);
  }

  @Override
  protected boolean isOld(final StorageItem item) {
    if (P2ProxyMetadataSource.isP2MetadataItem(item.getPath())) {
      return super.isOld(getMetadataMaxAge(), item);
    }
    else {
      return super.isOld(getArtifactMaxAge(), item);
    }
  }

  public ChecksumPolicy getChecksumPolicy() {
    return getExternalConfiguration(false).getChecksumPolicy();
  }

  public void setChecksumPolicy(final ChecksumPolicy checksumPolicy) {
    getExternalConfiguration(true).setChecksumPolicy(checksumPolicy);
  }

}
