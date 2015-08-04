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
package org.sonatype.plugin.nexus.testenvironment;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.sonatype.plugin.nexus.testenvironment.filter.TestScopeFilter;
import org.sonatype.plugins.portallocator.Port;
import org.sonatype.plugins.portallocator.PortAllocatorMojo;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.MapConstraints;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

public class AbstractEnvironmentMojo
    extends AbstractMojo
    implements Contextualizable
{

  public final static List<String> DEFAULT_PORT_NAMES = Collections.unmodifiableList(Arrays.asList(new String[]{
      "proxy-repo-port", "proxy-repo-control-port", "nexus-application-port", "nexus-proxy-port",
      "nexus-control-port", "email-server-port", "webproxy-server-port", "jira-server-port"
  }));

  /**
   * Max times to try and allocate unique port values
   */
  public static final int MAX_PORT_ALLOCATION_RETRY = 3;

  protected static final String PROP_NEXUS_BASE_DIR = "nexus-base-dir";

  @Component
  protected org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

  @Component
  private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

  @Parameter(defaultValue = "${localRepository}")
  protected org.apache.maven.artifact.repository.ArtifactRepository localRepository;

  @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
  protected java.util.List<ArtifactRepository> remoteRepositories;

  @Component
  private MavenFileFilter mavenFileFilter;

  @Component
  protected MavenProjectBuilder mavenProjectBuilder;

  @Component
  private ArtifactMetadataSource artifactMetadataSource;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  protected MavenSession session;

  private PlexusContainer plexus;

  /**
   * The maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  /**
   * Where nexus instance should be extracted
   */
  @Parameter(defaultValue = "${project.build.directory}/nexus", required = true)
  private File destination;

  /**
   * Artifact file containing nexus bundle
   */
  @Parameter
  protected MavenArtifact nexusBundleArtifact;

  /**
   * Name of teh directory created out of nexus artifact bundle. Default is
   * ${nexusBundleArtifactId}-${nexusBundleArtifactVersion}.
   */
  @Parameter
  protected String nexusBundleName;

  /**
   * Nexus plugin artifacts to be installed into the Nexus instance under test.
   */
  @Parameter
  private MavenArtifact[] nexusPluginsArtifacts;

  /**
   * Resources to be unpacked and then contents copied into Nexus default-configs
   */
  @Parameter
  private MavenArtifact[] extraResourcesArtifacts;

  /**
   * When true setup a maven instance
   */
  @Parameter(defaultValue = "true")
  private boolean setupMaven;

  /**
   * Maven used on ITs
   *
   * @see EnvironmentMojo#setupMaven
   */
  @Parameter
  private MavenArtifact mavenArtifact;

  /**
   * Where Maven instance should be created
   *
   * @see EnvironmentMojo#setupMaven
   */
  @Parameter(defaultValue = "${project.build.directory}/maven")
  private File mavenLocation;

  /**
   * Resources in the test project can be added beneath this directory so that
   */
  @Parameter(defaultValue = "${basedir}/src/test/it-resources")
  protected File resourcesSourceLocation;

  /**
   * This directory is where the default-configs included inside the this plugin will be extracted to BEFORE they are
   * copied into the nexus work dir. A project property 'test-resources-folder' contains the absolute path of this
   * directory.
   */
  @Parameter(defaultValue = "${project.build.directory}/resources")
  private File resourcesDestinationLocation;

  @Parameter(property = "maven.test.skip")
  protected boolean testSkip;

  @Parameter(property = "test-env.skip")
  protected boolean pluginSkip;

  @Parameter(defaultValue = "false")
  private boolean extractNexusPluginsJavascript;

  @Parameter(defaultValue = "${project.build.testOutputDirectory}")
  protected File testOutputDirectory;

  @Parameter(defaultValue = "${basedir}/src/test/resources")
  protected File testResourcesDirectory;

  @Parameter(defaultValue = "false")
  private boolean promoteOptionalPlugin;

  @Parameter
  private String mavenBaseDir;

  /**
   * Known ports can be manually set as part of the configuration
   */
  @Parameter
  @SuppressWarnings("rawtypes")
  private Map staticPorts;

  /**
   * If true plugin won't include nexus-plugin dependencies with scope test
   */
  @Parameter
  private boolean excludeTestDependencies;

  /**
   * Comma separated list of patterns to delete from an unpacked Nexus bundle. Patterns are deleted before any
   * optional plugins or other external resources are installed
   */
  @Parameter
  private String nexusBundleExcludes;

  /**
   * If there is one or more *-webapp.zip files in runtime/apps/nexus/plugin-repository, then unpack that zip to
   * nexus
   * base dir and delete the original file
   */
  @Parameter
  private boolean unpackPluginWebapps;

  /**
   * Prevents a given artifact to be copy into nexus plugin repository
   */
  @Parameter
  private String[] exclusions;

  @Parameter(property = "useBundlePluginsIfPresent")
  protected boolean useBundlePluginsIfPresent;

  public void execute()
      throws MojoExecutionException, MojoFailureException
  {
    if (testSkip || pluginSkip) {
      return;
    }

    init();

    validateStaticPorts();
    allocatePorts();

    project.getProperties().put("jetty-application-host", "0.0.0.0");
    project.getProperties().put("nexus-base-url",
        "http://localhost:" + project.getProperties().getProperty("nexus-application-port") + "/nexus/");
    project.getProperties().put("proxy-repo-base-url",
        "http://localhost:" + project.getProperties().getProperty("proxy-repo-port") + "/remote/");
    project.getProperties().put("proxy-repo-base-dir", getPath(new File(destination, "proxy-repo")));
    project.getProperties().put("proxy-repo-target-dir", getPath(new File(destination, "proxy-repo")));

    Artifact bundle = getNexusBundle();
    // extract the artifact specified in the plugin config
    if (!this.markerExist("bundle")) {
      unpack(bundle.getFile(), destination, bundle.getType());
      this.createMarkerFile("bundle");
    }

    // since specifying excludes/includes is not implemented for all archive types (tar.gz for example)
    // remove files and dirs based on specified pattern after all of the files were unpacked, rather than during the
    // unpack
    // the use case for this is that a plugin we are installing later may include a plugin that is already included
    // with bundle
    if (nexusBundleExcludes != null) {
      deleteFromDirectory(destination, nexusBundleExcludes);
    }

    File nexusBaseDir = new File(destination, bundle.getArtifactId() + "-" + bundle.getBaseVersion());
    if (nexusBundleName != null) {
      nexusBaseDir = new File(destination, nexusBundleName);
    }
    getLog().info("Nexus bundle directory: " + nexusBaseDir);

    File nexusWorkDir = new File(destination, "sonatype-work");
    getLog().info("Nexus work directory: " + nexusWorkDir);

    project.getProperties().put(PROP_NEXUS_BASE_DIR, getPath(nexusBaseDir));
    project.getProperties().put("nexus-work-dir", getPath(nexusWorkDir));

    // conf dir
    project.getProperties().put("application-conf", getPath(new File(destination, "sonatype-work/conf")));

    // final File plexusProps = new File( nexusBaseDir, "conf/plexus.properties" );
    final File plexusProps = new File(nexusBaseDir, "conf/nexus.properties");
    copyUrl("/default-config/nexus.properties", plexusProps);

    File extraPlexusProps = new File(testResourcesDirectory, "plexus.properties");
    if (extraPlexusProps.exists()) {
      merge(plexusProps, extraPlexusProps, "properties");
    }
    project.getProperties().put("nexus-plexus-config-file", getPath(new File(nexusBaseDir, "conf/plexus.xml")));

    File libFolder = new File(nexusBaseDir, "nexus/WEB-INF/lib");

    // if any plugin artifacts were specified, install them into runtime
    File pluginFolder = new File(nexusBaseDir, "nexus/WEB-INF/plugin-repository");
    project.getProperties().put("plugin-repository", getPath(pluginFolder));

    Collection<MavenArtifact> npas = getNexusPluginsArtifacts();
    if (npas != null) {
      setupPlugins(npas, libFolder, pluginFolder);
    }

    // promote any plugins included in the bundle to the runtime install
    if (promoteOptionalPlugin) {
      File optionalPluginFolder = new File(nexusBaseDir, "nexus/WEB-INF//optional-plugins");
      try {
        if (optionalPluginFolder.exists()) {
          FileUtils.copyDirectoryStructure(optionalPluginFolder, pluginFolder);
          FileUtils.deleteDirectory(optionalPluginFolder);
        }
      }
      catch (IOException e) {
        throw new MojoExecutionException("Failed to promote optinal plugins", e);
      }
    }

    if (unpackPluginWebapps) {
      try {
        // now if we have *-webapp.zip in pluginfolder, unpack that to sonatype-work dir and delete zip file
        @SuppressWarnings("unchecked")
        List<File> webapps = FileUtils.getFiles(pluginFolder, "*-webapp.zip", null);
        for (File webapp : webapps) {
          unpack(webapp, new File((String) project.getProperties().get(PROP_NEXUS_BASE_DIR)), "zip");
          webapp.delete();
        }
      }
      catch (IOException e) {
        throw new MojoExecutionException("Failed to unpack webapp plugins:", e);
      }

    }

    // setup Maven if requested for this test
    if (setupMaven) {
      mavenLocation.mkdirs();
      String mavenVersion = setupMaven().getBaseVersion();
      project.getProperties().put("maven-version", mavenVersion);
      if (this.mavenBaseDir == null) {
        this.mavenBaseDir = "apache-maven-" + mavenVersion;
      }
      project.getProperties().put("maven-basedir", getPath(new File(mavenLocation, mavenBaseDir)));

      File fakeRepoDest = new File(mavenLocation, "fake-repo");
      project.getProperties().put("maven-repository", getPath(fakeRepoDest));
    }

    if (!resourcesDestinationLocation.isDirectory()) {
      resourcesDestinationLocation.mkdirs();
    }
    project.getProperties().put("test-resources-folder", getPath(resourcesDestinationLocation));
    // ./resources dir at root of project by default, suitable for tests I guess?
    if (resourcesSourceLocation.isDirectory()) {
      project.getProperties().put("test-resources-source-folder", getPath(resourcesSourceLocation));
    }

    // start default configs
    File defaultConfig = new File(resourcesDestinationLocation, "default-configs");
    project.getProperties().put("default-configs", getPath(defaultConfig));

    copyUrl("/default-config/nexus.xml", new File(defaultConfig, "nexus.xml"));
    copyUrl("/default-config/security.xml", new File(defaultConfig, "security.xml"));
    copyUrl("/default-config/security-configuration.xml", new File(defaultConfig, "security-configuration.xml"));
    copyUrl("/default-config/settings.xml", new File(defaultConfig, "settings.xml"));
    copyUrl("/default-config/logback-nexus.xml", new File(defaultConfig, "logback-nexus.xml"));

    File sourceDefaultConfig = new File(resourcesSourceLocation, "default-config");
    if (sourceDefaultConfig.isDirectory()) {
      copyAndInterpolate(sourceDefaultConfig, defaultConfig);
    }
    // end default configs

    // start baseTest.properties
    File baseTestProperties = new File(testOutputDirectory, "baseTest.properties");
    copyUrl("/default-config/baseTest.properties", baseTestProperties);

    File testSuiteProperties = new File(resourcesSourceLocation, "baseTest.properties");
    if (testSuiteProperties.isFile()) {
      merge(baseTestProperties, testSuiteProperties, "properties");
    }

    addProjectProperties(baseTestProperties);
    // end baseTest.properties

    File logbackConfig = new File(testOutputDirectory, "logback.xml");
    if (!logbackConfig.exists()) {
      copyUrl("/test-config/logback.xml", logbackConfig);
    }

    copyExtraResources();

    File destinationComponents = new File(testOutputDirectory, "META-INF/plexus/components.xml");
    copyUrl("/default-config/components.xml", destinationComponents);

    File componentsXml = new File(testResourcesDirectory, "components.xml");
    if (componentsXml.exists()) {
      copyAndInterpolate(componentsXml.getParentFile(), destinationComponents.getParentFile());
    }

    if (extractNexusPluginsJavascript) {
      extractPluginJs();
    }
  }

  /**
   * Delete file patterns from a base directory
   */
  @SuppressWarnings("unchecked")
  protected void deleteFromDirectory(final File baseDirectory, final String patternsToDelete)
      throws MojoExecutionException
  {
    getLog().info("Deleting from: " + baseDirectory + "; pattern: " + patternsToDelete);
    try {
      final List<String> filesToDelete =
          FileUtils.getFileAndDirectoryNames(baseDirectory, patternsToDelete, null, true, true, true, true);
      for (String fileToDelete : filesToDelete) {
        FileUtils.forceDelete(fileToDelete);
      }
    }
    catch (IOException e1) {
      throw new MojoExecutionException(e1.getMessage(), e1);
    }
  }

  protected Collection<MavenArtifact> getNexusPluginsArtifacts()
      throws MojoExecutionException
  {
    if (this.nexusPluginsArtifacts == null) {
      return Collections.emptySet();
    }

    return Arrays.asList(this.nexusPluginsArtifacts);
  }

  protected Artifact getNexusBundle()
      throws MojoExecutionException, MojoFailureException
  {
    return getMavenArtifact(nexusBundleArtifact);
  }

  private void extractPluginJs()
      throws MojoExecutionException
  {
    Collection<Artifact> plugins = getNexusPlugins();

    File outputDir = new File(project.getProperties().getProperty("nexus.webapp"), "static");
    outputDir.mkdirs();

    for (Artifact plugin : plugins) {
      ZipFile file;
      try {
        file = new ZipFile(plugin.getFile());
      }
      catch (IOException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }

      getLog().debug("Processing " + plugin);

      Enumeration<? extends ZipEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();

        String name = entry.getName();
        if (!(name.startsWith("static/js/") && name.endsWith(".js"))) {
          continue;
        }

        File outFile = new File(outputDir, name.substring(10));
        getLog().debug("Extracting " + name + " to " + outFile);

        try (InputStream in = file.getInputStream(entry);
             FileOutputStream out = new FileOutputStream(outFile)) {
          IOUtils.copy(in, out);
        }
        catch (IOException e) {
          throw new MojoExecutionException(e.getMessage(), e);
        }
      }
    }
  }

  private void addProjectProperties(File baseTestProperties)
      throws MojoExecutionException
  {
    try {
      Properties original = new Properties();

      try (InputStream input = new FileInputStream(baseTestProperties)) {
        original.load(input);
      }

      original.putAll(this.project.getProperties());
      original.putAll(this.session.getExecutionProperties());

      try (OutputStream output = new FileOutputStream(baseTestProperties)) {
        original.store(output, "Updated by EnvironmentMojo");
      }
    }
    catch (Exception e) {
      throw new MojoExecutionException(
          "Error adding properties '" + baseTestProperties.getAbsolutePath() + "'.", e);
    }
  }

  // todo add this as a constraint
  static final Pattern PORT_PATTERN =
      Pattern.compile("^(6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)$");

  @SuppressWarnings("unchecked")
  private void validateStaticPorts()
      throws MojoExecutionException, MojoFailureException
  {
    if (this.staticPorts != null) {
      try {
        @SuppressWarnings("rawtypes")
        BiMap staticPortMap = HashBiMap.create(this.staticPorts.size());
        staticPortMap = MapConstraints.constrainedBiMap(staticPortMap, MapConstraints.notNull());
        staticPortMap.putAll(this.staticPorts);
        this.staticPorts = staticPortMap;
      }
      catch (NullPointerException npe) {
        throw new MojoExecutionException("Port names and values must not be null.", npe);
      }
      catch (IllegalArgumentException iae) {
        throw new MojoExecutionException("Port names and values must not be duplicated.", iae);
      }
    }
  }

  /**
   * Call this to allocate the port values and store as project properties so that they can be filtered into the
   * Nexus
   * config files
   */
  private void allocatePorts()
      throws MojoExecutionException, MojoFailureException
  {
    allocatePorts(0);
  }

  /**
   * @param entryNum a value less than {@link #MAX_PORT_ALLOCATION_RETRY}
   * @throws MojoFailureException if methodEntryCount exceeds {@link #MAX_PORT_ALLOCATION_RETRY}
   */
  private void allocatePorts(int methodEntryCount)
      throws MojoExecutionException, MojoFailureException
  {
    if (methodEntryCount >= MAX_PORT_ALLOCATION_RETRY) {
      throw new MojoFailureException("Exceeded the maximum number of port allocation retries ("
          + MAX_PORT_ALLOCATION_RETRY + ")");
    }
    methodEntryCount++;

    // calc dynamic and static ports
    List<Port> portsList = new ArrayList<Port>();
    for (String portName : DEFAULT_PORT_NAMES) {
      Port port = new Port(portName);
      if (this.staticPorts != null && this.staticPorts.containsKey(portName)) {
        // this prevents assigning random port and instead
        // tests the static port for availability
        String portNum = String.valueOf(this.staticPorts.get(portName));
        getLog().debug("Statically defining port '" + portName + "' with value '" + portNum + "'.");
        port.setPortNumber(Integer.valueOf(portNum));
        port.setFailIfOccupied(true);
      }
      portsList.add(port);
    }

    // allocate dynamic ports and verify requested ports
    PortAllocatorMojo portAllocator = new PortAllocatorMojo();
    portAllocator.setProject(project);
    portAllocator.setLog(getLog());
    portAllocator.setPorts(portsList.toArray(new Port[0]));
    portAllocator.execute();

    // detect port collisions from dynamic port assignment
    List<String> portNums = new ArrayList<String>();
    for (String portName : DEFAULT_PORT_NAMES) {
      String portNum = String.valueOf(project.getProperties().get(portName));
      assert !"null".equals(portNum);
      if (portNums.contains(portNum)) {
        // duplicate ports generated by port allocator, try again
        getLog().debug(
            "Duplicate port value of " + portNum
                + " is defined. Trying to re-allocate non-duplicate port values.");
        allocatePorts(methodEntryCount);
      }
      portNums.add(portNum);
    }

  }

  private void copyExtraResources()
      throws MojoExecutionException, MojoFailureException
  {
    for (MavenArtifact extraResource : getExtraResourcesArtifacts()) {
      Artifact artifact = getMavenArtifact(extraResource);

      File dest;
      if (extraResource.getOutputDirectory() != null) {
        dest = extraResource.getOutputDirectory();
      }
      else if (extraResource.getOutputProperty() != null) {
        dest = new File(project.getProperties().getProperty(extraResource.getOutputProperty()));
      }
      else {
        dest = resourcesDestinationLocation;
      }
      unpack(artifact.getFile(), dest, artifact.getType());
    }
  }

  protected Collection<MavenArtifact> getExtraResourcesArtifacts() {
    if (extraResourcesArtifacts == null) {
      return Collections.emptySet();
    }
    return Arrays.asList(extraResourcesArtifacts);
  }

  private void merge(File originalFile, File extraContentFile, String type)
      throws MojoFailureException, MojoExecutionException
  {
    try {
      String name = FileUtils.removeExtension(extraContentFile.getName());
      String extension = FileUtils.getExtension(extraContentFile.getName());

      if ("properties".equals(type)) {
        File tempFile = File.createTempFile(name, extension);
        mavenFileFilter.copyFile(extraContentFile, tempFile, true, project, null, true, "UTF-8", session);

        Properties original = new Properties();
        try (InputStream originalReader = new FileInputStream(originalFile)) {
          original.load(originalReader);
        }

        Properties extra = new Properties();
        try (InputStream extraContentReader = new FileInputStream(tempFile)) {
          extra.load(extraContentReader);
        }

        for (Object key : extra.keySet()) {
          original.put(key, extra.get(key));
        }

        try (OutputStream originalWriter = new FileOutputStream(originalFile)) {
          original.store(originalWriter, "Updated by EnvironmentMojo");
        }
      }
      else {
        throw new MojoFailureException("Invalid file type: " + type);
      }
    }
    catch (Exception e) {
      throw new MojoExecutionException("Error merging files: Original '" + originalFile.getAbsolutePath()
          + "', extraContent '" + extraContentFile.getAbsolutePath() + "'.", e);
    }
  }

  private void copyUrl(String sourceUrl, File destinationFile)
      throws MojoExecutionException
  {
    getLog().debug("Copying url '" + sourceUrl + "'");

    String name = FileUtils.removeExtension(FileUtils.removePath(sourceUrl, '/'));
    String extension = FileUtils.getExtension(sourceUrl);

    try {
      destinationFile.getParentFile().mkdirs();
      destinationFile.createNewFile();

      File tempFile = File.createTempFile(name, extension);
      FileUtils.copyStreamToFile(new RawInputStreamFacade(getClass().getResourceAsStream(sourceUrl)),
          tempFile);
      mavenFileFilter.copyFile(tempFile, destinationFile, true, project, null, true, "UTF-8", session);
      tempFile.delete();
    }
    catch (Exception e) {
      throw new MojoExecutionException("Unable to copy resouce " + sourceUrl + " to " + name + "." + extension,
          e);
    }
  }

  private void copyAndInterpolate(File sourceDir, File destinationDir)
      throws MojoExecutionException
  {
    destinationDir.mkdirs();

    getLog().debug("Copying and interpolating dir '" + sourceDir + "'");
    try {
      DirectoryScanner scanner = new DirectoryScanner();

      scanner.setBasedir(sourceDir);
      scanner.addDefaultExcludes();
      scanner.scan();

      String[] files = scanner.getIncludedFiles();
      for (String file : files) {
        String extension = FileUtils.getExtension(file);

        File source = new File(sourceDir, file);

        File dest = new File(destinationDir, file);
        dest.getParentFile().mkdirs();

        if (Arrays.asList("zip", "jar", "tar.gz").contains(extension)) {
          // just copy know binaries
          FileUtils.copyFile(source, dest);
        }
        else {
          mavenFileFilter.copyFile(source, dest, true, project, null, false, "UTF-8", session);
        }
      }
    }
    catch (MavenFilteringException e) {
      throw new MojoExecutionException("Failed to copy : " + sourceDir, e);
    }
    catch (IOException e) {
      throw new MojoExecutionException("Failed to copy : " + sourceDir, e);
    }
  }

  private Artifact setupMaven()
      throws MojoExecutionException, MojoFailureException
  {
    if (mavenArtifact == null) {
      mavenArtifact = new MavenArtifact("org.apache.maven", "apache-maven", "bin", "tar.gz");
    }
    Artifact artifact = getMavenArtifact(mavenArtifact);

    if (!this.markerExist("maven")) {
      unpack(artifact.getFile(), mavenLocation, artifact.getType());
      this.createMarkerFile("maven");
    }

    return artifact;
  }

  private void init() {
    destination.mkdirs();
    resourcesDestinationLocation.mkdirs();

    if (nexusBundleArtifact == null) {
      throw new RuntimeException("Missing 'nexusBundleArtifact' configuration");
    }
  }

  private void setupPlugins(Collection<MavenArtifact> nexusPluginsArtifacts, File libsFolder, File pluginsFolder)
      throws MojoFailureException, MojoExecutionException
  {

    Set<Artifact> plugins = new LinkedHashSet<Artifact>();
    for (MavenArtifact plugin : nexusPluginsArtifacts) {
      Artifact pluginArtifact = getMavenArtifact(plugin);
      plugins.add(pluginArtifact);
    }

    nexusPluginsArtifacts = new LinkedHashSet<MavenArtifact>(nexusPluginsArtifacts);
    Collection<Artifact> nonTransitivePlugins = getNonTransitivePlugins(plugins);
    for (Artifact artifact : nonTransitivePlugins) {
      final MavenArtifact ma =
          new MavenArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
              artifact.getType());
      ma.setVersion(artifact.getVersion());
      nexusPluginsArtifacts.add(ma);
    }

    nexusPluginsArtifacts = filterOutExcludedPlugins(nexusPluginsArtifacts);

    final Map<String, File> bundlePlugins = useBundlePluginsIfPresent ? listPlugins(pluginsFolder) : null;

    for (MavenArtifact plugin : nexusPluginsArtifacts) {
      getLog().info("Setting up plugin: " + plugin);

      Artifact pluginArtifact = getMavenArtifact(plugin);

      File dest;
      if (plugin.getOutputDirectory() != null) {
        dest = plugin.getOutputDirectory();
      }
      else if (plugin.getOutputProperty() != null) {
        dest = new File(project.getProperties().getProperty(plugin.getOutputProperty()));
      }
      else if ("bundle".equals(pluginArtifact.getClassifier()) && "zip".equals(pluginArtifact.getType())) {
        dest = pluginsFolder;
      }
      else {
        dest = libsFolder;
      }

      String type = pluginArtifact.getType();

      if ("jar".equals(type)) {
        // System.out.println( "copying jar: "+ pluginArtifact.getFile().getAbsolutePath() + " to: "+
        // dest.getAbsolutePath() );
        copy(pluginArtifact.getFile(), dest);
      }
      else if ("zip".equals(type) || "tar.gz".equals(type)) {
        File file = pluginArtifact.getFile();
        if (file == null || !file.isFile()) {
          throw new MojoFailureException("Could not properly resolve artifact " + pluginArtifact + ", got "
              + file);
        }
        final String pluginKey = pluginArtifact.getGroupId() + ":" + pluginArtifact.getArtifactId();
        if (!useBundlePluginsIfPresent || !bundlePlugins.containsKey(pluginKey)) {
          unpack(file, dest, type);
        }
      }
      else {
        throw new MojoFailureException("Invalid plugin type: " + type);
      }
    }
  }

  private void copy(File sourceFile, File destinationDir)
      throws MojoExecutionException
  {
    getLog().info("Copying " + sourceFile + " to: " + destinationDir);

    try {
      FileUtils.copyFileToDirectory(sourceFile, destinationDir);
    }
    catch (IOException e) {
      throw new MojoExecutionException("Failed to copy : " + sourceFile, e);
    }
  }

  private String getPath(File nexusBaseDir) {
    try {
      return nexusBaseDir.getCanonicalPath();
    }
    catch (IOException e) {
      return nexusBaseDir.getAbsolutePath();
    }
  }

  private void unpack(File sourceFile, File destDirectory, String type)
      throws MojoExecutionException
  {
    destDirectory.mkdirs();

    getLog().info("Unpacking (" + type + ") " + sourceFile + " to: " + destDirectory);
    UnArchiver unarchiver;
    try {
      unarchiver = (UnArchiver) plexus.lookup(UnArchiver.ROLE, type);

      unarchiver.setSourceFile(sourceFile);
      unarchiver.setDestDirectory(destDirectory);
      try {
        unarchiver.extract();
      }
      catch (Exception e) {
        throw new MojoExecutionException("Unable to unpack " + sourceFile, e);
      }
    }
    catch (ComponentLookupException ce) {
      getLog().warn("Invalid packaging type " + type);

      try {
        FileUtils.copyFileToDirectory(sourceFile, destDirectory);
      }
      catch (IOException e) {
        throw new MojoExecutionException("Unable to copy " + sourceFile, e);
      }
    }

  }

  protected Artifact getMavenArtifact(MavenArtifact mavenArtifact)
      throws MojoExecutionException, MojoFailureException
  {
    Artifact artifact;
    if (mavenArtifact.getVersion() != null) {
      artifact =
          artifactFactory.createArtifactWithClassifier(mavenArtifact.getGroupId(),
              mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), mavenArtifact.getType(),
              mavenArtifact.getClassifier());
    }
    else {
      Set<Artifact> projectArtifacts =
          getFilteredArtifacts(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(),
              mavenArtifact.getType(), mavenArtifact.getClassifier());

      if (projectArtifacts.isEmpty()) {
        throw new MojoFailureException("Maven artifact: '" + mavenArtifact.toString()
            + "' not found on dependencies list");
      }
      else if (projectArtifacts.size() != 1) {
        throw new MojoFailureException("More then one artifact found on dependencies list: '"
            + mavenArtifact.toString() + "'");
      }

      artifact = projectArtifacts.iterator().next();
    }

    if ("nexus-plugin".equals(mavenArtifact.getType())) {
      artifact =
          artifactFactory.createArtifactWithClassifier(artifact.getGroupId(), artifact.getArtifactId(),
              artifact.getVersion(), "zip", "bundle");
    }

    return resolve(artifact);
  }

  @SuppressWarnings("unchecked")
  private Set<Artifact> getFilteredArtifacts(String groupId, String artifactId, String type, String classifier)
      throws MojoExecutionException
  {
    Set<Artifact> projectArtifacts = new LinkedHashSet<Artifact>();

    projectArtifacts.addAll(project.getAttachedArtifacts());
    projectArtifacts.addAll(project.getArtifacts());

    FilterArtifacts filter = getFilters(groupId, artifactId, type, classifier);

    return filtterArtifacts(projectArtifacts, filter);
  }

  @SuppressWarnings("unchecked")
  private Set<Artifact> filtterArtifacts(Set<Artifact> projectArtifacts, FilterArtifacts filter)
      throws MojoExecutionException
  {
    // perform filtering
    try {
      projectArtifacts = filter.filter(projectArtifacts);
    }
    catch (ArtifactFilterException e) {
      throw new MojoExecutionException("Error filtering artifacts", e);
    }

    return projectArtifacts;
  }

  private FilterArtifacts getFilters(String groupId, String artifactId, String type, String classifier) {
    FilterArtifacts filter = new FilterArtifacts();

    if (type != null) {
      filter.addFilter(new TypeFilter(type, null));
    }
    if (classifier != null) {
      filter.addFilter(new ClassifierFilter(classifier, null));
    }
    if (groupId != null) {
      filter.addFilter(new GroupIdFilter(groupId, null));
    }
    if (artifactId != null) {
      filter.addFilter(new ArtifactIdFilter(artifactId, null));
    }

    if (excludeTestDependencies) {
      filter.addFilter(new TestScopeFilter());
    }
    return filter;
  }

  protected Artifact resolve(Artifact artifact)
      throws MojoExecutionException
  {
    if (!artifact.isResolved()) {
      try {
        resolver.resolve(artifact, remoteRepositories, localRepository);
      }
      catch (AbstractArtifactResolutionException e) {
        throw new MojoExecutionException("Unable to resolve artifact: " + artifact, e);
      }
    }

    return artifact;
  }

  protected Collection<Artifact> getNexusPlugins()
      throws MojoExecutionException
  {

    Set<Artifact> projectArtifacts = new LinkedHashSet<Artifact>();
    projectArtifacts.addAll(getFilteredArtifacts(null, null, "zip", "bundle"));
    projectArtifacts.addAll(getFilteredArtifacts(null, null, "nexus-plugin", null));
    projectArtifacts.addAll(getNonTransitivePlugins(projectArtifacts));

    List<Artifact> resolvedArtifacts = new ArrayList<Artifact>();

    for (Artifact artifact : projectArtifacts) {
      Artifact ra =
          artifactFactory.createArtifactWithClassifier(artifact.getGroupId(), artifact.getArtifactId(),
              artifact.getVersion(), artifact.getType(), artifact.getClassifier());

      resolvedArtifacts.add(resolve(ra));
    }

    return resolvedArtifacts;
  }

  @SuppressWarnings("unchecked")
  private Collection<Artifact> getNonTransitivePlugins(Set<Artifact> projectArtifacts)
      throws MojoExecutionException
  {
    Collection<Artifact> deps = new LinkedHashSet<Artifact>();

    for (Artifact artifact : projectArtifacts) {
      Artifact pomArtifact =
          artifactFactory.createArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
              artifact.getClassifier(), "pom");
      Set<Artifact> result;
      try {
        MavenProject pomProject =
            mavenProjectBuilder.buildFromRepository(pomArtifact, remoteRepositories, localRepository);

        Set<Artifact> artifacts = pomProject.createArtifacts(artifactFactory, null, null);
        artifacts = filterOutSystemDependencies(artifacts);
        ArtifactResolutionResult arr =
            resolver.resolveTransitively(artifacts, pomArtifact, localRepository, remoteRepositories,
                artifactMetadataSource, null);
        result = arr.getArtifacts();
      }
      catch (Exception e) {
        throw new MojoExecutionException("Failed to resolve non-transitive deps " + e.getMessage(), e);
      }

      LinkedHashSet<Artifact> plugins = new LinkedHashSet<Artifact>();
      plugins.addAll(filtterArtifacts(result, getFilters(null, null, "nexus-plugin", null)));
      plugins.addAll(filtterArtifacts(result, getFilters(null, null, "zip", "bundle")));

      plugins.addAll(getNonTransitivePlugins(plugins));

      if (!plugins.isEmpty()) {
        getLog().debug(
            "Adding non-transitive dependencies for: " + artifact + " -\n"
                + plugins.toString().replace(',', '\n'));
      }

      deps.addAll(plugins);
    }

    return deps;
  }

  private Set<Artifact> filterOutSystemDependencies(Set<Artifact> artifacts) {
    return Sets.filter(artifacts, new Predicate<Artifact>()
    {
      @Override
      public boolean apply(Artifact a) {
        return !"system".equals(a.getScope());
      }
    });
  }

  private Set<MavenArtifact> filterOutExcludedPlugins(Collection<MavenArtifact> artifacts) {
    if (exclusions == null) {
      return Sets.newLinkedHashSet(artifacts);
    }

    return Sets.filter(Sets.newLinkedHashSet(artifacts), new Predicate<MavenArtifact>()
    {

      @Override
      public boolean apply(MavenArtifact a) {
        for (String exclusion : exclusions) {
          String[] pieces = exclusion.split(":");
          if (pieces.length != 2) {
            throw new IllegalArgumentException("Invalid exclusion " + exclusion);
          }
          String groupId = pieces[0];
          String artifactId = pieces[1];

          if ("*".equals(groupId)) {
            if (artifactId.equals(a.getArtifactId())) {
              return false;
            }
          }
          else {
            if (groupId.equals(a.getGroupId()) && artifactId.equals(a.getArtifactId())) {
              return false;
            }
          }
        }

        return true;
      }
    });
  }

  public void contextualize(Context context)
      throws ContextException
  {
    plexus = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
  }

  private boolean markerExist(String markerName) {
    File marker = new File(project.getBuild().getDirectory(), markerName + ".marker");
    return marker.exists();
  }

  private void createMarkerFile(String markerName) {
    File marker = new File(project.getBuild().getDirectory(), markerName + ".marker");
    try {
      if (!marker.createNewFile()) {
        this.getLog().warn(
            "Failed to create marker file: " + marker.getAbsolutePath()
                + " bundle will be extracted every time you run the build.");
      }
    }
    catch (IOException e) {
      this.getLog().warn(
          "Failed to create marker file: " + marker.getAbsolutePath()
              + " bundle will be extracted every time you run the build.");
    }
  }

  private Map<String, File> listPlugins(final File pluginsDir) {
    final Map<String, File> plugins = Maps.newHashMap();
    final File[] foundPlugins = pluginsDir.listFiles(new FileFilter()
    {
      @Override
      public boolean accept(final File file) {
        return file.isDirectory();
      }
    });
    if (foundPlugins != null && foundPlugins.length > 0) {
      for (final File plugin : foundPlugins) {
        final Optional<File> mainJar = getPluginMainJar(plugin);
        if (mainJar.isPresent()) {
          final Optional<String> gaCoordinates = getPluginGACoordinates(mainJar.get());
          if (gaCoordinates.isPresent()) {
            plugins.put(gaCoordinates.get(), plugin);
          }
        }
      }
    }
    return plugins;
  }

  private Optional<String> getPluginGACoordinates(final File mainJar) {
    try {
      final ZipFile jarFile = new ZipFile(mainJar);
      final Enumeration<? extends ZipEntry> entries = jarFile.entries();
      if (entries != null) {
        while (entries.hasMoreElements()) {
          final ZipEntry zipEntry = entries.nextElement();
          if (zipEntry.getName().startsWith("META-INF/maven")
              && zipEntry.getName().endsWith("pom.properties")) {
            final Properties props = new Properties();
            props.load(jarFile.getInputStream(zipEntry));
            return Optional.of(props.getProperty("groupId") + ":" + props.getProperty("artifactId"));
          }
        }
      }
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
    return Optional.absent();
  }

  private Optional<File> getPluginMainJar(final File plugin) {
    final String[] mainJars = plugin.list(new FilenameFilter()
    {
      @Override
      public boolean accept(final File dir, final String name) {
        return name.endsWith(".jar");
      }
    });
    if (mainJars != null && mainJars.length > 0) {
      if (mainJars.length > 1) {
        throw new IllegalStateException(
            "Plugin '" + plugin.getAbsolutePath() + "' contains more then one jar"
        );
      }
      else {
        return Optional.of(new File(plugin, mainJars[0]));
      }
    }
    return Optional.absent();
  }

}
