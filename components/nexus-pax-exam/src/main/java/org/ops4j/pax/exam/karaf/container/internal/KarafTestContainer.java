/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.karaf.container.internal;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.rbc.Constants.RMI_HOST_PROPERTY;
import static org.ops4j.pax.exam.rbc.Constants.RMI_NAME_PROPERTY;
import static org.ops4j.pax.exam.rbc.Constants.RMI_PORT_PROPERTY;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.ops4j.net.FreePort;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Info;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.RelativeTimeout;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.container.remote.RBCRemoteTarget;
import org.ops4j.pax.exam.karaf.container.internal.adaptions.KarafManipulator;
import org.ops4j.pax.exam.karaf.container.internal.adaptions.KarafManipulatorFactory;
import org.ops4j.pax.exam.karaf.container.internal.runner.Runner;
import org.ops4j.pax.exam.karaf.options.DoNotModifyLogOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionBaseConfigurationOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationConsoleOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFilePutOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileReplacementOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationSecurityOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.KarafExamSystemConfigurationOption;
import org.ops4j.pax.exam.karaf.options.KarafFeaturesOption;
import org.ops4j.pax.exam.karaf.options.KeepRuntimeFolderOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.karaf.options.configs.FeaturesCfg;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.PropagateSystemPropertyOption;
import org.ops4j.pax.exam.options.ServerModeOption;
import org.ops4j.pax.exam.options.SystemPackageOption;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.rbc.client.RemoteBundleContextClient;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafTestContainer implements TestContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafTestContainer.class);

    private static final String KARAF_TEST_CONTAINER = "KarafTestContainer.start";
    private static final String EXAM_INJECT_PROPERTY = "pax.exam.inject";
    private static final MavenArtifactUrlReference EXAM_REPO_URL = maven()
        .groupId("org.ops4j.pax.exam").artifactId("pax-exam-features")
        .version(Info.getPaxExamVersion()).type("xml");

    private final Runner runner;
    private final ExamSystem system;
    private KarafDistributionBaseConfigurationOption framework;
    @SuppressWarnings("unused")
    private KarafManipulator versionAdaptions;
    private boolean started;
    private RBCRemoteTarget target;

    private File targetFolder;

    private Registry rgstry;

    public KarafTestContainer(ExamSystem system,
        KarafDistributionBaseConfigurationOption framework, Runner runner) {
        this.framework = framework;
        this.system = system;
        this.runner = runner;
    }

    @Override
    public synchronized TestContainer start() {
        try {
            String name = system.createID(KARAF_TEST_CONTAINER);

            Option invokerConfiguration = getInvokerConfiguration();

            //registry.selectGracefully();
            FreePort freePort = new FreePort(21000, 21099);
            int port = freePort.getPort();
            LOGGER.debug("using RMI registry at port {}", port);
            rgstry = LocateRegistry.createRegistry(port);

            String host = InetAddress.getLocalHost().getHostName();

            ExamSystem subsystem = system
                .fork(options(
                    systemProperty(RMI_HOST_PROPERTY).value(host),
                    systemProperty(RMI_PORT_PROPERTY).value(Integer.toString(port)),
                    systemProperty(RMI_NAME_PROPERTY).value(name),
                    invokerConfiguration,
                    systemProperty(EXAM_INJECT_PROPERTY).value("true"),
                    editConfigurationFileExtend("etc/system.properties", "jline.shutdownhook",
                        "true")));
            target = new RBCRemoteTarget(name, port, subsystem.getTimeout());

            System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");

            URL sourceDistribution = new URL(framework.getFrameworkURL());
            targetFolder = retrieveFinalTargetFolder(subsystem);
            ArchiveExtractor.extract(sourceDistribution, targetFolder);

            File karafBase = searchKarafBase(targetFolder);
            File karafHome = karafBase;

            versionAdaptions = createVersionAdapter(karafBase);
            DependenciesDeployer deployer = new DependenciesDeployer(subsystem, karafBase,
                karafHome);
            deployer.copyBootClasspathLibraries();

            updateLogProperties(karafHome, subsystem);
            setupSystemProperties(karafHome, subsystem);

            List<KarafDistributionConfigurationFileOption> options = new ArrayList<KarafDistributionConfigurationFileOption>(
                Arrays.asList(subsystem.getOptions(KarafDistributionConfigurationFileOption.class)));
            options.addAll(fromFeatureOptions(subsystem.getOptions(KarafFeaturesOption.class)));
            options.addAll(fromFeatureOptions(KarafDistributionOption.features(EXAM_REPO_URL,
                "exam")));

            if (framework.isUseDeployFolder()) {
                deployer.copyReferencedArtifactsToDeployFolder();
            }
            else {
                options.addAll(fromFeatureOptions(deployer.getDependenciesFeature()));
            }

            options.addAll(configureBootDelegation(subsystem));
            options.addAll(configureSystemPackages(subsystem));
            updateUserSetProperties(karafHome, options);

            startKaraf(subsystem, karafBase, karafHome);
            started = true;
        }
        catch (IOException e) {
            throw new RuntimeException("Problem starting container", e);
        }
        return this;
    }

    private KarafManipulator createVersionAdapter(File karafBase) {
        File karafEtc = new File(karafBase, framework.getKarafEtc());
        File distributionInfo = new File(karafEtc, "distribution.info");

        framework = new InternalKarafDistributionConfigurationOption(framework, distributionInfo);
        return KarafManipulatorFactory.createManipulator(framework.getKarafVersion());
    }

    private void startKaraf(ExamSystem subsystem, File karafBase, File karafHome) {
        long startedAt = System.currentTimeMillis();
        File karafBin = new File(karafBase, "bin");
        File karafEtc = new File(karafBase, framework.getKarafEtc());
        File karafData = new File(karafBase, framework.getKarafData());
        String[] classPath = buildKarafClasspath(karafHome);
        makeScriptsInBinExec(karafBin);
        File javaHome = new File(System.getProperty("java.home"));
        String main = framework.getKarafMain();
        String options = "";
        String[] environment = new String[] {};
        ArrayList<String> javaOpts = new ArrayList<String>();
        appendVmSettingsFromSystem(javaOpts, subsystem);
        String[] javaEndorsedDirs = null;
        if (System.getProperty("java.version").startsWith("9")) {
            javaEndorsedDirs = new String[] {};
        } else {
            javaEndorsedDirs = new String[] { javaHome + "/jre/lib/endorsed",
                                                       javaHome + "/lib/endorsed", karafHome + "/lib/endorsed" };
        }
        String[] javaExtDirs = new String[] { javaHome + "/jre/lib/ext", javaHome + "/lib/ext",
            javaHome + "/lib/ext" };
        List<String> opts = Arrays.asList("-Dkaraf.startLocalConsole="
            + shouldLocalConsoleBeStarted(subsystem), "-Dkaraf.startRemoteShell="
            + shouldRemoteShellBeStarted(subsystem));
        boolean enableMBeanServerBuilder = shouldMBeanServerBuilderBeEnabled(subsystem);
        String[] karafOpts = new String[] {};
        runner.exec(environment, karafBase, javaHome.toString(), javaOpts.toArray(new String[] {}),
            javaEndorsedDirs, javaExtDirs, karafHome.toString(), karafData.toString(), karafEtc.toString(),
            karafOpts, opts.toArray(new String[] {}), classPath, main, options,
            enableMBeanServerBuilder);

        LOGGER.debug("Test Container started in " + (System.currentTimeMillis() - startedAt)
            + " millis");
        LOGGER.info("Wait for test container to finish its initialization "
            + subsystem.getTimeout());

        if (subsystem.getOptions(ServerModeOption.class).length == 0) {
            waitForState(org.ops4j.pax.exam.karaf.container.internal.Constants.SYSTEM_BUNDLE,
                Bundle.ACTIVE, subsystem.getTimeout());
        }
        else {
            LOGGER
                .info("System runs in Server Mode. Which means, no Test facility bundles available on target system.");
        }
    }

    private boolean shouldDeleteRuntime() {
        boolean deleteRuntime = true;
        KeepRuntimeFolderOption[] keepRuntimeFolder = system
            .getOptions(KeepRuntimeFolderOption.class);
        if (keepRuntimeFolder != null && keepRuntimeFolder.length != 0) {
            deleteRuntime = false;
        }
        return deleteRuntime;
    }

    private Option getInvokerConfiguration() {
        KarafExamSystemConfigurationOption[] internalConfigurationOptions = system
            .getOptions(KarafExamSystemConfigurationOption.class);
        Option invokerConfiguration = systemProperty("pax.exam.invoker").value("junit");
        if (internalConfigurationOptions != null && internalConfigurationOptions.length != 0) {
            invokerConfiguration = systemProperty("pax.exam.invoker").value(
                internalConfigurationOptions[0].getInvoker());
        }
        return invokerConfiguration;
    }

    private String shouldRemoteShellBeStarted(ExamSystem subsystem) {
        KarafDistributionConfigurationConsoleOption[] consoleOptions = subsystem
            .getOptions(KarafDistributionConfigurationConsoleOption.class);
        if (consoleOptions == null) {
            return "true";
        }
        for (KarafDistributionConfigurationConsoleOption consoleOption : consoleOptions) {
            if (consoleOption.getStartRemoteShell() != null) {
                return consoleOption.getStartRemoteShell() ? "true" : "false";
            }
        }
        return "true";
    }

    private String shouldLocalConsoleBeStarted(ExamSystem subsystem) {
        KarafDistributionConfigurationConsoleOption[] consoleOptions = subsystem
            .getOptions(KarafDistributionConfigurationConsoleOption.class);
        if (consoleOptions == null) {
            return "true";
        }
        for (KarafDistributionConfigurationConsoleOption consoleOption : consoleOptions) {
            if (consoleOption.getStartLocalConsole() != null) {
                return consoleOption.getStartLocalConsole() ? "true" : "false";
            }
        }
        return "true";
    }

    private boolean shouldMBeanServerBuilderBeEnabled(ExamSystem subsystem) {
        KarafDistributionConfigurationSecurityOption[] securityOptions = subsystem
            .getOptions(KarafDistributionConfigurationSecurityOption.class);
        if (securityOptions == null) {
            return false;
        }
        for (KarafDistributionConfigurationSecurityOption securityOption : securityOptions) {
            if (securityOption.getEnableKarafMBeanServerBuilder() != null) {
                return securityOption.getEnableKarafMBeanServerBuilder();
            }
        }
        return false;
    }

    private void makeScriptsInBinExec(File karafBin) {
        if (!karafBin.exists()) {
            return;
        }
        File[] files = karafBin.listFiles();
        for (File file : files) {
            file.setExecutable(true);
        }
    }

    private File retrieveFinalTargetFolder(ExamSystem subsystem) {
        if (framework.getUnpackDirectory() == null) {
            return subsystem.getConfigFolder();
        }
        else {
            File targetDir = new File(framework.getUnpackDirectory() + "/"
                + UUID.randomUUID().toString());
            targetDir = transformToAbsolutePath(targetDir);
            targetDir.mkdirs();
            return targetDir;
        }
    }

    private File transformToAbsolutePath(File file) {
        return new File(file.getAbsolutePath());
    }

    private void appendVmSettingsFromSystem(ArrayList<String> opts, ExamSystem subsystem) {
        VMOption[] options = subsystem.getOptions(VMOption.class);
        for (VMOption option : options) {
            opts.add(option.getOption());
        }
    }

    private void updateUserSetProperties(File karafHome,
        List<KarafDistributionConfigurationFileOption> options) throws IOException {
        HashMap<String, HashMap<String, List<KarafDistributionConfigurationFileOption>>> optionMap = new HashMap<String, HashMap<String, List<KarafDistributionConfigurationFileOption>>>();
        for (KarafDistributionConfigurationFileOption option : options) {
            if (!optionMap.containsKey(option.getConfigurationFilePath())) {
                optionMap.put(option.getConfigurationFilePath(),
                    new HashMap<String, List<KarafDistributionConfigurationFileOption>>());
            }
            HashMap<String, List<KarafDistributionConfigurationFileOption>> optionEntries = optionMap
                .get(option.getConfigurationFilePath());
            if (!optionEntries.containsKey(option.getKey())) {
                optionEntries.put(option.getKey(),
                    new ArrayList<KarafDistributionConfigurationFileOption>());
            }
            else {
                // if special file warn, replace and continue
                if (!option.getConfigurationFilePath().equals(FeaturesCfg.FILE_PATH)) {
                    LOGGER
                        .warn("you're trying to add an additional value to a config file; you're current "
                            + "value will be replaced.");
                    optionEntries.put(option.getKey(),
                        new ArrayList<KarafDistributionConfigurationFileOption>());
                }
            }
            optionEntries.get(option.getKey()).add(option);
        }
        String karafData = framework.getKarafData();
        String karafEtc = framework.getKarafEtc();
        Set<String> configFiles = optionMap.keySet();
        for (String configFile : configFiles) {
            KarafPropertiesFile karafPropertiesFile = new KarafPropertiesFile(karafHome, configFile);
            if (!karafPropertiesFile.exists()) {
                // some property options will come from Pax-Exam and use the default data/etc locations,
                // in those cases when the property file doesn't exist and we have custom data/etc paths
                // we need to consider the custom location and use that - but only if it matches+exists
                KarafPropertiesFile customPropertiesFile = null;
                if (configFile.startsWith("data/") && !configFile.startsWith(karafData)) {
                    customPropertiesFile = new KarafPropertiesFile(karafHome, karafData + configFile.substring(4));
                }
                if (configFile.startsWith("etc/") && !configFile.startsWith(karafEtc)) {
                    customPropertiesFile = new KarafPropertiesFile(karafHome, karafEtc + configFile.substring(3));
                }
                if (customPropertiesFile != null && customPropertiesFile.exists()) {
                    karafPropertiesFile = customPropertiesFile;
                }
            }
            karafPropertiesFile.load();
            Collection<List<KarafDistributionConfigurationFileOption>> optionsToApply = optionMap
                .get(configFile).values();
            boolean store = true;
            for (List<KarafDistributionConfigurationFileOption> optionListToApply : optionsToApply) {
                for (KarafDistributionConfigurationFileOption optionToApply : optionListToApply) {
                    if (optionToApply instanceof KarafDistributionConfigurationFilePutOption) {
                        karafPropertiesFile.put(optionToApply.getKey(), optionToApply.getValue());
                    }
                    else if (optionToApply instanceof KarafDistributionConfigurationFileReplacementOption) {
                        karafPropertiesFile
                            .replace(((KarafDistributionConfigurationFileReplacementOption) optionToApply)
                                .getSource());
                        store = false;
                        break;
                    }
                    else {
                        karafPropertiesFile
                            .extend(optionToApply.getKey(), optionToApply.getValue());
                    }
                }
                if (!store) {
                    break;
                }
            }
            if (store) {
                karafPropertiesFile.store();
            }
        }
    }

    private Collection<? extends KarafDistributionConfigurationFileOption> configureSystemPackages(
        ExamSystem subsystem) {
        String systemPackages = JoinUtil.join(subsystem.getOptions(SystemPackageOption.class));
        if (systemPackages.length() == 0) {
            return Arrays.asList();
        }
        return Arrays.asList(new KarafDistributionConfigurationFileExtendOption(
            CustomProperties.SYSTEM_PACKAGES_EXTRA, systemPackages));
    }

    private Collection<? extends KarafDistributionConfigurationFileOption> configureBootDelegation(
        ExamSystem subsystem) {
        BootDelegationOption[] bootDelegationOptions = subsystem
            .getOptions(BootDelegationOption.class);
        return Arrays.asList(new KarafDistributionConfigurationFileExtendOption(
            CustomProperties.BOOTDELEGATION, JoinUtil.join(bootDelegationOptions)));
    }

    private Collection<? extends KarafDistributionConfigurationFileOption> fromFeatureOptions(
        KarafFeaturesOption... featuresOptions) {
        ArrayList<KarafDistributionConfigurationFileOption> retVal = new ArrayList<KarafDistributionConfigurationFileOption>();

        for (KarafFeaturesOption featuresOption : featuresOptions) {
            retVal.add(new KarafDistributionConfigurationFileExtendOption(FeaturesCfg.REPOSITORIES,
                featuresOption.getURL()));
            retVal.add(new KarafDistributionConfigurationFileExtendOption(FeaturesCfg.BOOT,
                JoinUtil.join(featuresOption.getFeatures())));
        }
        return retVal;
    }

    private void setupSystemProperties(File karafHome, ExamSystem _system) throws IOException {
        File customPropertiesFile = new File(karafHome, framework.getKarafEtc() + "/system.properties");
        SystemPropertyOption[] customProps = _system.getOptions(SystemPropertyOption.class);
        Properties karafPropertyFile = new Properties();
        karafPropertyFile.load(new FileInputStream(customPropertiesFile));
        for (SystemPropertyOption systemPropertyOption : customProps) {
            karafPropertyFile.put(systemPropertyOption.getKey(), systemPropertyOption.getValue());
        }
        for (PropagateSystemPropertyOption option : system.getOptions(PropagateSystemPropertyOption.class)) {
            String key = option.getKey();
            String value = System.getProperty(key);
            if (value != null) {
                karafPropertyFile.put(key, value);
            }
        }

        karafPropertyFile.store(new FileOutputStream(customPropertiesFile), "updated by pax-exam");
    }

    private void updateLogProperties(File karafHome, ExamSystem _system) throws IOException {
        DoNotModifyLogOption[] modifyLog = _system.getOptions(DoNotModifyLogOption.class);
        if (modifyLog != null && modifyLog.length != 0) {
            LOGGER.info("Log file should not be modified by the test framework");
            return;
        }
        String realLogLevel = retrieveRealLogLevel(_system);
        File customPropertiesFile = new File(karafHome, framework.getKarafEtc() + "/org.ops4j.pax.logging.cfg");
        Properties karafPropertyFile = new Properties();
        karafPropertyFile.load(new FileInputStream(customPropertiesFile));
        karafPropertyFile.put("log4j.rootLogger", realLogLevel + ", out, stdout, osgi:*");
        karafPropertyFile.store(new FileOutputStream(customPropertiesFile), "updated by pax-exam");
    }

    private String retrieveRealLogLevel(ExamSystem _system) {
        LogLevelOption[] logLevelOptions = _system.getOptions(LogLevelOption.class);
        return logLevelOptions != null && logLevelOptions.length != 0 ? logLevelOptions[0]
            .getLogLevel().toString() : "WARN";
    }

    private String[] buildKarafClasspath(File karafHome) {
        List<String> cp = new ArrayList<String>();
        File[] jars = new File(karafHome + "/lib").listFiles((FileFilter) new WildcardFileFilter(
            "*.jar"));
        for (File jar : jars) {
            cp.add(jar.toString());
        }
        // do the same for lib/boot
        File[] bootJars = new File(karafHome + "/lib/boot")
                .listFiles((FileFilter) new WildcardFileFilter("*.jar"));
        if (bootJars != null) {
            for (File jar : bootJars) {
                cp.add(jar.toString());
            }
        }
        // do the same for lib/ext
        File[] extJars = new File(karafHome + "/lib/ext")
            .listFiles((FileFilter) new WildcardFileFilter("*.jar"));
        if (extJars != null) {
            for (File jar : extJars) {
                cp.add(jar.toString());
            }
        }
        return cp.toArray(new String[] {});
    }

    /**
     * Since we might get quite deep use a simple breath first search algorithm
     */
    private File searchKarafBase(File _targetFolder) {
        Queue<File> searchNext = new LinkedList<File>();
        searchNext.add(_targetFolder);
        while (!searchNext.isEmpty()) {
            File head = searchNext.poll();
            if (!head.isDirectory()) {
                continue;
            }
            boolean isSystem = false;
            boolean etc = false;
            for (File file : head.listFiles()) {
                if (file.isDirectory() && file.getName().equals("system")) {
                    isSystem = true;
                }
                if (file.isDirectory() && file.getName().equals("etc")) {
                    etc = true;
                }
            }
            if (isSystem && etc) {
                return head;
            }
            searchNext.addAll(Arrays.asList(head.listFiles()));
        }
        throw new IllegalStateException("No karaf base dir found in extracted distribution.");
    }

    @Override
    public synchronized TestContainer stop() {
        LOGGER.debug("Shutting down the test container (Pax Runner)");
        try {
            if (started) {
                target.stop();
                RemoteBundleContextClient remoteBundleContextClient = target.getClientRBC();
                if (remoteBundleContextClient != null) {
                    remoteBundleContextClient.stop();

                }
                if (runner != null) {
                    runner.shutdown();
                }
                try {
                    UnicastRemoteObject.unexportObject(rgstry, true);
                    /*
                     * NOTE: javaRunner.waitForExit() works for Equinox and Felix, but not for Knopflerfish,
                     * need to investigate why. OTOH, it may be better to kill the process as we're doing
                     * now, just to be on the safe side.
                     */
                }
                catch (NoSuchObjectException exc) {
                    throw new TestContainerException(exc);
                }

            }
            else {
                throw new RuntimeException("Container never came up");
            }
        }
        finally {
            started = false;
            target = null;
            if (shouldDeleteRuntime()) {
                system.clear();
                try {
                    FileUtils.forceDelete(targetFolder);
                }
                catch (IOException e) {
                    forceCleanup();
                }
            }
        }
        return this;
    }

    private void forceCleanup() {
        LOGGER.info("Can't remove runtime system; shedule it for exit of the jvm.");
        try {
            FileUtils.forceDeleteOnExit(targetFolder);
        }
        catch (IOException e1) {
            LOGGER.error("Well, this should simply not happen...");
        }
    }

    private void waitForState(final long bundleId, final int state, final RelativeTimeout timeout) {
        target.getClientRBC().waitForState(bundleId, state, timeout);
    }
    

    @Override
    public synchronized void call(TestAddress address) {
        target.call(address);
    }

    @Override
    public synchronized long install(InputStream stream) {
        return install("local", stream);
    }

    @Override
    public synchronized long install(String location, InputStream stream) {
        return target.install(location, stream);
    }

    @Override
    public String toString() {
        return "KarafTestContainer{" + framework.getFrameworkURL() + "}";
    }

    @Override
    public long installProbe(InputStream stream) {
        return target.installProbe(stream);
    }

    @Override
    public void uninstallProbe() {
        target.uninstallProbe();
    }

}
