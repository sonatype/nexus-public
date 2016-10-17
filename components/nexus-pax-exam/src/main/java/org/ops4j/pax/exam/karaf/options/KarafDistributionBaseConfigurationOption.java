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
package org.ops4j.pax.exam.karaf.options;

import java.io.File;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenUrlReference;

public class KarafDistributionBaseConfigurationOption implements Option {

    protected String frameworkURL;
    protected MavenUrlReference frameworkURLReference;
    protected String name;
    protected String karafVersion;
    protected String karafMain = "org.apache.karaf.main.Main";
    protected String karafData = "data";
    protected String karafEtc = "etc";
    protected File unpackDirectory;
    protected boolean useDeployFolder;
    protected boolean runEmbedded;

    public KarafDistributionBaseConfigurationOption() {
        frameworkURL = null;
        frameworkURLReference = null;
        name = null;
        karafVersion = null;
    }

    public KarafDistributionBaseConfigurationOption(String frameworkURL, String name,
        String karafVersion) {
        this.frameworkURL = frameworkURL;
        frameworkURLReference = null;
        this.name = name;
        this.karafVersion = karafVersion;
    }

    public KarafDistributionBaseConfigurationOption(MavenUrlReference frameworkURLReference,
        String name, String karafVersion) {
        frameworkURL = null;
        this.frameworkURLReference = frameworkURLReference;
        this.name = name;
        this.karafVersion = karafVersion;
    }

    public KarafDistributionBaseConfigurationOption(MavenUrlReference frameworkURLReference) {
        frameworkURL = null;
        this.frameworkURLReference = frameworkURLReference;
    }

    /**
     * Simply clones the inserted {@link KarafDistributionConfigurationOption}
     *
     * @param base
     *            option to be cloned
     */
    public KarafDistributionBaseConfigurationOption(KarafDistributionBaseConfigurationOption base) {
        frameworkURL = base.frameworkURL;
        frameworkURLReference = base.frameworkURLReference;
        name = base.name;
        karafVersion = base.karafVersion;
        karafMain = base.karafMain;
        karafData = base.karafData;
        karafEtc = base.karafEtc;
        unpackDirectory = base.unpackDirectory;
        useDeployFolder = base.useDeployFolder;
    }

    /**
     * Sets the URL of the framework as a String (for example a file).
     *
     * @param _frameworkURL
     *            framework URL
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption frameworkUrl(String _frameworkURL) {
        this.frameworkURL = _frameworkURL;
        return this;
    }

    /**
     * Sets the URL of the frameworks as a maven reference.
     *
     * @param _frameworkURL
     *            framework URL
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption frameworkUrl(MavenUrlReference _frameworkURL) {
        frameworkURLReference = _frameworkURL;
        return this;
    }

    /**
     * Sets the name of the framework. This is only used for logging.
     *
     * @param _name
     *            framework name
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption name(String _name) {
        this.name = _name;
        return this;
    }

    /**
     * The version of karaf used by the framework. That one is required since there is the high
     * possibility that configuration is different between various karaf versions.
     *
     * @param _karafVersion
     *            Karaf version
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption karafVersion(String _karafVersion) {
        this.karafVersion = _karafVersion;
        return this;
    }

    /**
     * The main entry-point used by the framework. Defaults to {@code "org.apache.karaf.main.Main"}.
     *
     * @param _karafMain
     *            Karaf main
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption karafMain(String _karafMain) {
        this.karafMain = _karafMain;
        return this;
    }

    /**
     * Sets the location of karaf.data relative to the installation. Defaults to {@code "data"}.
     *
     * @param _karafData
     *            Karaf data
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption karafData(String _karafData) {
        this.karafData = _karafData;
        return this;
    }

    /**
     * Sets the location of karaf.etc relative to the installation. Defaults to {@code "etc"}.
     *
     * @param _karafEtc
     *            Karaf etc
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption karafEtc(String _karafEtc) {
        this.karafEtc = _karafEtc;
        return this;
    }

    /**
     * Define the unpack directory for the karaf distribution. In this directory a UUID named
     * directory will be created for each environment.
     *
     * @param _unpackDirectory
     *            unpack directory
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption unpackDirectory(File _unpackDirectory) {
        this.unpackDirectory = _unpackDirectory;
        return this;
    }

    /**
     * Per default the framework simply copies all referenced artifacts (via Pax Exam
     * DistributionOption) to the deploy folder of the karaf (based) distribution. If you don't have
     * such a folder (for any reason) you can set this option to false. PaxExam Karaf will then try
     * to add those deployment urls directly to a features xml instead of copying those files to the
     * deploy folder.
     *
     * @param _useDeployFolder
     *            shall artifacts be copied to deploy folder?
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption useDeployFolder(boolean _useDeployFolder) {
        this.useDeployFolder = _useDeployFolder;
        return this;
    }

    /**
     * Per default the framework will run Karaf as forked Java process. This can be used to switch
     * to run Karaf as embedded instance.
     *
     * @param _runEmbedded
     *            shall Karaf be run as embedded instance.
     * @return this for fluent syntax
     */
    public KarafDistributionBaseConfigurationOption runEmbedded(boolean _runEmbedded) {
        this.runEmbedded = _runEmbedded;
        return this;
    }

    public String getFrameworkURL() {
        if (frameworkURL == null && frameworkURLReference == null) {
            throw new IllegalStateException(
                "Either frameworkurl or frameworkUrlReference need to be set.");
        }
        return frameworkURL != null ? frameworkURL : frameworkURLReference.getURL();
    }

    public String getName() {
        return name;
    }

    public String getKarafVersion() {
        return karafVersion;
    }

    public String getKarafMain() {
      return karafMain;
    }

    public String getKarafData() {
      return karafData;
    }

    public String getKarafEtc() {
      return karafEtc;
    }

    public File getUnpackDirectory() {
        return unpackDirectory;
    }

    public boolean isUseDeployFolder() {
        return useDeployFolder;
    }

    public boolean isRunEmbedded() {
        return runEmbedded;
    }

}
