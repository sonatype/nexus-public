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
package org.sonatype.nexus.bundle.launcher.support;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.bootstrap.Launcher;
import org.sonatype.nexus.bootstrap.monitor.CommandMonitorTalker;
import org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread;
import org.sonatype.nexus.bootstrap.monitor.KeepAliveThread;
import org.sonatype.nexus.bootstrap.monitor.commands.ExitCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.HaltCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.PingCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.StopApplicationCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.StopMonitorCommand;
import org.sonatype.nexus.bundle.launcher.NexusBundle;
import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.bundle.launcher.internal.NexusITLauncher;
import org.sonatype.sisu.bl.jmx.JMXConfiguration;
import org.sonatype.sisu.bl.jsw.JSWConfig;
import org.sonatype.sisu.bl.support.DefaultWebBundle;
import org.sonatype.sisu.bl.support.RunningBundles;
import org.sonatype.sisu.bl.support.TimedCondition;
import org.sonatype.sisu.bl.support.port.PortReservationService;
import org.sonatype.sisu.filetasks.FileTaskBuilder;
import org.sonatype.sisu.goodies.common.Time;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread.LOCALHOST;
import static org.sonatype.sisu.bl.BundleConfiguration.RANDOM_PORT;
import static org.sonatype.sisu.bl.jsw.JSWConfig.WRAPPER_JAVA_MAINCLASS;
import static org.sonatype.sisu.filetasks.FileTaskRunner.onDirectory;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;
import static org.sonatype.sisu.filetasks.builder.FileRef.path;
import static org.sonatype.sisu.goodies.common.SimpleFormat.format;

/**
 * Default Nexus bundle implementation.
 *
 * @since 2.0
 */
@Named
public class DefaultNexusBundle
    extends DefaultWebBundle<NexusBundle, NexusBundleConfiguration>
    implements NexusBundle
{

  private static final Logger log = LoggerFactory.getLogger(DefaultNexusBundle.class);

  private static final String USE_BUNDLE_PLUGINS_IF_PRESENT = "useBundlePluginsIfPresent";

  private static final int COMMAND_MONITOR_READY_CHECK_INITIAL_DELAY_SECONDS = 1;

  private static final int COMMAND_MONITOR_READY_CHECK_TIMEOUT_SECONDS = 15;

  private static final int COMMAND_MONITOR_READY_CHECK_INTERVAL_SECONDS = 1;

  /**
   * File task builder.
   * Cannot be null.
   */
  private final FileTaskBuilder fileTaskBuilder;

  /**
   * Whether plugins installed in "sonatype-work/nexus/plugin-repository" should not be used in case they are present
   * in "nexus/WEB-INF/plugin-repository". This is mainly used by tests that wish to test against a bundle that
   * already contains the plugins installed by tests.
   */
  private final Boolean useBundlePluginsIfPresent;

  /**
   * Port on which Nexus Command Monitor is running. 0 (zero) if application is not running.
   */
  private int commandMonitorPort;

  /**
   * Port on which Nexus Keep Alive is running. 0 (zero) if application is not running.
   */
  private int keepAlivePort;

  /**
   * Keep alive thread. Null if server is not started.
   */
  private CommandMonitorThread keepAliveThread;

  private ConfigurationStrategy strategy;

  /**
   * SSL port if HTTPS support is enabled, otherwise -1.
   */
  private int sslPort;

  /**
   * Secure URL or null
   */
  private URL secureUrl;

  @Inject
  public DefaultNexusBundle(final Provider<NexusBundleConfiguration> configurationProvider,
                            final RunningBundles runningBundles,
                            final FileTaskBuilder fileTaskBuilder,
                            final PortReservationService portReservationService,
                            final @Named("${" + USE_BUNDLE_PLUGINS_IF_PRESENT
                                + ":-false}") Boolean useBundlePluginsIfPresent)
  {
    super("nexus", configurationProvider, runningBundles, fileTaskBuilder, portReservationService);
    this.fileTaskBuilder = fileTaskBuilder;
    this.useBundlePluginsIfPresent = useBundlePluginsIfPresent;
  }

  private static void installStopShutdownHook(final int commandPort) {
    Thread stopShutdownHook = new Thread("JSW Sanity Stopper")
    {
      @Override
      public void run() {
        terminateRemoteNexus(commandPort);
      }
    };

    Runtime.getRuntime().addShutdownHook(stopShutdownHook);
    log.debug("Installed stop shutdown hook");
  }

  private static void sendStopToNexus(final int commandPort) {
    log.debug("Sending stop command to Nexus");
    try {
      // FIXME HOSTNAME should be configurable
      new CommandMonitorTalker(LOCALHOST, commandPort).send(StopApplicationCommand.NAME);
    }
    catch (Exception e) {
      log.debug(
          "Skipping exception got while sending stop command to Nexus {}:{}",
          e.getClass().getName(), e.getMessage()
      );
    }
  }

  // FIXME accept host to terminate
  private static void terminateRemoteNexus(final int commandPort) {
    log.debug("attempting to terminate gracefully at {}", commandPort);

    // First attempt graceful shutdown
    sendStopToNexus(commandPort);

    // FIXME LOCALHOST should be getHostName
    CommandMonitorTalker talker = new CommandMonitorTalker(LOCALHOST, commandPort);
    long started = System.currentTimeMillis();
    long max = 5 * 60 * 1000; // wait 5 minutes for NX to shutdown, before attempting to halt it
    long period = 1000;

    // Then ping for a bit and finally give up and ask it to halt
    while (true) {
      try {
        talker.send(PingCommand.NAME);
      }
      catch (ConnectException e) {
        // likely its shutdown already
        break;
      }
      catch (Exception e) {
        // ignore, not sure there is much we can do
      }

      // If we have waited long enough, then ask remote to halt
      if (System.currentTimeMillis() - started > max) {
        try {
          talker.send(HaltCommand.NAME);
          break;
        }
        catch (Exception e) {
          // ignore, not sure there is much we can do
          break;
        }
      }

      // Wait a wee bit and try again
      try {
        Thread.sleep(period);
      }
      catch (InterruptedException e) {
        // ignore
      }
    }
  }

  private static void sendStopToKeepAlive(final int commandPort) {
    log.debug("Sending stop command to keep alive thread");
    try {
      // FIXME replace LOCALHOST with getHostName
      new CommandMonitorTalker(LOCALHOST, commandPort).send(StopMonitorCommand.NAME);
    }
    catch (Exception e) {
      log.debug(
          "Skipping exception got while sending stop command to keep alive thread {}:{}",
          e.getClass().getName(), e.getMessage()
      );
    }
  }

  /**
   * Gets the configured context path for this bundle.
   * <p/>
   * The default context path if not explicitly configured is "/" + {@link #getName}.
   *
   * @return webapp context path where this bundle is rooted always starting with a slash
   */
  protected @NotNull String getContextPath() {
    return getConfiguration().getContextPath() == null ? "/" + getName() : getConfiguration().getContextPath();
  }

  @Override
  protected String composeApplicationURL() {
    return String.format("http://%s:%s%s/", getConfiguration().getHostName(), getPort(), getContextPath());
  }

  @Override
  public URL getSecureUrl() {
    return this.secureUrl;
  }


  /**
   * Additionally <br/>
   * - configures Nexus/Jetty port<br/>
   * - installs command monitor<br/>
   * - installs keep alive monitor<br/>
   * - configure remote debugging if requested
   * <p/>
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  protected void configure()
      throws Exception
  {
    super.configure();

    commandMonitorPort = getPortReservationService().reservePort();
    keepAlivePort = getPortReservationService().reservePort();

    strategy = determineConfigurationStrategy();

    if (getConfiguration().getSslPort() == RANDOM_PORT) {
      sslPort = portReservationService.reservePort();
    }
    else {
      sslPort = getConfiguration().getSslPort();
    }

    if (getSslPort() > 0) {
      this.secureUrl = new URL(
          String.format("https://%s:%s%s/", getConfiguration().getHostName(), getSslPort(), getContextPath()));
    }

    configureJSW(strategy);
    configureNexusProperties(strategy);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  protected void unconfigure() {
    super.unconfigure();

    if (commandMonitorPort > 0) {
      getPortReservationService().cancelPort(commandMonitorPort);
      commandMonitorPort = 0;
    }
    if (keepAlivePort > 0) {
      getPortReservationService().cancelPort(keepAlivePort);
      keepAlivePort = 0;
    }
    strategy = null;

    if (getConfiguration().getSslPort() == RANDOM_PORT && sslPort > 0) {
      getPortReservationService().cancelPort(sslPort);
    }
    sslPort = 0;
  }

  /**
   * Starts Nexus using JSW.
   * <p/>
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  protected void startApplication() {
    try {
      keepAliveThread = new CommandMonitorThread(
          keepAlivePort,
          new PingCommand(),
          new StopMonitorCommand(),
          new ExitCommand(),
          new HaltCommand()
      );
      keepAliveThread.start();
    }
    catch (IOException e) {
      throw new RuntimeException("Could not start JSW keep alive thread", e);
    }
    installStopShutdownHook(commandMonitorPort);

    final File nexusDir = getNexusDirectory();

    makeExecutable(nexusDir, "nexus");
    makeExecutable(nexusDir, "wrapper");

    // log whenever ports are configured to aid solving test port conflicts
    log.info("{} ({}) spawned env [{}={},{}={}]", getName(), getConfiguration().getId(),
        strategy.commandMonitorProperty(), commandMonitorPort, strategy.keepAliveProperty(), keepAlivePort);
    onDirectory(nexusDir).apply(
        fileTaskBuilder.exec().spawn()
            .script(path("bin/nexus" + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".bat" : "")))
            .withArgument("console")
            .withEnv(strategy.commandMonitorProperty(), String.valueOf(commandMonitorPort))
            .withEnv(strategy.keepAliveProperty(), String.valueOf(keepAlivePort))
    );

    if (getConfiguration().isSuspendOnStart()) {
      // verify the debugger socket has been opened and is waiting for a debugger to connect
      // command monitor thread is not started while suspended so this is the best we can do
      final boolean jvmSuspended = new TimedCondition()
      {
        @Override
        protected boolean isSatisfied()
            throws Exception
        {
          Socket socket = new Socket();
          socket.setSoTimeout(5000);
          socket.connect(
              new InetSocketAddress(getConfiguration().getHostName(), getConfiguration().getDebugPort()));
          return true;
        }
      }.await(Time.seconds(1), Time.seconds(10), Time.seconds(1));
      if (jvmSuspended) {
        log.info("{} ({}) suspended for debugging at {}:{}", getName(), getConfiguration().getId(),
            getConfiguration().getHostName(), getConfiguration().getDebugPort());
      }
      else {
        throw new RuntimeException(
            format(
                "%s (%s) no open socket for debugging at %s:%s within 10 seconds", getName(),
                getConfiguration().getId(), getConfiguration().getHostName(),
                getConfiguration().getDebugPort()
            )
        );
      }
    }
    else {
      // when not suspending, we expect the internal command monitor thread to start well before bundle is ready
      log.info("{} ({}) pinging command monitor at {}:{}", getName(), getConfiguration().getId(),
          getConfiguration().getHostName(), commandMonitorPort);
      final boolean monitorInstalled = new TimedCondition()
      {
        @Override
        protected boolean isSatisfied()
            throws Exception
        {
          // FIXME replace LOCALHOST with getHostName() after making default hostname be 127.0.0.1
          new CommandMonitorTalker(LOCALHOST, commandMonitorPort).send(PingCommand.NAME);
          return true;
        }
      }.await(Time.seconds(COMMAND_MONITOR_READY_CHECK_INITIAL_DELAY_SECONDS),
          Time.seconds(COMMAND_MONITOR_READY_CHECK_TIMEOUT_SECONDS),
          Time.seconds(COMMAND_MONITOR_READY_CHECK_INTERVAL_SECONDS));
      if (monitorInstalled) {
        log.debug("{} ({}) command monitor detected at {}:{}", getName(), getConfiguration().getId(),
            getConfiguration().getHostName(), commandMonitorPort);
      }
      else {
        throw new RuntimeException(
            format("%s (%s) no command monitor detected at %s:%s within %s seconds", getName(),
                getConfiguration().getId(), getConfiguration().getHostName(),
                commandMonitorPort, COMMAND_MONITOR_READY_CHECK_TIMEOUT_SECONDS
            )
        );
      }
    }
  }

  /**
   * Stops Nexus using JSW.
   * <p/>
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  protected void stopApplication() {
    // application may be in suspended state waiting for debugger to attach, if we can reasonably guess this is
    // the case, then we should resume the vm so that we can ask command monitor to immediately halt
    try {
      if (getConfiguration().isSuspendOnStart()) {

        boolean isSuspended = new TimedCondition()
        {
          @Override
          protected boolean isSatisfied()
              throws Exception
          {
            Socket socket = new Socket();
            socket.setSoTimeout(5000);
            socket.connect(
                new InetSocketAddress(getConfiguration().getHostName(),
                    getConfiguration().getDebugPort()));
            return true;
          }
        }.await(Time.seconds(1), Time.seconds(10), Time.seconds(1));

        if (isSuspended) {
          // FIXME avoiding the compile time dependency for now on jdi classes (DebuggerUtils)
          throw new RuntimeException(
              format(
                  "%s (%s) looks suspended at {}:{}, CANNOT STOP THIS BUNDLE!", getName(),
                  getConfiguration().getId(), getConfiguration().getHostName(),
                  getConfiguration().getDebugPort()
              )
          );
        }
      }
      terminateRemoteNexus(commandMonitorPort);

    }
    finally {
      // Stop the launcher-controller-side monitor thread if there is one
      if (keepAliveThread != null) {
        sendStopToKeepAlive(keepAlivePort);
        keepAliveThread = null;
      }
    }
  }

  /**
   * Checks if Nexus is alive by using REST status service.
   *
   * @return true if Nexus is alive
   * @since 2.0
   */
  @Override
  protected boolean applicationAlive() {
    return RequestUtils.isNexusRESTStarted(getUrl().toExternalForm());
  }

  @Override
  public void doPrepare() {
    super.doPrepare();
    if (useBundlePluginsIfPresent) {
      removePluginsPresentInBundle();
    }
  }

  private void removePluginsPresentInBundle() {
    final Map<String, File> workDirPlugins = listPlugins(
        new File(getWorkDirectory(), "plugin-repository")
    );
    if (workDirPlugins.size() > 0) {
      final Map<String, File> bundlePlugins = listPlugins(
          new File(getNexusDirectory(), "nexus/WEB-INF/plugin-repository")
      );
      for (final Map.Entry<String, File> entry : workDirPlugins.entrySet()) {
        if (bundlePlugins.containsKey(entry.getKey())) {
          log.info(
              "{} ({}) removing plugin '{}' already present in extracted bundle", getName(), getConfiguration().getId(),
              entry.getValue().getName()
          );
          fileTaskBuilder.delete().directory(file(entry.getValue())).run();
        }
      }
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

  /**
   * Configure Nexus properties using provided configuration strategy.
   *
   * @param strategy configuration strategy
   */
  private void configureNexusProperties(final ConfigurationStrategy strategy) {
    strategy.configureNexus();
  }

  /**
   * Configure JSW properties using provided configuration strategy.
   *
   * @param strategy configuration strategy
   */
  private void configureJSW(final ConfigurationStrategy strategy) {
    try {
      final NexusBundleConfiguration config = getConfiguration();

      final File jswConfigFile = new File(config.getTargetDirectory(), "nexus/bin/jsw/conf/wrapper.conf");

      final JSWConfig jswConfig = new JSWConfig(
          jswConfigFile,
          "The following properties are added by Nexus IT as an override of properties already configured"
      ).load();

      strategy.configureJSW(jswConfig);

      jswConfig.save();
    }
    catch (final IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Determines a configuration strategy based on version of Nexus to be started.
   *
   * @return configuration strategy. Never null.
   */
  private ConfigurationStrategy determineConfigurationStrategy() {
    return new CS22AndAbove();
  }

  @Override
  public File getWorkDirectory() {
    return new File(getConfiguration().getTargetDirectory(), "sonatype-work/nexus");
  }

  public File getNexusDirectory() {
    return new File(getConfiguration().getTargetDirectory(), "nexus");
  }

  @Override
  public File getNexusLog() {
    return new File(getWorkDirectory(), "logs/nexus.log");
  }

  @Override
  public File getLauncherLog() {
    return new File(getNexusDirectory(), "logs/wrapper.log");
  }

  @Override
  public int getSslPort() {
    return sslPort;
  }

  @Override
  protected String generateId() {
    return "nx"; // TODO? use a system property if we should or not add: + "-" + System.currentTimeMillis();
  }

  private void makeExecutable(final File baseDir,
                              final String scriptName)
  {
    if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
      onDirectory(baseDir).apply(
          fileTaskBuilder.chmod(path("/"))
              .include("**/" + scriptName)
              .permissions("u+x")
      );
    }
  }

  private static interface ConfigurationStrategy
  {
    String commandMonitorProperty();

    String keepAliveProperty();

    void configureJSW(JSWConfig jswConfig);

    void configureNexus();
  }

  private abstract class ConfigurationStrategySupport
      implements ConfigurationStrategy
  {
    @Override
    public void configureJSW(final JSWConfig jswConfig) {
      // configure remote debug if requested
      if (getConfiguration().getDebugPort() > 0) {
        jswConfig.addJavaStartupParameter("-Xdebug");
        jswConfig.addJavaStartupParameter("-Xnoagent");
        jswConfig.addJavaStartupParameter(
            "-Xrunjdwp:transport=dt_socket,server=y,suspend="
                + (getConfiguration().isSuspendOnStart() ? "y" : "n")
                + ",address=" + getConfiguration().getDebugPort()
        );
        jswConfig.addJavaSystemProperty("java.compiler", "NONE");
      }

      JMXConfiguration jmxConfig = getConfiguration().getJmxConfiguration();
      if (jmxConfig.getRemotePort() != null) {
        Map<String, String> jmxProps = jmxConfig.getSystemProperties();
        jmxProps.put(JMXConfiguration.PROP_COM_SUN_MANAGEMENT_JMXREMOTE_PORT, Integer.toString(getJmxRemotePort()));
        jswConfig.addJavaSystemProperties(jmxProps);
      }

      if (getSslPort() > 0) {
        jswConfig.addIndexedProperty("wrapper.app.parameter", "./conf/jetty-https.xml");
      }
    }
  }

  private class CS22AndAbove
      extends ConfigurationStrategySupport
      implements ConfigurationStrategy
  {

    @Override
    public String commandMonitorProperty() {
      return Launcher.COMMAND_MONITOR_PORT;
    }

    @Override
    public String keepAliveProperty() {
      return KeepAliveThread.KEEP_ALIVE_PORT;
    }

    @Override
    public void configureNexus() {
      final Properties nexusProperties = new Properties();

      nexusProperties.setProperty("application-port", String.valueOf(getPort()));
      nexusProperties.setProperty("application-port-ssl", String.valueOf(getSslPort()));
      nexusProperties.setProperty(commandMonitorProperty(), String.valueOf(commandMonitorPort));
      nexusProperties.setProperty(keepAliveProperty(), String.valueOf(keepAlivePort));
      if (getConfiguration().getContextPath() != null) {
        nexusProperties.setProperty("nexus-webapp-context-path", getConfiguration().getContextPath());
      }

      final Map<String, String> systemProperties = getConfiguration().getSystemProperties();
      if (!systemProperties.isEmpty()) {
        for (final Map.Entry<String, String> entry : systemProperties.entrySet()) {
          nexusProperties.setProperty(entry.getKey(), entry.getValue() == null ? "true" : entry.getValue());
        }
      }

      onDirectory(getConfiguration().getTargetDirectory()).apply(
          fileTaskBuilder.properties(path("nexus/conf/nexus-test.properties"))
              .properties(nexusProperties)
      );
    }

  }

  private class CS21AndBellow
      extends ConfigurationStrategySupport
      implements ConfigurationStrategy
  {

    @Override
    public String commandMonitorProperty() {
      return NexusITLauncher.COMMAND_MONITOR_PORT;
    }

    @Override
    public String keepAliveProperty() {
      return KeepAliveThread.KEEP_ALIVE_PORT;
    }

    @Override
    public void configureJSW(final JSWConfig jswConfig) {
      String mainClass = jswConfig.getProperty(WRAPPER_JAVA_MAINCLASS);
      if (!NexusITLauncher.class.getName().equals(mainClass)) {
        jswConfig.setJavaMainClass(NexusITLauncher.class);
        jswConfig.addJavaSystemProperty(NexusITLauncher.LAUNCHER, mainClass);

        jswConfig.addToJavaClassPath(NexusITLauncher.class);
        jswConfig.addToJavaClassPath(Launcher.class);
      }

      jswConfig.addJavaSystemProperties(getConfiguration().getSystemProperties());

      super.configureJSW(jswConfig);

    }

    @Override
    public void configureNexus() {
      onDirectory(getConfiguration().getTargetDirectory()).apply(
          fileTaskBuilder.properties(path("nexus/conf/nexus.properties"))
              .property("application-port", String.valueOf(getPort()))
      );
    }

  }

}
