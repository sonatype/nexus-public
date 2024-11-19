/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * CoreUi plugin strings.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.app.PluginStrings', {
  '@aggregate_priority': 90,

  singleton: true,
  requires: [
    'NX.I18n'
  ],

  /**
   * String keys.
   *
   * Keys follow the following naming convention:
   *
   * Class_Name>_[Component_or_Attribute]: string
   *
   * @type {Object}
   */
  keys: {
    // Browse -> Browse
    Assets_Info_Repository: 'Repository',
    Assets_Info_Format: 'Format',
    Assets_Info_Group: 'Component Group',
    Assets_Info_Name: 'Component Name',
    Assets_Info_Version: 'Component Version',
    Assets_Info_Path: 'Path',
    Assets_Info_ContentType: 'Content type',
    Assets_Info_FileSize: 'File size',
    Assets_Info_Last_Downloaded: 'Last downloaded',
    Assets_Info_No_Downloads: 'has not been downloaded',
    Assets_Info_Locally_Cached: 'Locally cached',
    Assets_Info_BlobRef: 'Blob reference',
    Assets_Info_Blob_Created: 'Blob created',
    Assets_Info_Blob_Updated: 'Blob updated',
    Assets_Info_ContainingRepositoryName: 'Containing repo',
    Assets_Info_Downloaded_Count: 'Last 30 days',
    Assets_Info_Downloaded_Unit: 'downloads',
    Assets_Info_UploadedBy: 'Uploader',
    Assets_Info_UploadedIp: 'Uploader\'s IP Address',
    AssetInfo_Delete_Button: 'Delete asset',
    AssetInfo_Delete_Title: 'Confirm deletion?',
    AssetInfo_Delete_Success: 'Asset deleted: {0}',
    FolderInfo_Delete_Button: 'Delete folder',
    FolderInfo_Delete_Title: 'Delete the entire folder?',
    FolderInfo_Delete_Text: 'All assets you have permission to delete under folder \'{0}\' will be removed. The view will not automatically refresh to show progress. This operation cannot be undone.',
    FolderInfo_Delete_Success: 'The folder is now being deleted in the background',
    Component_Asset_Tree_Title_Feature: 'Tree',
    Component_Asset_Tree_Description_Feature: 'View tree layout of components and assets',
    Component_Asset_Tree_EmptyText_View: 'No component/assets found in repository',
    Component_Asset_Tree_Expand_Failure: 'Unable to show requested tree entry',
    Component_Asset_Tree_Filtered_EmptyText_View: 'All components have been filtered out, try using <a href="#browse/search">search</a> instead?',
    Component_Asset_Tree_Results_Warning: 'There may be additional results, try filtering the results or searching if you cannot find what you\'re looking for.',
    Component_Asset_Tree_Html_View: 'HTML View',
    Component_Asset_Tree_Upload_Component: 'Upload component',

    ComponentDetails_Delete_Button: 'Delete component',
    ComponentDetails_Analyze_Button: 'Analyze application',
    ComponentDetails_View_Vulnerabilities_Button: 'View Vulnerabilities',
    ComponentDetails_View_Vulnerabilities_Count_Button: 'View {0} Vulnerabilities',
    ComponentDetails_Browse_Snapshots_Button: 'Browse SNAPSHOT(s)',
    ComponentDetails_Delete_Body: 'This will delete all asset(s) associated with the component: {0}',
    ComponentDetails_Delete_Title: 'Confirm deletion?',
    ComponentDetails_Delete_Success: 'Component deleted: {0}',
    ComponentDetails_Analyze_Success: 'Analysis in process. Email will be sent when report is ready.',
    ComponentDetails_Loading_Mask: 'Loading...',
    ComponentDetails_Rebuild_Warning: 'Browse tree is being rebuilt; results may not be complete until the rebuild is finished.',

    ComponentUtils_Delete_Button_Unauthenticated: 'Please sign in first',
    ComponentUtils_Delete_Asset_No_Permissions: 'You do not have permission to delete this asset',
    ComponentUtils_Delete_Component_No_Permissions: 'You do not have permission to delete this component',

    AnalyzeApplication_Button_Unauthenticated: 'Please sign in first',

    AnalyzeApplicationWindow_Title: 'Analyze Application',
    AnalyzeApplicationWindow_Form_Asset_FieldLabel: 'Application asset',
    AnalyzeApplicationWindow_Form_Asset_HelpText: 'Select the asset that contains the application',
    AnalyzeApplicationWindow_Form_Asset_EmptyText: 'Select an asset',
    AnalyzeApplicationWindow_Form_Email_FieldLabel: 'Email address',
    AnalyzeApplicationWindow_Form_Email_HelpText: 'The address where the summary report will be sent',
    AnalyzeApplicationWindow_Form_Password_FieldLabel: 'Report password',
    AnalyzeApplicationWindow_Form_Password_HelpText: 'A password to gain access to the detailed report',
    AnalyzeApplicationWindow_Form_ProprietaryPackages_FieldLabel: 'Proprietary packages',
    AnalyzeApplicationWindow_Form_ProprietaryPackages_HelpText: 'A comma separated list of proprietary packages',
    AnalyzeApplicationWindow_Form_Label_FieldLabel: 'Report label',
    AnalyzeApplicationWindow_Form_Label_HelpText: 'The name the report will be given',
    AnalyzeApplicationWindow_Analyze_Button: 'Analyze',
    AnalyzeApplicationWindow_Cancel_Button: 'Cancel',
    AnalyzeApplicationWindow_Form_Html: '<p>Application analysis performs a deep inspection of this application, ' +
        'identifying potential risks.  More information is available ' +
        '<a href="http://links.sonatype.com/products/insight/ac/home" target="_blank" rel="noopener" class="x-link">here</a>.</p>',
    AnalyzeApplicationWindow_Loading_Mask: 'Loading',
    AnalyzeApplicationWindow_No_Assets_Error_Title: 'Component has no application assets',
    AnalyzeApplicationWindow_No_Assets_Error_Message: 'This component has no application assets or you do not have read permission for any of its application assets',

    HealthCheckInfo_Most_Popular_Version_Label: 'Most popular version',
    HealthCheckInfo_Age_Label: 'Age',
    HealthCheckInfo_Popularity_Label: 'Popularity',
    HealthCheckInfo_Loading_Text: 'Loading...',
    HealthCheckInfo_Disabled_Tooltip: 'The age and popularity data is only available once Repository Health Check (RHC) has been enabled.',
    HealthCheckInfo_Error_Tooltip: 'Error retrieving component data',
    HealthCheckInfo_Quota_Tooltip: 'The query limit for age and popularity data has been reached. Contact Sonatype support to extend current quota limits.',
    HealthCheckInfo_Unavailable_Tooltip: 'No data available for this component',

    // Vulnerability
    Vulnerability_Information: 'Information',
    Vulnerability_NotScanned: 'Vulnerability information unavailable',
    Vulnerability_Count: 'Vulnerability Count',
    Vulnerability_Ref: 'Vulnerability Details',

    // Browse -> Search
    Search_Text: 'Search',
    Search_Description: 'Search for components by attribute',
    Search_SearchRestrictions: 'Your search must contain at least one criterion, an additional criterion beyond format, at least three characters before a trailing wildcard (*), and it cannot begin with a wildcard (*).',
    Search_KeywordSearchRestrictions: 'Enclose your criteria in quotation marks to search an exact phrase; otherwise, search criteria will be split by any commas, spaces, dashes, or forward slashes.<br>' +
                                      'All keyword searches automatically append a wildcard (*) at the end of each criterion.',
    Search_SaveSearchFilter_Title: 'Save search filter',
    Search_SaveSearchFilter_Name_FieldLabel: 'Filter name',
    Search_SaveSearchFilter_Description_FieldLabel: 'Filter description',
    Search_Results_Limit_Message: 'Only showing the first {0} of {1} results',
    Search_Results_TimedOut_Message: 'Search exceeded timeout of {0}s, please refine your search criteria. {1}',
    Search_Results_TimedOut_LearnMore: 'Learn More',
    SearchCriteria_Keyword_FieldLabel: 'Keyword',
    SearchCriteria_RepositoryName_FieldLabel: 'Repository Name',
    SearchCriteria_Name_FieldLabel: 'Name',
    SearchCriteria_Tag_FieldLabel: 'Tag',
    SearchCriteria_Format_FieldLabel: 'Format',
    SearchCriteria_Group_FieldLabel: 'Group',
    SearchCriteria_Checksum_Group: 'Checksum',
    SearchDocker_Group: 'Docker Repositories',
    SearchMaven_Group: 'Maven Repositories',
    SearchComposer_Group: 'Composer Repositories',
    SearchNpm_Group: 'npm Repositories',
    SearchNuget_Group: 'NuGet Repositories',
    SearchPyPi_Group: 'PyPI Repositories',
    SearchRubygems_Group: 'RubyGems Repositories',
    SearchGitLfs_Group: 'Git LFS Repositories',
    SearchYum_Group: 'Yum Repositories',
    SearchCriteria_MD5_FieldLabel: 'MD5',
    SearchCriteria_SHA1_FieldLabel: 'SHA-1',
    SearchCriteria_SHA256_FieldLabel: 'SHA-256',
    SearchCriteria_SHA2_FieldLabel: 'SHA-512',
    SearchCriteria_Version_FieldLabel: 'Version',
    Search_TextSearchCriteria_Filter_EmptyText: 'Any',
    SearchDocker_Image_Name_FieldLabel: 'Image Name',
    SearchDocker_Image_Tag_FieldLabel: 'Image Tag',
    SearchDocker_LayerId_FieldLabel: 'Layer Id',
    SearchDocker_ContentDigest_FieldLabel: 'Content Digest',
    SearchMaven_ArtifactID_FieldLabel: 'Artifact Id',
    SearchMaven_BaseVersion_FieldLabel: 'Base Version',
    SearchMaven_Extension_FieldLabel: 'Extension',
    SearchMaven_GroupID_FieldLabel: 'Group Id',
    SearchMaven_Classifier_FieldLabel: 'Classifier',
    SearchMaven_Version_FieldLabel: 'Version',
    SearchComposer_Vendor_FieldLabel: 'Vendor',
    SearchComposer_Package_FieldLabel: 'Package',
    SearchComposer_Version_FieldLabel: 'Version',
    SearchNpm_Scope_FieldLabel: 'Scope',
    SearchNpm_Name_FieldLabel: 'Name',
    SearchNpm_Version_FieldLabel: 'Version',
    SearchNpm_Author_FieldLabel: 'Author',
    SearchNpm_Description_FieldLabel: 'Description',
    SearchNpm_Keywords_FieldLabel: 'Keywords',
    SearchNpm_License_FieldLabel: 'License',
    SearchNuget_ID_FieldLabel: 'ID',
    SearchNuget_Tags_FieldLabel: 'Tags',
    SearchPyPi_Classifiers_FieldLabel: 'Classifiers',
    SearchPyPi_Description_FieldLabel: 'Description',
    SearchPyPi_Keywords_FieldLabel: 'PyPI Keywords',
    SearchPyPi_Summary_FieldLabel: 'Summary',
    SearchRubygems_Name_FieldLabel: 'Name',
    SearchRubygems_Version_FieldLabel: 'Version',
    SearchRubygems_Platform_FieldLabel: 'Platform',
    SearchRubygems_Summary_FieldLabel: 'Summary',
    SearchRubygems_Description_FieldLabel: 'Description',
    SearchRubygems_Licenses_FieldLabel: 'Licenses',
    SearchRubygems_Homepage_FieldLabel: 'Homepage',
    SearchYum_Architecture_FieldLabel: 'Architecture',
    SearchYum_Name_FieldLabel: 'Package Name',
    SearchGolang_Group: 'Go Repositories',
    SearchGolang_License_FieldLabel: 'License',
    Search_More_Text: 'More criteria',
    Search_SearchResultList_Format_Header: 'Format',
    Search_SearchResultList_Last_Updated_Header: 'Last Updated',
    Search_SearchResultList_Group_Header: 'Group',
    Search_SearchResultList_Name_Header: 'Name',
    Search_SearchResultList_Repository_Header: 'Repository',
    Search_SearchResultList_Version_Header: 'Version',
    Search_SearchResultList_EmptyText: 'No components matched the filter criteria',
    Search_Assets_Group: 'Group',
    Search_Assets_Name: 'Name',
    Search_Assets_Format: 'Format',
    Search_Assets_Repository: 'Repository',
    Search_Assets_Version: 'Version',
    SearchResultAssetList_Name_Header: 'Name',
    Component_AssetInfo_Info_Title: 'Summary',
    Component_Vulnerability_Info_Title: 'OSS Index Vulnerabilities',
    Component_AssetInfo_Attributes_Title: 'Attributes',
    Component_AssetInfo_HealthCheck_Title: 'Sonatype Lifecycle Component',

    // Browse -> Search -> Docker
    SearchDocker_Text: 'Docker',
    SearchDocker_Description: 'Search for components in Docker repositories',

    // Browse -> Search -> R
    SearchR_Text: 'R',
    SearchR_Description: 'Search for components in R repositories',

    // Browse -> Search -> Raw
    SearchRaw_Text: 'Raw',
    SearchRaw_Description: 'Search for components in Raw repositories',

    // Browse -> Search -> Git LFS
    SearchGitLfs_Text: 'Git LFS',
    SearchGitLfs_Description: 'Search for components in Git LFS repositories',

    // Browse -> Search -> Go
    SearchGolang_Text: 'Go',
    SearchGolang_Description: 'Search for components in Go repositories',

    // Browse -> Search -> Helm
    SearchHelm_Text: 'Helm',
    SearchHelm_Description: 'Search for components in Helm repositories',
    SearchHelm_Group: 'Helm Repositories',
    Repository_Facet_HelmFacet_Title: 'Helm Settings',

    // Browse -> Search -> npm
    SearchNpm_Text: 'npm',
    SearchNpm_Description: 'Search for components in npm repositories',

    // Browse -> Search -> Nuget
    SearchNuget_Text: 'NuGet',
    SearchNuget_Description: 'Search for components in NuGet repositories',

    // Browse -> Search -> PyPI
    SearchPyPi_Text: 'PyPI',
    SearchPyPi_Description: 'Search for components in PyPI repositories',

    // Browse -> Search -> Rubygems
    SearchRubygems_Text: 'RubyGems',
    SearchRubygems_Description: 'Search for components in RubyGems repositories',

    // Browse -> Search -> Custom
    Search_Custom_Text: 'Custom',
    Search_Custom_Description: 'Search for components by custom criteria',

    // Browse -> Search -> Maven
    SearchMaven_Text: 'Maven',
    SearchMaven_Description: 'Search for components by Maven coordinates',

    // Browse -> Search -> Yum
    SearchYum_Text: 'Yum',
    SearchYum_Description: 'Search for components in Yum repositories',

    // Browse -> Search -> Apt
    SearchApt_Text: 'Apt',
    SearchApt_Description: 'Search for components in Apt repositories',

    // Browse -> Search -> Cocoapods
    SearchCocoapods_Text: 'Cocoapods',
    SearchCocoapods_Description: 'Search for components in Cocoapods repositories',

    // Browse -> Search -> p2
    SearchP2_Text: 'P2',
    SearchP2_Description: 'Search for components in P2 repositories',
    SearchP2_Group: 'P2 Repositories',
    SearchP2_PluginName_FieldLabel: 'Plugin name',

    // Browse -> Search -> Conan
    SearchConan_Group: 'Conan Repositories',
    SearchConan_Text: 'Conan',
    SearchConan_Description: 'Search for components in Conan repositories',
    SearchConan_BaseVersion_FieldLabel: 'Base Version',
    SearchConan_Channel_FieldLabel: 'Channel',
    SearchConan_RecipeRevision_FieldLabel: 'Recipe Revision',
    SearchConan_PackageId_FieldLabel: 'Package Id',
    SearchConan_PackageRevision_FieldLabel: 'Package Revision',
    SearchConan_BaseVersionStrict_FieldLabel: 'Base Version Strict',
    SearchConan_RecipeRevisionLatest_FieldLabel: 'Latest revision',
    SearchConan_Arch_FieldLabel: 'Arch',
    SearchConan_Os_FieldLabel: 'Os',
    SearchConan_Compiler_FieldLabel: 'Compiler',
    SearchConan_CompilerVersion_FieldLabel: 'Compiler Version',
    SearchConan_CompilerRuntime_FieldLabel: 'Compiler Runtime',

    // Browse -> Search -> Conda
    SearchConda_Text: 'Conda',
    SearchConda_Description: 'Search for components in Conda repositories',
    SearchConda_Group: 'Conda Repositories',
    SearchConda_License_FieldLabel: 'License',

    // Browse -> Browse
    FeatureGroups_Browse_Text: 'Browse',
    FeatureGroups_Browse_Description: 'Browse assets and components',

    // Browse -> Search -> Cargo
    SearchCargo_Text: 'Cargo',
    SearchCargo_Description: 'Search for components in Cargo repositories',

    // Browse -> Search -> Composer
    SearchComposer_Text: 'Composer',
    SearchComposer_Description: 'Search for components in Composer repositories',

    // Browse -> Search -> HuggingFace
    SearchHuggingFace_Text: 'HuggingFace',
    SearchHuggingFace_Description: 'Search for components in HuggingFace repositories',

    // Browse -> Upload
    FeatureGroups_Upload_Text: 'Upload',
    FeatureGroups_Upload_Description: 'Upload content to the repository',
    FeatureGroups_Upload_Wait_Message: 'Uploading your components...',
    FeatureGroups_Upload_Successful: 'Components uploaded successfully',
    FeatureGroups_Upload_Successful_Link_Text: 'view it now.',
    FeatureGroups_Upload_Successful_Text: 'Component uploaded to the {0} repository',
    FeatureGroups_Upload_Asset_Form_Title: 'Choose assets for this component',
    FeatureGroups_Upload_Asset_Form_File_Label: 'File',
    FeatureGroups_Upload_Asset_Form_Remove_Button: 'Remove',
    FeatureGroups_Upload_Asset_Form_Add_Asset_Button: 'Add another asset',
    FeatureGroups_Upload_Asset_Form_Not_Unique_Error_Message: 'Asset not unique',
    FeatureGroups_Upload_Form_Upload_Button: 'Upload',
    FeatureGroups_Upload_Form_Discard_Button: 'Cancel',
    FeatureGroups_Upload_Form_Browse_Button: 'Browse',
    FeatureGroups_Upload_Form_DetailsFromPom_Mask: 'Component details will be extracted from the provided POM file.',

    // Admin -> Repository
    FeatureGroups_Repository_Text: 'Repository',
    FeatureGroups_Repository_Description: 'Repository administration',

    // Admin -> Repository -> Repositories
    Repositories_Text: 'Repositories',
    Repositories_Description: 'Manage repositories',
    Repositories_Delete_Mask: 'Deleting repository',
    Repositories_Create_Title: 'Create Repository: {0}',
    Repositories_SelectRecipe_Title: 'Select Recipe',
    Repository_RepositoryAdd_Create_Success: 'Repository created: ',
    Repository_RepositoryAdd_Create_Error: 'You do not have permission to create repositories',
    Repository_RepositorySettingsForm_Update_Success: 'Repository updated: ',
    Repository_RepositorySettingsForm_Update_Error: 'You do not have permission to update repositories',
    Repository_RepositoryList_New_Button: 'Create repository',
    Repository_RepositoryList_Name_Header: 'Name',
    Repository_RepositoryList_Size_Header: 'Size',
    Repository_RepositoryList_Type_Header: 'Type',
    Repository_RepositoryList_Format_Header: 'Format',
    Repository_RepositoryList_Status_Header: 'Status',
    Repository_RepositoryList_URL_Header: 'URL',
    Repository_RepositoryList_BlobStore_Header: 'Blob Store',
    Repository_RepositoryList_Filter_EmptyText: 'No repositories matched "$filter"',
    Repository_RepositoryList_EmptyText: '<div class="summary">There are no repositories created yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is a repository?</h3>' +
        '<p>A repository is a storage location where components, such as packages, libraries, binaries, and containers, ' +
        'are retrieved so they can be installed or used. Creating and managing repositories is an essential part of ' +
        'your Nexus Repository Manager configuration since it allows you to expose content to your end users as well ' +
        'as provide a location for them to store more content. For more information, check ' +
        '<a href="http://links.sonatype.com/products/nxrm3/docs/repository" target="_blank" rel="noopener noreferrer">the documentation</a>.</p></div>',
    Repository_RepositoryFeature_Delete_Button: 'Delete repository',
    Repository_RepositoryFeature_RebuildIndex_Button: 'Rebuild index',
    Repository_RepositoryFeature_HealthCheckDisable_Button: 'Disable HealthCheck',
    Repository_RepositoryFeature_HealthCheckEnable_Button: 'Enable HealthCheck',
    Repository_RepositoryFeature_InvalidateCache_Button: 'Invalidate cache',
    Repository_RepositorySettings_Title: 'Settings',
    Repository_Facet_CargoRequire_Authentication_Title: 'Cargo Settings',
    Repository_Facet_CargoRequire_Authentication_Enabled: 'Authentication Requirements:',
    Repository_Facet_CargoRequire_Authentication_Help: 'Restrict repository content to authenticated users',
    Repository_Facet_CargoRequire_Authentication_HelpText: 'Leaving this box unchecked only allows anonymous access to this repository if anonymous access is also enabled in your instance',
    Repository_Facet_DockerHostedFacet_V1_Title: 'Docker Registry API Support',
    Repository_Facet_DockerHostedFacet_V1_Enabled: 'Enable Docker V1 API',
    Repository_Facet_DockerHostedFacet_V1_Enabled_Help: 'Allow clients to use the V1 API to interact with this repository',
    Repository_Facet_DockerConnectorFacet_Title: 'Repository Connectors',
    Repository_Facet_DockerConnectorFacet_Help: '<em>Connectors allow Docker clients to connect directly ' +
        'to hosted registries, but are not always required. Consult our ' +
        '<a href="https://links.sonatype.com/products/nexus/docker-ssl-connector/docs" target="_blank" rel="noopener">documentation</a>' +
        ' for which connector is appropriate for your use case. For information on scaling the repositories' +
        ' see our <a href="https://links.sonatype.com/products/nexus/docker-scaling-repositories/docs" target="_blank" rel="noopener">scaling documentation</a>.</em>',
    Repository_Facet_Docker_Subdomain_FieldLabel: 'Allow Subdomain Routing',
    Repository_Facet_Docker_Subdomain_HelpText: 'Use the following subdomain to make push and pull requests for this repository.',
    Repository_Facet_DockerConnectorFacet_HttpPort_FieldLabel: 'HTTP',
    Repository_Facet_DockerConnectorFacet_HttpPort_HelpText: 'Create an HTTP connector at specified port. Normally used if the server is behind a secure proxy.',
    Repository_Facet_DockerConnectorFacet_HttpsPort_FieldLabel: 'HTTPS',
    Repository_Facet_DockerConnectorFacet_HttpsPort_HelpText: 'Create an HTTPS connector at specified port. Normally used if the server is configured for https.',
    Repository_Facet_DockerProxyFacet_IndexType_FieldLabel: 'Docker Index',
    Repository_Facet_DockerProxyFacet_IndexTypeRegistry_BoxLabel: 'Use proxy registry (specified above)',
    Repository_Facet_DockerProxyFacet_IndexTypeHub_BoxLabel: 'Use Docker Hub',
    Repository_Facet_DockerProxyFacet_IndexTypeCustom_BoxLabel: 'Custom index',
    Repository_Facet_DockerProxyFacet_IndexUrl_HelpText: 'Location of Docker index',
    Repository_Facet_DockerProxyFacet_ForeignLayers_FieldLabel: 'Foreign Layer Caching',
    Repository_Facet_DockerProxyFacet_ForeignLayers_HelpText: 'Allow Nexus Repository Manager to download and cache foreign layers',
    Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_FieldLabel: 'Foreign Layer Allowed URLs',
    Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_HelpText: 'Regular expressions used to identify URLs that are allowed for foreign layer requests',
    Repository_Facet_DockerProxyFacet_ForeignLayersWhitelist_AddButton: 'Add URL Pattern',
    Repository_Facet_DockerProxyFacet_BasicAuth_FieldLabel: 'Allow anonymous docker pull',
    Repository_Facet_DockerProxyFacet_BasicAuth_BoxLabel: 'Allow anonymous docker pull ( Docker Bearer Token Realm required )',
    Repository_Facet_YumHostedFacet_Title: 'Yum',
    Repository_Facet_YumHostedFacet_RepodataDepth_FieldLabel: 'Repodata Depth',
    Repository_Facet_YumHostedFacet_RepodataDepth_HelpText: 'Specifies the repository depth where repodata folder(s) are created',
    Repository_Facet_YumHostedFacet_DeployPolicy_FieldLabel: 'Layout Policy',
    Repository_Facet_YumHostedFacet_DeployPolicy_HelpText: 'Validate that all paths are RPMs or yum metadata',
    Repository_Facet_YumHostedFacet_DeployPolicy_EmptyText: 'Select a policy',
    Repository_Facet_YumHostedFacet_DeployPolicy_StrictItem: 'Strict',
    Repository_Facet_YumHostedFacet_DeployPolicy_PermissiveItem: 'Permissive',
    Repository_Facet_AptFacet_Title: 'APT Settings',
    Repository_Facet_AptFacet_Distribution_FieldLabel: 'Distribution',
    Repository_Facet_AptFacet_Distribution_HelpText: 'Distribution to fetch e.g. bionic',
    Repository_Facet_AptFacet_Flat_FieldLabel: 'Flat',
    Repository_Facet_AptFacet_Flat_HelpText: 'Is this repository flat?',
    Repository_Facet_AptSigningFacet_Keypair_FieldLabel: 'Signing Key',
    Repository_Facet_AptSigningFacet_Keypair_HelpText: 'PGP signing key pair (armored private key e.g. gpg --export-secret-key --armor <Name or ID>)',
    Repository_Facet_AptSigningFacet_Passphrase_FieldLabel: 'Passphrase',
    Repository_Facet_CondaFacet_Title: 'Conda Settings',
    Repository_Facet_ConanProxyFacet_Title: 'Conan',
    Repository_Facet_ConanProxyFacet_ProtocolVersion: 'Protocol version',
    Repository_Facet_ConanProxyFacet_Version: 'Version',
    Repository_Facet_ConanProxyFacet_V1: 'Conan V1',
    Repository_Facet_ConanProxyFacet_V2: 'Conan V2',
    Repository_Facet_ConanProxyFacet_HelpText: 'Automatic migration from Conan 1 to Conan 2 is not available.',
    Repository_Facet_ConanProxyFacet_HelpLink: 'Read our <a href="https://links.sonatype.com/products/nxrm3/docs/conan-v2" target="_blank" style="font-weight: bold">documentation</a> for more details.',
    Repository_Facet_ConanGroupFacet_Title: 'Conan',

    Repository_Facet_GroupFacet_Title: 'Group',
    Repository_Facet_NugetGroupFacet_NugetGroupValidationLabel: '<span style="color: red; ">Group repositories cannot include a mix of NuGet v2 and v3 members. You cannot add <b>{0}</b> ({1}) because the group contains <b>{2}</b> ({3}).</span>',
    Repository_Facet_HttpClientFacet_Title: 'HTTP',
    Repository_Facet_Maven2Facet_Title: 'Maven 2',
    Repository_Facet_NegativeCacheFacet_Title: 'Negative Cache',
    Repository_Facet_NugetProxyFacet_Title: 'NuGet',
    Repository_Facet_ProxyFacet_Title: 'Proxy',
    Repository_Facet_Raw_Title: 'Raw',
    Repository_Facet_Raw_ContentDisposition_FieldLabel: 'Content Disposition',
    Repository_Facet_Raw_ContentDisposition_HelpText: 'Add Content-Disposition header as \'Attachment\' to disable some content from being inline in a browser.',
    Repository_Facet_Raw_ContentDisposition_Inline: 'Inline',
    Repository_Facet_Raw_ContentDisposition_Attachment: 'Attachment',
    Repository_Facet_YumSigningFacet_Title: 'Yum Settings',
    Repository_Facet_YumSigningFacet_Hint: '<em style="font-size: 12px">Verifying of Yum repodata files can use GPG keys. ' +
        'Read our <a href="http://links.sonatype.com/products/nxrm3/docs/gpg-signatures-for-yum-proxy-group" target="_blank">documentation</a>' +
        ' for more details.</em>',
    Repository_Facet_YumSigningFacet_GPG_Keypair_FieldLabel: 'Signing Key',
    Repository_Facet_YumSigningFacet_GPG_Keypair_HelpText: 'PGP signing key pair (armored private key e.g. gpg --export-secret-key --armor <Name or ID>)',
    Repository_Facet_YumSigningFacet_GPG_Passphrase_FieldLabel: 'Passphrase',
    Repository_Facet_StorageFacet_Title: 'Storage',
    Repository_Facet_StorageFacetHosted_Title: 'Hosted',
    Repository_Facet_RoutingRuleFacet_Title: 'Routing Rule',
    Repository_Facet_RoutingRuleFacet_HelpText: 'Choose a rule to restrict some requests from being served by this repository',
    Repository_Facet_ProxyFacet_Autoblock_FieldLabel: 'Auto blocking enabled',
    Repository_Facet_ProxyFacet_Autoblock_HelpText: 'Auto-block outbound connections on the repository if remote peer is detected as unreachable/unresponsive',
    Repository_Facet_ProxyFacet_Blocked_FieldLabel: 'Blocked',
    Repository_Facet_ProxyFacet_Blocked_HelpText: 'Block outbound connections on the repository',
    Repository_RepositorySettingsForm_Name_FieldLabel: 'Name',
    Repository_RepositorySettingsForm_Name_HelpText: 'A unique identifier for this repository',
    Repository_RepositorySettingsForm_URL_FieldLabel: 'URL',
    Repository_RepositorySettingsForm_URL_HelpText: 'The URL used to access this repository',
    Repository_Facet_GroupFacet_Members_FieldLabel: 'Member repositories',
    Repository_Facet_GroupFacet_Members_HelpText: 'Select and order the repositories that are part of this group',
    Repository_Facet_GroupFacet_Members_FromTitle: 'Available',
    Repository_Facet_GroupFacet_Members_ToTitle: 'Members',
    Repository_Facet_GroupWriteFacet_Writable_Repository_FieldLabel: 'Writable Repository',
    Repository_Facet_GroupWriteFacet_Writable_Repository_HelpText: 'The member repository that POST and PUT requests will be routed to',
    Repository_Facet_StorageFacetHosted_Deployment_FieldLabel: 'Deployment policy',
    Repository_Facet_StorageFacetHosted_Deployment_HelpText: 'Controls if deployments of and updates to artifacts are allowed',
    Repository_Facet_StorageFacetHosted_Deployment_EmptyText: 'Select a policy',
    Repository_Facet_StorageFacetHosted_Deployment_AllowItem: 'Allow redeploy',
    Repository_Facet_StorageFacetHosted_Deployment_DisableItem: 'Disable redeploy',
    Repository_Facet_StorageFacetHosted_Deployment_DisableLatestItem: 'Allow redeploy only on \'latest\' tag',
    Repository_Facet_StorageFacetHosted_Deployment_DisableLatestItemHelpText: 'Allow redeploying the \'latest\' tag but defer to the Deployment Policy for all other tags',
    Repository_Facet_StorageFacetHosted_Deployment_ReadOnlyItem: 'Read-only',
    Repository_Facet_StorageFacetHosted_Proprietary_Components_HelpText: 'Components in this repository count as proprietary for namespace conflict attacks (requires Sonatype Nexus Firewall)',
    Repository_Facet_StorageFacetHosted_Proprietary_Components_FieldLabel: 'Proprietary Components',
    Repository_Facet_ProxyFacet_Remote_FieldLabel: 'Remote storage',
    Repository_Facet_ProxyFacet_Remote_HelpText: 'Location of the remote repository being proxied',
    Repository_Facet_ProxyFacet_Remote_EmptyText: 'Enter a URL',
    Repository_Facet_ProxyFacet_PreemptivePull_FieldLabel: 'Pre-emptive Pull',
    Repository_Facet_ProxyFacet_PreemptivePull_HelpText: 'If enabled, the remote storage will be monitored for changes, and new components will be replicated automatically, and cached locally',
    Repository_Facet_ProxyFacet_AssetNameMatcher_FieldLabel: 'Asset Name Matcher',
    Repository_Facet_ProxyFacet_AssetNameMatcher_HelpText: 'This field allows you to use a RegEx to match search for specific components to help define scope.\n' +
        'For more information check out our <a target="_blank" href="https://links.sonatype.com/products/nxrm3/docs/pull-replication/asset-name-matcher">documentation for format specific options</a>',
    Repository_Facet_ProxyFacet_AssetNameMatcher_EmptyText: 'Entry',
    Repository_Facet_ProxyFacet_AssetNameMatcher_InvalidText: 'Invalid Regex',
    Repository_Facet_ProxyFacet_Docker_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://registry-1.docker.io',
    Repository_Facet_ProxyFacet_Huggingface_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://huggingface.co/',
    Repository_Facet_ProxyFacet_Maven_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://repo1.maven.org/maven2/',
    Repository_Facet_ProxyFacet_Npm_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://registry.npmjs.org',
    Repository_Facet_ProxyFacet_Nuget_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://api.nuget.org/v3/index.json',
    Repository_Facet_ProxyFacet_Pypi_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://pypi.org',
    Repository_Facet_ProxyFacet_Rubygems_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://rubygems.org',
    Repository_Facet_ProxyFacet_Yum_Remote_HelpText: 'Location of the remote repository being proxied, e.g.  http://mirror.centos.org/centos/',
    Repository_Facet_CargoFacet_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://index.crates.io',
    Repository_Facet_ComposerFacet_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://packagist.org',
    Repository_Facet_ProxyFacet_Conan_Remote_HelpText: 'Location of the remote repository being proxied, e.g. https://center.conan.io',
    Ssl_SslUseTrustStore_BoxLabel: 'Use the Nexus Repository truststore',
    Ssl_SslUseTrustStore_Certificate_Button: 'View certificate',
    Ssl_SslUseTrustStore_Certificate_HelpText: 'Use certificates stored in the Nexus Repository truststore to connect to external systems',
    Maven2Facet_VersionPolicy_FieldLabel: 'Version policy',
    Maven2Facet_VersionPolicy_HelpText: 'What type of artifacts does this repository store?',
    Maven2Facet_VersionPolicy_EmptyText: 'Select a policy',
    Maven2Facet_VersionPolicy_MixedItem: 'Mixed',
    Maven2Facet_VersionPolicy_ReleaseItem: 'Release',
    Maven2Facet_VersionPolicy_SnapshotItem: 'Snapshot',
    Repository_Facet_Maven2Facet_ContentDisposition_FieldLabel: 'Content Disposition',
    Repository_Facet_Maven2Facet_ContentDisposition_HelpText: 'Add Content-Disposition header as \'Attachment\' to disable some content from being inline in a browser.',
    Repository_Facet_Maven2Facet_ContentDisposition_Inline: 'Inline',
    Repository_Facet_Maven2Facet_ContentDisposition_Attachment: 'Attachment',
    Repository_Facet_Maven2Facet_LayoutPolicy_FieldLabel: 'Layout policy',
    Repository_Facet_Maven2Facet_LayoutPolicy_HelpText: 'Validate that all paths are maven artifact or metadata paths',
    Repository_Facet_Maven2Facet_LayoutPolicy_EmptyText: 'Select a policy',
    Repository_Facet_Maven2Facet_LayoutPolicy_StrictItem: 'Strict',
    Repository_Facet_Maven2Facet_LayoutPolicy_PermissiveItem: 'Permissive',
    Repository_RepositorySettingsForm_Format_FieldLabel: 'Format',
    Repository_RepositorySettingsForm_Format_HelpText: 'The format of the repository (i.e. maven2, docker, raw, nuget...)',
    Repository_RepositorySettingsForm_Type_FieldLabel: 'Type',
    Repository_RepositorySettingsForm_Type_HelpText: 'The type of repository (i.e. group, hosted, or proxy)',
    Repository_RepositorySettingsForm_Online_FieldLabel: 'Online',
    Repository_RepositorySettingsForm_Online_HelpText: 'If checked, the repository accepts incoming requests',
    Repository_Facet_ProxyFacet_ArtifactAge_FieldLabel: 'Maximum component age',
    Repository_Facet_ProxyFacet_MetadataAge_FieldLabel: 'Maximum metadata age',
    Repository_Facet_ProxyFacet_ArtifactAge_HelpText: 'How long (in minutes) to cache artifacts before rechecking the remote repository. Release repositories should use -1.',
    Repository_Facet_ProxyFacet_MetadataAge_HelpText: 'How long (in minutes) to cache metadata before rechecking the remote repository.',
    Repository_Facet_HttpClientFacet_ConnectionRetries_FieldLabel: 'Connection retries',
    Repository_Facet_HttpClientFacet_ConnectionRetries_HelpText: 'Total retries if the initial connection attempt suffers a timeout',
    Repository_Facet_HttpClientFacet_ConnectionTimeout_FieldLabel: 'Connection timeout',
    Repository_Facet_HttpClientFacet_ConnectionTimeout_HelpText: 'Seconds to wait for activity before stopping and retrying the connection. Leave blank to use the globally defined HTTP timeout.',
    Repository_Facet_HttpClientFacet_EnableCircularRedirects_FieldLabel: 'Enable circular redirects',
    Repository_Facet_HttpClientFacet_EnableCircularRedirects_HelpText: 'Enable redirects to the same location (may be required by some servers)',
    Repository_Facet_HttpClientFacet_EnableCookies_FieldLabel: 'Enable cookies',
    Repository_Facet_HttpClientFacet_EnableCookies_HelpText: 'Allow cookies to be stored and used',
    Repository_Facet_StorageFacet_BlobStore_FieldLabel: 'Blob store',
    Repository_Facet_StorageFacet_BlobStore_HelpText: 'Blob store used to store repository contents',
    Repository_Facet_StorageFacet_BlobStore_EmptyText: 'Select a blob store',
    Repository_Facet_StorageFacet_ContentTypeValidation_FieldLabel: 'Strict Content Type Validation',
    Repository_Facet_StorageFacet_ContentTypeValidation_HelpText: 'Validate that all content uploaded to this repository is of a MIME type appropriate for the repository format',
    Repository_Facet_StorageFacet_DataStore_FieldLabel: 'Data store',
    Repository_Facet_StorageFacet_DataStore_HelpText: 'Data store used to store content metadata',
    Repository_Facet_StorageFacet_DataStore_EmptyText: 'Select a data store',
    Repository_Facet_NegativeCacheFacet_Enabled_FieldLabel: 'Not found cache enabled',
    Repository_Facet_NegativeCacheFacet_Enabled_HelpText: 'Cache responses for content not present in the proxied repository',
    Repository_Facet_NegativeCacheFacet_TTL_FieldLabel: 'Not found cache TTL',
    Repository_Facet_NegativeCacheFacet_TTL_HelpText: 'How long to cache the fact that a file was not found in the repository (in minutes)',
    Repository_Facet_NugetProxyFacet_ProtocolVersion: 'Protocol version',
    Repository_Facet_NugetProxyFacet_V2: 'NuGet V2',
    Repository_Facet_NugetProxyFacet_V3: 'NuGet V3',
    Repository_Facet_NugetProxyFacet_ItemMaxAge_FieldLabel: 'Metadata query cache age',
    Repository_Facet_NugetProxyFacet_ItemMaxAge_HelpText: 'How long to cache query results from the proxied repository (in seconds)',
    Repository_Facet_Npm_Title: 'npm',
    Repository_Facet_Npm_RemoveQuarantined_Label: 'Download policy compliant versions only',
    Repository_Facet_Npm_RemoveQuarantined_HelpText: 'Versions that are going to be quarantined will not be downloaded. <span style="font-weight: bold">Firewall Audit and Quarantine</span> capability must be enabled for this feature to take effect. <a target="_blank" href="http://links.sonatype.com/products/nxrm3/docs/npm-with-firewall">Learn more.</a>',
    Repository_Facet_Npm_RemoveQuarantined_Warning: 'This feature requires IQ Server Release 134 or higher.',
    Repository_Facet_Npm_RemoveQuarantined_Warning_Default: 'To use this feature, enable the Firewall Audit and Quarantine capability with the Enable Quarantine checkbox selected. This feature requires IQ Server Release 134 or higher.',
    Repository_Facet_Pypi_Title: 'PyPI',
    Repository_Facet_Pypi_RemoveQuarantined_Label: 'Download policy compliant versions only',
    Repository_Facet_Pypi_RemoveQuarantined_HelpText: 'Versions that are going to be quarantined will not be downloaded. <span style="font-weight: bold">Firewall Audit and Quarantine</span> capability must be enabled for this feature to take effect. <a target="_blank" href="http://links.sonatype.com/products/nxrm3/docs/pccs/pypi">Learn more.</a>',
    Repository_Facet_Pypi_RemoveQuarantined_Warning: 'This feature requires IQ Server Release 167 or higher.',
    Repository_Facet_Pypi_RemoveQuarantined_Warning_Default: 'To use this feature, enable the Firewall Audit and Quarantine capability with the Enable Quarantine checkbox selected. This feature requires IQ Server Release 167 or higher.',
    Repository_Facet_HttpClientFacet_AuthenticationType_FieldLabel: 'Authentication type',
    Repository_Facet_HttpClientFacet_AuthenticationType_Username: 'Username',
    Repository_Facet_HttpClientFacet_AuthenticationType_NTLM: 'Windows NTLM',
    Repository_Facet_HttpClientFacet_AuthenticationType_Bearer_Token: 'Preemptive Bearer Token',
    Repository_Facet_HttpClientFacet_Authentication_Title: 'Authentication',
    Repository_Facet_HttpClientFacet_HTTP_Title: 'HTTP request settings',
    Repository_Facet_CleanupPolicyFacet_Title: 'Cleanup',
    Repository_Facet_CleanupPolicyFacet_Policy_FieldLabel: 'Cleanup Policies',
    Repository_Facet_CleanupPolicyFacet_Policy_HelpText: 'Components that match any of the Applied policies will be deleted',
    Repository_Facet_CleanupPolicyFacet_Policy_FromTitle: 'Available',
    Repository_Facet_CleanupPolicyFacet_Policy_ToTitle: 'Applied',
    Repository_Facet_CleanupPolicyFacet_Policy_EmptyText: 'None',
    Repository_Formats_All: '(All Formats)',
    Repository_Facet_GolangFacet_Title: 'Go Settings',
    Repository_Facet_CargoFacet_Title: 'Cargo Settings',
    Repository_Facet_ComposerFacet_Title: 'Composer Settings',
    Repository_Replication_InformationMessage: 'This repository is using the replication {0} to connect to source repository {1}.',
    Repository_Copy_URL: 'Use your repository\'s direct URL (shown below) to connect other tools to your repository. ' +
      'For more information, see our ' +
      '<a href="http://links.sonatype.com/products/nxrm3/docs/{0}" target="_blank">{1}-specific help documentation.</a>',
    HealthCheckRepositoryColumn_Header: 'Health check',
    HealthCheckRepositoryColumn_Analyzing: 'Analyzing&hellip;',
    HealthCheckRepositoryColumn_Analyzing_Tooltip: '<span><h2>The Analysis is Under Way</h2>' +
        'The contents of your repository are being analyzed. This process should only take a few minutes.<br><br>' +
        'When the analysis is complete and this page has been refreshed, we will show you the top 5 most vulnerable ' +
        'components in the repository, the number of downloads over the last month, and a year-over-year overview.</span>',
    HealthCheckRepositoryColumn_View_Permission_Error: '<span><h2>Insufficient Permissions to View Summary Report</h2>' +
        'To view healthcheck summary report for a repository your user account must have the necessary permissions.</span>',
    HealthCheckRepositoryColumn_Analyze: 'Analyze',
    HealthCheckRepositoryColumn_Analyze_Tooltip: '<span><h2>Repository Health Check Analysis</h2>Click this button to request a Repository Health Check (RHC) ' +
        'by IQ Server.  The process is non-invasive and non-disruptive. IQ Server ' +
        'will return actionable quality and security information about the open source components in the repository.' +
        '<br><br><a href="http://links.sonatype.com/products/clm/rhc/home" rel="noopener" ' +
        'target="_blank">How the IQ Server Repository Health Check can help you make better software faster</a></span>',
    HealthCheckRepositoryColumn_Analyze_Dialog_Title: 'Analyze Repository',
    HealthCheckRepositoryColumn_Analyze_Dialog_Msg: 'Do you want to analyze the repository {0} and others for security vulnerabilities and license issues?',
    HealthCheckRepositoryColumn_Analyze_Dialog_Ok_Text: 'Yes, all repositories',
    HealthCheckRepositoryColumn_Analyze_Dialog_Yes_Text: 'Yes, only this repository',
    HealthCheckRepositoryColumn_Analyze_Permission_Error: '<span><h2>Insufficient Permissions to Analyze a Repository</h2>' +
        'To analyze a repository your user account must have permissions to start analysis.</span>',
    HealthCheckRepositoryColumn_Loading: 'Loading&hellip;',
    HealthCheckRepositoryColumn_Unavailable_Tooltip: '<span><h2>Repository Health Check Unavailable</h2>A Repository Health Check (RHC) ' +
        'cannot be performed on this repository, because it is an unsupported type or out of service.<br><br>' +
        '<a href="http://links.sonatype.com/products/clm/rhc/home" rel="noopener" ' +
        'target="_blank">How the IQ Server Repository Health Check can help you make better software faster</a></span>',

    HealthCheckSummary_Help: '<a href="http://links.sonatype.com/products/nexus/rhc/manual-remediation-with-rhc" target="_blank"' +
        ' rel="noopener">What should I do with this report?</a>',

    // Admin -> Repository -> Blob Stores
    Blobstores_Text: 'Blob Stores',
    Blobstores_Description: 'Manage blob stores',
    Blobstores_Delete_Mask: 'Deleting blob store',
    Blobstores_Update_Mask: 'Updating blob store',
    Blobstores_Create_Title: 'Create blob store',
    Blobstores_Update_Success: 'Blob store updated: {0}',
    Blobstore_BlobstoreAdd_Create_Success: 'Blob store created: ',
    Blobstore_BlobstoreAdd_Create_Error: 'You do not have permission to create blob stores',
    Blobstore_BlobstoreSettingsForm_Update_Success: 'Blob store updated: ',
    Blobstore_BlobstoreSettingsForm_Update_Error: 'Update is not supported for blob stores',
    Blobstore_BlobstoreList_New_Button: 'Create blob store',
    Blobstore_BlobstoreList_Name_Header: 'Name',
    Blobstore_BlobstoreList_Type_Header: 'Type',
    Blobstore_BlobstoreList_State_Header: 'State',
    Blobstore_BlobstoreList_BlobCount_Header: 'Blob count',
    Blobstore_BlobstoreList_TotalSize_Header: 'Total size',
    Blobstore_BlobstoreList_AvailableSpace_Header: 'Available space',
    Blobstore_BlobstoreList_Filter_EmptyText: 'No blob stores matched "$filter"',
    Blobstore_BlobstoreList_EmptyText: '<div class="summary">There are no blob stores created yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is a blob store?</h3>' +
        '<p>The binary assets you download via proxy repositories, or publish to hosted repositories, are stored in ' +
        'the blob store attached to those repositories. In traditional, single node NXRM deployments, blob stores ' +
        'are typically associated with a local filesystem directory, usually within the sonatype-work directory. ' +
        'For more information, check <a href="http://links.sonatype.com/products/nxrm3/docs/blob-store" ' +
        'target="_blank" rel="noopener noreferrer">the documentation</a>.</p></div>',
    Blobstore_BlobstoreList_Failed: 'Failed',
    Blobstore_BlobstoreList_Started: 'Started',
    Blobstore_BlobstoreList_Unlimited: 'Unlimited',
    Blobstore_BlobstoreList_Unavailable: 'Unavailable',
    Blobstore_BlobstoreFeature_Delete_Button: 'Delete blob store',
    Blobstore_BlobstoreFeature_Delete_Disabled_Message: 'This blob store is in use by: {0}, {1}, {2} and cannot be deleted',
    Blobstore_BlobstoreFeature_Editing_Enabled_Message: 'Updating blob store configuration will cause it to be temporarily unavailable for a short period. Edits to configuration may also leave the blob store in a non-functional state. Use caution when changing values.',
    Blobstore_BlobstoreFeature_Promote_Button: 'Promote to group',
    Blobstore_BlobstoreFeature_Confirm_Title: 'Create Blob Store Group?',
    Blobstore_BlobstoreFeature_Confirm_Warning: 'Warning: This operation cannot be undone',
    Blobstore_BlobstoreFeature_Promote_Success: 'Blob store: {0} promoted to blob store group',
    Blobstore_BlobstoreFeature_Update_Title: 'Update Blob Store?',
    Blobstore_BlobstoreFeature_Update_Warning: 'Warning: The blob store will be temporarily unavailable for a short period.  This function does not migrate data to a new location. Previously created data will not be available',
    Blobstore_BlobstoreSettings_Title: 'Settings',
    Blobstore_BlobstoreAdd_Type_FieldLabel: 'Type',
    Blobstore_BlobstoreAdd_Type_EmptyText: 'Select a type',
    Blobstore_BlobstoreSettingsForm_Name_FieldLabel: 'Name',
    Blobstore_BlobstoreSettingsForm_State_FieldLabel: 'State:',
    Blobstore_BlobstoreSettingsForm_Path_FieldLabel: 'Path',
    Blobstore_BlobstoreSettingsForm_EnableSoftQuota_FieldLabel: 'Enable Soft Quota',
    Blobstore_BlobstoreSettingsForm_SoftQuota_HelpText: 'A soft quota provides warnings when a limit is violated.  It never causes an operation to be rejected',
    Blobstore_BlobstoreSettingsForm_QuotaType_FieldLabel: 'Type of Quota',
    Blobstore_BlobstoreSettingsForm_QuotaLimit_FieldLabel: 'Quota Limit in MB',
    Blobstore_BlobstoreSettingsForm_Test_Connection_Button: 'Test Connection',
    Blobstore_BlobstoreSettingsForm_Test_Success_Message: 'Connection successful',

    // Admin -> Repository -> Selectors
    Selectors_Text: 'Content Selectors',
    Selectors_Description: 'Manage content selectors',
    Selectors_Create_Title: 'Create Selector',
    Selector_SelectorAdd_Create_Error: 'You do not have permission to create selectors',
    Selector_SelectorAdd_Create_Success: 'Selector created: {0}',
    Selector_SelectorSettingsForm_Update_Error: 'You do not have permission to update selectors',
    Selector_SelectorSettingsForm_Update_Success: 'Selector updated: {0}',
    Selector_SelectorList_New_Button: 'Create selector',
    Selector_SelectorList_Name_Header: 'Name',
    Selector_SelectorList_Type_Header: 'Type',
    Selector_SelectorList_Description_Header: 'Description',
    Selector_SelectorList_EmptyText: '<div class="summary">There are no content selectors created yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is a content selector?</h3>' +
        '<p>Content selectors provide a means for you to select specific content from all of your content. The ' +
        'content you select is evaluated against expressions written in CSEL (Content Selector Expression Language). ' +
        'For more information, check <a href="http://links.sonatype.com/products/nxrm3/docs/content-selector" ' +
        'target="_blank" rel="noopener noreferrer">the documentation</a>.</p></div>',
    Selector_SelectorList_Filter_EmptyText: 'No selectors matched "$filter"',
    Selector_SelectorFeature_Delete_Button: 'Delete selector',
    Selector_SelectorFeature_Delete_Disabled_Message: 'This selector cannot be deleted because it is in use by {0}',
    Selectors_Delete_Message: 'Selector deleted: {0}',
    Selector_SelectorFeature_Settings_Title: 'Settings',
    Selector_SelectorSettingsForm_Name_FieldLabel: 'Name',
    Selector_SelectorSettingsForm_Type_FieldLabel: 'Type',
    Selector_SelectorSettingsForm_Type_Jexl: 'JEXL',
    Selector_SelectorSettingsForm_Type_Sonatype: 'CSEL',
    Selector_SelectorSettingsForm_Description_FieldLabel: 'Description',
    Selector_SelectorSettingsForm_Expression_FieldLabel: 'Search expression',
    Selector_SelectorSettingsForm_Expression_HelpText: 'Use query to identify repositories, components or assets',
    Selector_SelectorSettingsForm_Expression_Examples: '<div style="font-size: 11px"><br/>' +
        '<h4>Example Content Selector Expressions:</h4>' +
        '<p>Select all "raw" format content<br/><i>format == "raw"</i></p>' +
        '<p>Select all "maven2" content along a path that starts with "/org/sonatype/nexus"<br/><i>format == "maven2" and path =^ "/org/sonatype/nexus"</i></p>' +
        '<br/>' +
        '<p>See the <a href="http://links.sonatype.com/products/nexus/selectors/docs" target="_blank" rel="noopener">documentation</a> for more details</p>' +
        '</div>',
    Selector_SelectorSettingsForm_Expression_Examples_jexl: '<div style="font-size: 11px"><br/>' +
        '<h4>Example <a href="http://links.sonatype.com/products/nexus/jexl" target="_blank" rel="noopener">JEXL</a> queries:</h4>' +
        '<p>Select all "raw" format content<br/><i>format == "raw"</i></p>' +
        '<p>Select all "maven2" content along a path that starts with "/org/sonatype/nexus"<br/><i>format == "maven2" and path =^ "/org/sonatype/nexus"</i></p>' +
        '<br/>' +
        '<p>See the <a href="http://links.sonatype.com/products/nexus/selectors/docs" target="_blank" rel="noopener">documentation</a> for more details</p>' +
        '</div>',
    Selector_SelectorSettingsForm_SelectorID_Title: 'Selector ID',
    Selector_SelectorSettingsForm_Specification_Title: 'Specification',
    Selector_SelectorSettingsForm_Preview_Button: 'Preview results',

    // Admin -> Repository -> Selectors -> Preview Window
    SelectorPreviewWindow_Title: 'Preview results',
    SelectorPreviewWindow_expression_FieldLabel: 'Expression',
    SelectorPreviewWindow_expression_jexl: 'JEXL',
    SelectorPreviewWindow_expression_csel: 'CSEL',
    SelectorPreviewWindow_type_FieldLabel: 'Type',
    SelectorPreviewWindow_repository_FieldLabel: 'Preview Repository',
    SelectorPreviewWindow_repository_HelpText: 'Select a repository to evaluate the content selector and see the content that would be available.',
    SelectorPreviewWindow_repository_EmptyText: 'Select a repository...',
    SelectorPreviewWindow_EmptyText_View: 'No assets in repository matched the expression',
    SelectorPreviewWindow_EmptyText_Filter: 'No assets matched "$filter"',
    SelectorPreviewWindow_Name_Column: 'Name',
    SelectorPreviewWindow_Preview_Button: 'Preview',

    // Admin -> Security
    FeatureGroups_Security_Title: 'Security',
    FeatureGroups_Security_Description: 'Security administration',

    // Admin -> Security -> Privileges
    Privileges_Text: 'Privileges',
    Privileges_Description: 'Manage privileges',
    Privileges_Update_Mask: 'Updating privilege',
    Privileges_Update_Success: 'Privilege updated: {0}',
    Privileges_Create_Success: 'Privilege created: {0}',
    Privileges_Delete_Success: 'Privilege deleted: {0}',
    Privileges_Select_Title: 'Select Privilege Type',
    Privilege_PrivilegeList_New_Button: 'Create privilege',
    Privilege_PrivilegeList_Name_Header: 'Name',
    Privilege_PrivilegeList_Description_Header: 'Description',
    Privilege_PrivilegeList_Type_Header: 'Type',
    Privilege_PrivilegeList_Permission_Header: 'Permission',
    Privilege_PrivilegeList_EmptyText: 'No privileges defined',
    Privilege_PrivilegeList_Filter_EmptyText: 'No privileges matched "$filter"',
    Privilege_PrivilegeFeature_Details_Tab: 'Summary',
    Privilege_PrivilegeFeature_Delete_Button: 'Delete privilege',
    Privilege_PrivilegeFeature_Settings_Title: 'Settings',
    Privilege_PrivilegeSelectType_Type_Header: 'Type',
    Privilege_PrivilegeAdd_Create_Error: 'You do not have permission to create privileges',
    Privilege_PrivilegeSettingsForm_Update_Success: 'Privilege updated: {0}',
    Privilege_PrivilegeSettingsForm_Update_Error: 'You do not have permission to update privileges or privilege is read only',
    Privilege_PrivilegeSettingsForm_Description_FieldLabel: 'Description',
    Privilege_PrivilegeSettingsForm_Name_FieldLabel: 'Name',
    Privileges_Summary_ID: 'ID',
    Privileges_Summary_Type: 'Type',
    Privileges_Summary_Name: 'Name',
    Privileges_Summary_Description: 'Description',
    Privileges_Summary_Permission: 'Permission',
    Privileges_Summary_Property: 'Property-{0}',
    Privileges_Create_Title: 'Create {0} Privilege',

    // Admin -> Security -> Roles
    Roles_Text: 'Roles',
    Roles_Description: 'Manage roles',
    Roles_Create_Title: 'Create Role',
    Role_RoleAdd_Create_Error: 'You do not have permission to create roles',
    Role_RoleAdd_Create_Success: 'Role created: ',
    Role_RoleSettingsForm_Update_Error: 'You do not have permission to update roles or role is readonly',
    Role_RoleSettingsForm_Update_Success: 'Role updated: ',
    Role_RoleList_New_Button: 'Create role',
    Role_RoleList_New_NexusRoleItem: 'Nexus role',
    Roles_New_ExternalRoleItem: 'External role mapping',
    Role_RoleList_Name_Header: 'Name',
    Role_RoleList_Source_Header: 'Source',
    Role_RoleList_Description_Header: 'Description',
    Role_RoleList_EmptyText: 'No roles defined',
    Role_RoleList_Filter_EmptyText: 'No roles matched "$filter"',
    Role_RoleFeature_Delete_Button: 'Delete role',
    Roles_Delete_Message: 'Role deleted: {0}',
    Role_RoleFeature_Settings_Title: 'Settings',
    Role_RoleSettingsForm_RoleID_FieldLabel: 'Role ID',
    Role_RoleSettingsForm_MappedRole_FieldLabel: 'Mapped Role',
    Role_RoleSettingsForm_MappedRole_EmptyText: 'Select a role',
    Role_RoleSettingsForm_Name_FieldLabel: 'Role name',
    Role_RoleSettingsForm_Description_FieldLabel: 'Role description',
    Role_RoleSettingsForm_Privileges_FieldLabel: 'Privileges',
    Role_RoleSettingsForm_Privileges_FromTitle: 'Available',
    Role_RoleSettingsForm_Privileges_ToTitle: 'Given',
    Role_RoleSettingsForm_Roles_FieldLabel: 'Roles',
    Role_RoleSettingsForm_Roles_FromTitle: 'Available',
    Role_RoleSettingsForm_Roles_ToTitle: 'Contained',

    // Admin -> Security -> Users
    User_Text: 'Users',
    User_Description: 'Manage users',
    User_UserSettingsForm_Update_Error: 'You do not have permission to update users or is an external user',
    User_UserSettingsForm_Update_Success: 'User updated: ',
    User_UserSettingsForm_UpdateRoles_Success: 'User role mappings updated: {0}',
    User_UserSettingsExternalForm_Remove_Error: 'Cannot remove role',
    Users_Create_Title: 'Create User',
    User_UserAdd_Password_FieldLabel: 'Password',
    User_UserAdd_PasswordConfirm_FieldLabel: 'Confirm password',
    User_UserChangePassword_NoMatch_Error: 'Passwords do not match',
    User_UserAdd_Create_Error: 'You do not have permission to create users',
    User_UserAdd_Create_Success: 'User created: ',
    User_UserChangePassword_Title: 'Change Password',
    User_UserChangePassword_Password_FieldLabel: 'New password',
    User_UserChangePassword_PasswordConfirm_FieldLabel: 'Confirm password',
    User_UserChangePassword_Submit_Button: 'Change password',
    User_UserChangePassword_Cancel_Button: '@Button_Cancel',
    User_UserChangePassword_NoPermission_Error: 'You do not have permission to change your password',
    User_UserList_New_Button: 'Create local user',
    User_UserList_Source_Label: 'Source:',
    User_UserList_Default_Button: 'Default',
    User_UserList_Filter_EmptyText: 'Filter by user ID',
    User_UserList_ID_Header: 'User ID',
    User_UserList_Realm_Header: 'Realm',
    User_UserList_FirstName_Header: 'First name',
    User_UserList_LastName_Header: 'Last name',
    User_UserList_Email_Header: 'Email',
    User_UserList_Status_Header: 'Status',
    User_UserList_EmptyText: 'No users defined',
    User_UserFeature_Delete_Button: 'Delete user',
    Users_Delete_Success: 'User deleted: {0}',
    User_UserFeature_More_Button: 'More',
    User_UserFeature_ChangePasswordItem: 'Change password',
    Users_Change_Success: 'Password changed',
    User_UserFeature_Settings_Title: 'Settings',
    User_UserSettingsForm_ID_FieldLabel: 'ID',
    User_UserSettingsForm_ID_HelpText: 'This will be used as the username',
    User_UserSettingsForm_FirstName_FieldLabel: 'First name',
    User_UserSettingsForm_LastName_FieldLabel: 'Last name',
    User_UserSettingsForm_Email_FieldLabel: 'Email',
    User_UserSettingsForm_Email_HelpText: 'Used for notifications',
    User_UserSettingsForm_Status_FieldLabel: 'Status',
    User_UserSettingsForm_Status_EmptyText: 'Select status',
    User_UserSettingsForm_Status_ActiveItem: 'Active',
    User_UserSettingsForm_Status_DisabledItem: 'Disabled',
    User_UserSettingsForm_DefaultRole_FieldLabel: 'Applied Default-Role',
    User_UserSettingsForm_DefaultRole_HelpText: 'The following role is configured as the default role for this system and is automatically granted to all authenticated users',
    User_UserSettingsExternalForm_Roles_FieldLabel: 'Roles',
    User_UserSettingsExternalForm_Roles_FromTitle: 'Available',
    User_UserSettingsExternalForm_Roles_ToTitle: 'Granted',
    User_UserSettingsExternalForm_ExternalRoles_FieldLabel: 'External roles',
    User_UserSettingsExternalForm_ExternalRoles_HelpText: 'External roles should be managed at their source, and cannot be managed here.',

    // Admin -> Security -> Anonymous
    AnonymousSettings_Text: 'Anonymous',
    AnonymousSettings_Description: 'Browse server contents without authenticating',
    SamlConfiguration_Text: 'SAML Configuration',
    SamlConfiguration_Description: 'SAML Identity Provider Configuration',
    Security_AnonymousSettings_Update_Error: 'You do not have permission to configure the anonymous user',
    Security_AnonymousSettings_Update_Success: 'Anonymous security settings $action',
    Security_AnonymousSettings_Allow_BoxLabel: 'Allow anonymous users to access the server',
    Security_AnonymousSettings_Username_FieldLabel: 'Username',
    Security_AnonymousSettings_Realm_FieldLabel: 'Realm',

    // Admin -> Security -> LDAP
    LdapServers_Text: 'LDAP',
    LdapServers_Description: 'Manage LDAP server configuration',
    LdapServers_Update_Mask: 'Updating LDAP connection',
    LdapServers_Update_Success: 'LDAP server updated: {0}',
    Ldap_LdapServerConnectionForm_Update_Error: 'You do not have permission to update LDAP servers',
    LdapServers_Create_Mask: 'Creating LDAP connection',
    LdapServers_CreateConnection_Title: 'Create LDAP Connection',
    LdapServers_CreateUsersAndGroups_Title: 'Choose Users and Groups',
    LdapServers_Create_Success: 'LDAP server created: {0}',
    Ldap_LdapServerConnectionAdd_Create_Error: 'You do not have permission to create LDAP servers',
    LdapServers_Delete_Success: 'LDAP server deleted: {0}',
    Ldap_LdapServerChangeOrder_Title: 'Change LDAP servers ordering',
    LdapServers_ChangeOrder_Success: 'LDAP server order changed',
    Ldap_LdapServerUserAndGroupLoginCredentials_Title: 'Login Credentials',
    Ldap_LdapServerUserAndGroupLoginCredentials_Text: 'You have requested an operation which requires validation of your credentials.',
    Ldap_LdapServerUserAndGroupLoginCredentials_Input_Text: '<div>Enter your LDAP server credentials</div>',
    Ldap_LdapServerUserAndGroupLoginCredentials_Username_FieldLabel: 'LDAP server username',
    Ldap_LdapServerUserAndGroupLoginCredentials_Password_FieldLabel: 'LDAP server password',
    Ldap_LdapServerUserAndGroupLoginCredentials_Submit_Button: 'Test connection',
    Ldap_LdapServerUserAndGroupLoginCredentials_Cancel_Button: '@Button_Cancel',
    Ldap_LdapServerUserAndGroupMappingTestResults_Title: 'User Mapping Test Results',
    Ldap_LdapServerUserAndGroupMappingTestResults_ID_Header: 'User ID',
    Ldap_LdapServerUserAndGroupMappingTestResults_Name_Header: 'Name',
    Ldap_LdapServerUserAndGroupMappingTestResults_Email_Header: 'Email',
    Ldap_LdapServerUserAndGroupMappingTestResults_Roles_Header: 'Roles',
    Ldap_LdapServerList_New_Button: 'Create connection',
    Ldap_LdapServerList_ChangeOrder_Button: 'Change order',
    Ldap_LdapServerList_ClearCache_Button: 'Clear cache',
    Ldap_LdapServerList_Order_Header: 'Order',
    Ldap_LdapServerList_Name_Header: 'Name',
    Ldap_LdapServerList_URL_Header: 'URL',
    Ldap_LdapServerList_Filter_EmptyText: 'No LDAP servers matched "$filter"',
    Ldap_LdapServerList_EmptyText: '<div class="summary">There are no LDAP servers defined yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is LDAP?</h3>' +
        '<p>You can configure your NXRM instance to use LDAP for authentication and user role mapping. The repository ' +
        'manager can cache authentication information and supports multiple LDAP servers and user/group mappings ' +
        'to take advantage of the central authentication set up across your organization in all your repository managers. ' +
        'For more information check <a href="http://links.sonatype.com/products/nxrm3/docs/ldap" target="_blank" rel="noopener noreferrer">the ' +
        'documentation</a>.</p></div>',
    Ldap_LdapServerFeature_Delete_Button: 'Delete connection',
    Ldap_LdapServerFeature_Connection_Title: 'Connection',
    Ldap_LdapServerFeature_UserAndGroup_Title: 'User and group',
    LdapServers_ClearCache_Success: 'LDAP cache has been cleared',
    LdapServers_VerifyConnection_Mask: 'Checking connection to {0}',
    LdapServers_VerifyConnection_Success: 'Connection to LDAP server verified: {0}',
    LdapServers_VerifyMapping_Mask: 'Checking user mapping on {0}',
    LdapServers_VerifyMapping_Success: 'LDAP server user mapping verified: {0}',
    LdapServers_VerifyLogin_Mask: 'Checking login on {0}',
    LdapServers_VerifyLogin_Success: 'LDAP login completed successfully on: {0}',
    LdapServersConnectionFieldSet_Address_Text: 'LDAP server address:',
    LdapServersConnectionFieldSet_Address_HelpText: 'The LDAP server usually listens on port 389 (ldap://) or port 636 (ldaps://)',
    LdapServersConnectionFieldSet_Name_FieldLabel: 'Name',
    LdapServersConnectionFieldSet_Protocol_EmptyText: 'Protocol',
    LdapServersConnectionFieldSet_Protocol_PlainItem: 'ldap',
    LdapServersConnectionFieldSet_Protocol_SecureItem: 'ldaps',
    LdapServersConnectionFieldSet_Host_EmptyText: 'Hostname',
    LdapServersConnectionFieldSet_Port_EmptyText: 'Port',
    LdapServersConnectionFieldSet_Base_FieldLabel: 'Search base DN',
    LdapServersConnectionFieldSet_Base_HelpText: 'LDAP location to be added to the connection URL (e.g. "dc=example,dc=com")',
    LdapServersConnectionFieldSet_AuthMethod_FieldLabel: 'Authentication method',
    LdapServersConnectionFieldSet_AuthMethod_EmptyText: 'Select an authentication method',
    LdapServersConnectionFieldSet_AuthMethod_SimpleItem: 'Simple Authentication',
    LdapServersConnectionFieldSet_AuthMethod_AnonymousItem: 'Anonymous Authentication',
    LdapServersConnectionFieldSet_AuthMethod_DigestItem: 'DIGEST-MD5',
    LdapServersConnectionFieldSet_AuthMethod_CramItem: 'CRAM-MD5',
    LdapServersConnectionFieldSet_SaslRealm_FieldLabel: 'SASL realm',
    LdapServersConnectionFieldSet_SaslRealm_HelpText: 'The SASL realm to bind to (e.g. mydomain.com)',
    LdapServersConnectionFieldSet_Username_FieldLabel: 'Username or DN',
    LdapServersConnectionFieldSet_Username_HelpText: 'This must be a fully qualified username if simple authentication is used',
    LdapServersConnectionFieldSet_ChangePasswordItem: 'Change password',
    LdapServersConnectionFieldSet_Password_FieldLabel: 'Password',
    LdapServersConnectionFieldSet_Password_HelpText: 'The password to bind with',
    LdapServersConnectionFieldSet_Rules_Text: 'Connection rules',
    LdapServersConnectionFieldSet_Rules_HelpText: 'Set timeout parameters and max connection attempts to avoid being blacklisted',
    LdapServersConnectionFieldSet_Rules_Text1: 'Wait ',
    LdapServersConnectionFieldSet_Rules_Text2: ' seconds before timeout. Retry after ',
    LdapServersConnectionFieldSet_Rules_Text3: ' seconds, max of ',
    LdapServersConnectionFieldSet_Rules_Text4: ' failed attempts.',
    Ldap_LdapServerConnectionForm_VerifyConnection_Button: 'Verify connection',
    Ldap_LdapServerUserAndGroupFieldSet_Template_FieldLabel: 'Configuration template',
    Ldap_LdapServerUserAndGroupFieldSet_Template_EmptyText: 'Select a template',
    Ldap_LdapServerUserAndGroupFieldSet_BaseDN_FieldLabel: 'User relative DN',
    Ldap_LdapServerUserAndGroupFieldSet_BaseDN_HelpText: 'The relative DN where user objects are found (e.g. ou=people). This value will have the Search base DN value appended to form the full User search base DN',
    Ldap_LdapServerUserAndGroupFieldSet_UserSubtree_FieldLabel: 'User subtree',
    Ldap_LdapServerUserAndGroupFieldSet_UserSubtree_HelpText: 'Are users located in structures below the user base DN?',
    Ldap_LdapServerUserAndGroupFieldSet_ObjectClass_FieldLabel: 'Object class',
    Ldap_LdapServerUserAndGroupFieldSet_ObjectClass_HelpText: 'LDAP class for user objects (e.g. inetOrgPerson)',
    Ldap_LdapServerUserAndGroupFieldSet_UserFilter_FieldLabel: 'User filter',
    Ldap_LdapServerUserAndGroupFieldSet_UserFilter_HelpText: 'LDAP search filter to limit user search (e.g. "attribute=foo" or "(|(mail=*@example.com)(uid=dom*))")',
    Ldap_LdapServerUserAndGroupFieldSet_UserID_FieldLabel: 'User ID attribute',
    Ldap_LdapServerUserAndGroupFieldSet_RealName_FieldLabel: 'Real name attribute',
    Ldap_LdapServerUserAndGroupFieldSet_Email_FieldLabel: 'Email attribute',
    Ldap_LdapServerUserAndGroupFieldSet_Password_FieldLabel: 'Password attribute',
    Ldap_LdapServerUserAndGroupFieldSet_Password_HelpText: 'If this field is blank the user will be authenticated against a bind with the LDAP server.',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMap_FieldLabel: 'Map LDAP groups as roles',
    Ldap_LdapServerUserAndGroupFieldSet_GroupType_FieldLabel: 'Group type',
    Ldap_LdapServerUserAndGroupFieldSet_GroupType_EmptyText: 'Select a group type',
    Ldap_LdapServerUserAndGroupFieldSet_GroupType_DynamicItem: 'Dynamic Groups',
    Ldap_LdapServerUserAndGroupFieldSet_GroupType_StaticItem: 'Static Groups',
    Ldap_LdapServerUserAndGroupFieldSet_GroupBaseDN_FieldLabel: 'Group relative DN',
    Ldap_LdapServerUserAndGroupFieldSet_GroupBaseDN_HelpText: 'The relative DN where group objects are found (e.g. ou=Group). This value will have the Search base DN value appended to form the full Group search base DN',
    Ldap_LdapServerUserAndGroupFieldSet_GroupSubtree_FieldLabel: 'Group subtree',
    Ldap_LdapServerUserAndGroupFieldSet_GroupSubtree_HelpText: 'Are groups located in structures below the group base DN.',
    Ldap_LdapServerUserAndGroupFieldSet_GroupObject_FieldLabel: 'Group object class',
    Ldap_LdapServerUserAndGroupFieldSet_GroupObject_HelpText: 'LDAP class for group objects (e.g. posixGroup)',
    Ldap_LdapServerUserAndGroupFieldSet_GroupID_FieldLabel: 'Group ID attribute',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMember_FieldLabel: 'Group member attribute',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMember_HelpText: 'LDAP attribute containing the usernames for the group.',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMemberFormat_FieldLabel: 'Group member format',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMemberFormat_HelpText: 'The format of user ID stored in the group member attribute (e.g. "uid=${username},ou=people,dc=example,dc=com")',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMemberOf_FieldLabel: 'Group member of attribute',
    Ldap_LdapServerUserAndGroupFieldSet_GroupMemberOf_HelpText: 'Set this to the attribute used to store the attribute which holds groups DN in the user object',
    Ldap_LdapServerUserAndGroupForm_VerifyGroupMapping_Button: 'Verify user mapping',
    Ldap_LdapServerUserAndGroupForm_VerifyLogin_Button: 'Verify login',

    // Admin -> Security -> SSL Certificates
    SslCertificates_Text: 'SSL Certificates',
    SslCertificates_Description: 'Manage trusted SSL certificates for use with the Nexus Repository truststore',
    SslCertificates_Paste_Title: 'Paste Certificate as PEM',
    Ssl_SslCertificateAddFromPem_Cancel_Button: '@Button_Cancel',
    SslCertificates_Load_Title: 'Load Certificate from Server',
    Ssl_SslCertificateAddFromServer_Load_FieldLabel: 'Please enter a hostname, hostname:port or a URL to fetch a SSL certificate from',
    SslTrustStore_Load_Mask: 'Loading certificate&hellip;',
    Ssl_SslCertificateAddFromServer_Cancel_Button: '@Button_Cancel',
    SslCertificates_Load_Success: 'SSL Certificate created: {0}',
    Ssl_SslCertificateList_New_Button: 'Load certificate',
    Ssl_SslCertificateList_Load_Button: 'Load from server',
    Ssl_SslCertificateList_Paste_Button: 'Paste PEM',
    Ssl_SslCertificateList_Name_Header: 'Name',
    Ssl_SslCertificateList_IssuedTo_Header: 'Issued to',
    Ssl_SslCertificateList_IssuedBy_Header: 'Issued by',
    Ssl_SslCertificateList_Fingerprint_Header: 'Fingerprint',
    Ssl_SslCertificateList_EmptyText: '<div class="summary">There are no SSL certificates defined yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is SSL?</h3>' +
        '<p>Using Secure Socket Layer (SSL) communication with the repository manager is an important security ' +
        'feature and a recommended best practice. Secure communication can be inbound or outbound. Outbound client ' +
        'communication may include integration with: proxy repository, email servers, LDAPS servers. Inbound client ' +
        'communication includes: web browser HTTPS access, tool access to repository content, usage of REST APIs. ' +
        'For more information check <a href="http://links.sonatype.com/products/nxrm3/docs/ssl-certificate" target="_blank" rel="noopener noreferrer">the ' +
        'documentation</a>.</p></div>',
    Ssl_SslCertificateList_Filter_EmptyText: 'No SSL certificates matched "$filter"',
    Ssl_SslCertificateDetailsWindow_Title: 'Certificate Details',
    SslCertificates_Remove_Button: 'Remove certificate from truststore',
    SslCertificates_Add_Button: 'Add certificate to truststore',
    Ssl_SslCertificateFeature_Delete_Button: 'Delete certificate',
    SslCertificates_Delete_Success: 'SSL Certificate deleted: {0}',
    Ssl_SslCertificateDetailsWindow_Cancel_Button: '@Button_Cancel',
    Ssl_SslCertificateDetailsForm_Subject_Title: 'Subject',
    Ssl_SslCertificateDetailsForm_SubjectCommonName_FieldLabel: 'Common name',
    Ssl_SslCertificateDetailsForm_SubjectOrganization_FieldLabel: 'Organization',
    Ssl_SslCertificateDetailsForm_SubjectUnit_FieldLabel: 'Unit',
    Ssl_SslCertificateDetailsForm_Issuer_Title: 'Issuer',
    Ssl_SslCertificateDetailsForm_IssuerName_FieldLabel: 'Common name',
    Ssl_SslCertificateDetailsForm_IssuerOrganization_FieldLabel: 'Organization',
    Ssl_SslCertificateDetailsForm_IssuerUnit_FieldLabel: 'Unit',
    Ssl_SslCertificateDetailsForm_Certificate_Title: 'Certificate',
    Ssl_SslCertificateDetailsForm_CertificateIssuedOn_FieldLabel: 'Issued on',
    Ssl_SslCertificateDetailsForm_CertificateValidUntil_FieldLabel: 'Valid until',
    Ssl_SslCertificateDetailsForm_CertificateFingerprint_FieldLabel: 'Fingerprint',
    Ssl_SslCertificateDetailsForm_RetrievedUntrustedConnection_Html: '<b>This certificate was retrieved over an untrusted connection. Always verify the details before adding it.</b>',

    // Admin -> Support
    FeatureGroups_Support_Text: 'Support',
    FeatureGroups_Support_Description: 'Support tools',

    // Admin -> Support -> System Information
    SysInfo_Title: 'System Information',
    SysInfo_Description: 'Shows system information',
    SysInfo_Load_Mask: 'Loading&hellip;',
    Support_SysInfo_Download_Button: 'Download',

    // Admin -> System
    FeatureGroups_System_Text: 'System',
    FeatureGroups_System_Description: 'System administration',

    // Admin -> System -> API
    Api_Text: 'API',
    Api_Description: 'Learn how to interact with Sonatype Nexus Repository programmatically',

    // Admin -> System -> Capabilities
    Capabilities_Text: 'Capabilities',
    Capabilities_Description: 'Manage capabilities',
    Capabilities_Update_Mask: 'Updating capability',
    Capabilities_Enable_Mask: 'Enabling capability',
    Capabilities_Disable_Mask: 'Disabling capability',
    Capabilities_Update_Error: 'You do not have permission to update capabilities',
    Capability_CapabilityAdd_Create_Error: 'You do not have permission to update capabilities',
    Capabilities_Update_Success: 'Capability updated: {0}',
    Capability_CapabilitySettingsForm_Update_Error: 'You do not have permission to create capabilities',
    Capabilities_Create_Title: 'Create {0} Capability',
    Capabilities_Create_Success: 'Capability created: {0}',
    Capabilities_Delete_Success: 'Capability deleted: {0}',
    Capability_CapabilityList_New_Button: 'Create capability',
    Capability_CapabilityList_Type_Header: 'Type',
    Capability_CapabilityList_Description_Header: 'Description',
    Capability_CapabilityList_Notes_Header: 'Notes',
    Capability_CapabilityList_EmptyText: 'No capabilities defined',
    Capability_CapabilityList_Filter_EmptyText: 'No capability matched criteria "$filter"',
    Capability_CapabilityFeature_Delete_Button: 'Delete',
    Capability_CapabilityFeature_Enable_Button: 'Enable',
    Capability_CapabilityFeature_Disable_Button: 'Disable',
    Capability_CapabilitySummary_Title: 'Summary',
    Capability_CapabilitySettings_Title: 'Settings',
    Capability_CapabilitySettingsForm_Enabled_FieldLabel: 'Enable this capability',
    Capability_CapabilitySummary_Status_Title: 'Status',
    Capability_CapabilitySummary_About_Title: 'About',
    Capability_CapabilitySummary_Notes_Title: 'Notes',
    Capabilities_Enable_Text: 'Capability enabled: {0}',
    Capabilities_Disable_Text: 'Capability disabled: {0}',
    Capabilities_Select_Title: 'Select Capability Type',
    Capability_CapabilitySelectType_Description_Header: 'Description',
    Capability_CapabilitySelectType_Type_Header: 'Type',
    Capabilities_TypeName_Text: 'Type',
    Capabilities_Description_Text: 'Description',
    Capabilities_State_Text: 'State',
    Capability_CapabilitySummary_Notes_HelpText: 'Optional notes about configured capability',
    Capability_CapabilityStatus_EmptyText: 'This capability does not provide any status',
    Capability_Settings_Enabled_Label: 'Capability is enabled',
    Capability_Settings_Disabled_Label: 'Capability is disabled',

    // Admin -> System -> Cleanup Policies
    CleanupPolicies_Text: 'Cleanup Policies',
    CleanupPolicies_Description: 'Manage component removal configuration',
    CleanupPolicies_Create_Title: 'Create Cleanup Policy',
    CleanupPolicies_Delete_Title: 'Confirm deletion?',
    CleanupPolicies_Delete_Description: 'This Cleanup Policy is not used by any repository',
    CleanupPolicies_Delete_Description_Multiple: 'This Cleanup Policy is used by {0} repositories',
    CleanupPolicies_Delete_Success: 'Cleanup Policy deleted: {0}',
    CleanupPolicy_CleanupPolicyList_Preview_Button: 'Preview results',
    CleanupPolicy_CleanupPolicyList_New_Button: 'Create Cleanup Policy',
    CleanupPolicy_CleanupPolicyList_Filter_EmptyState: 'No cleanup policies matched "$filter"',
    CleanupPolicy_CleanupPolicyList_EmptyState: '<div class="summary">There are no cleanup policies created yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is a cleanup policy?</h3>' +
        '<p>Cleanup policies can be used to remove content from your repositories. These policies will execute at ' +
        'the configured frequency. Once created, a cleanup policy must be assigned to a repository from ' +
        '<a href="#admin/repository/repositories">the repository configuration screen</a>. For more information, ' +
        'check <a href="http://links.sonatype.com/products/nxrm3/docs/cleanup-policy" target="_blank" rel="noopener noreferrer">the ' +
        'documentation</a>.</p></div>',
    CleanupPolicy_CleanupPolicyList_Name_Header: 'Name',
    CleanupPolicy_CleanupPolicyList_Format_Header: 'Format',
    CleanupPolicy_CleanupPolicyList_Notes_Header: 'Notes',
    CleanupPolicy_CleanupPolicyFeature_Settings_Title: 'Settings',
    CleanupPolicy_CleanupPolicyFeature_Delete_Button: 'Delete',
    CleanupPolicy_CleanupPolicySettingsForm_Update_Success: 'Cleanup Policy updated: ',
    CleanupPolicy_CleanupPolicySettingsForm_Update_Error: 'You do not have permission to update Cleanup Policies',
    CleanupPolicy_CleanupPolicySettingsForm_CleanupPolicy_Title: 'Cleanup Policy',
    CleanupPolicy_CleanupPolicySettingsForm_Name_FieldLabel: 'Name',
    CleanupPolicy_CleanupPolicySettingsForm_Name_HelpText: 'A unique name for the cleanup policy',
    CleanupPolicy_CleanupPolicySettingsForm_Format_FieldLabel: 'Format',
    CleanupPolicy_CleanupPolicySettingsForm_Format_HelpText: 'The format that this cleanup policy can be applied to',
    CleanupPolicy_CleanupPolicySettingsForm_Notes_FieldLabel: 'Notes',
    CleanupPolicy_CleanupPolicySettingsForm_Criteria_Title: 'Criteria',
    CleanupPolicy_CleanupPolicySettingsForm_AddCriteria_Text: 'Add criteria',
    CleanupPolicy_CleanupPolicySettingsForm_LastBlobUpdated_FieldLabel: 'Published Before',
    CleanupPolicy_CleanupPolicySettingsForm_LastBlobUpdated_HelpText: 'Restrict cleanup to components that were published to NXRM more than the given number of days ago. (Blob updated date)',
    CleanupPolicy_CleanupPolicySettingsForm_LastDownloaded_FieldLabel: 'Last Downloaded Before',
    CleanupPolicy_CleanupPolicySettingsForm_LastDownloaded_HelpText: 'Restrict cleanup to components that were last downloaded more than the given number of days ago (Last downloaded date) OR never downloaded with uploaded date more than the given number of days ago',
    CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_FieldLabel: 'Release Type',
    CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_HelpText: 'Restrict cleanup to components that are of this release type',
    CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_Prereleases_Item: 'Pre-Release / Snapshot Versions',
    CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_Releases_Item: 'Release Versions',
    CleanupPolicy_CleanupPolicySettingsForm_Regex_FieldLabel: 'Asset Name Matcher',
    CleanupPolicy_CleanupPolicySettingsForm_Regex_HelpText: 'Restrict cleanup to components which have at least one asset name matching the specified <a href="http://links.sonatype.com/products/nexus/cleanup-policies/asset-name-matcher-regex" target="_blank" rel="noopener">regular expression pattern</a>.' +
        '<div>Before using this feature refer to our <a href="http://links.sonatype.com/products/nexus/cleanup-policies/asset-name-matcher" target="_blank" rel="noopener">documentation</a> and preview results.</div>',
    CleanupPolicy_CleanupPolicyAdd_Create_Error: 'You do not have permission to create Cleanup Policies',
    CleanupPolicy_CleanupPolicyAdd_Create_Success: 'Cleanup Policy created: ',
    CleanupPolicy_CleanupPolicyPreviewWindow_Title: 'Cleanup Policy preview',
    CleanupPolicy_CleanupPolicyPreviewWindow_repository_FieldLabel: 'Repository to Preview',
    CleanupPolicy_CleanupPolicyPreviewWindow_repository_HelpText: 'Select a repository to preview what might get cleaned up if this policy was applied',
    CleanupPolicy_CleanupPolicyPreviewWindow_repository_EmptyText: 'Select a repository',
    CleanupPolicy_CleanupPolicyPreviewWindow_Preview_Button: 'Preview',
    CleanupPolicy_CleanupPolicyPreviewWindow_EmptyText_View: 'No assets in repository matched the criteria',
    CleanupPolicy_CleanupPolicyPreviewWindow_EmptyText_Filter: 'No assets matched "$filter"',
    CleanupPolicy_CleanupPolicyPreviewWindow_Group_Column: 'Group',
    CleanupPolicy_CleanupPolicyPreviewWindow_Name_Column: 'Name',
    CleanupPolicy_CleanupPolicyPreviewWindow_Version_Column: 'Version',
    CleanupPolicy_CleanupPolicyPreviewWindow_Total_Component_Count: 'Component count (matching criteria) viewing',
    CleanupPolicy_CleanupPolicyPreviewWindow_Total_Component_Count_Out_Of: ' out of ',
    CleanupPolicy_CleanupPolicyPreviewWindow_Warning: 'Results may only be a sample of what will be deleted using the current criteria',

    // Admin -> System -> Email Server
    SmtpSettings_Text: 'Email Server',
    SmtpSettings_Description: 'Manage email server configuration',
    System_SmtpSettings_Update_Error: 'You do not have permission to configure email server',
    System_SmtpSettings_Update_Success: 'Email server configuration $action',
    System_SmtpSettings_Enabled_FieldLabel: 'Enabled',
    System_SmtpSettings_Host_FieldLabel: 'Host',
    System_SmtpSettings_Port_FieldLabel: 'Port',
    System_SmtpSettings_Username_FieldLabel: 'Username',
    System_SmtpSettings_Password_FieldLabel: 'Password',
    System_SmtpSettings_FromAddress_FieldLabel: 'From address',
    System_SmtpSettings_SubjectPrefix_FieldLabel: 'Subject prefix',
    System_SmtpSettings_SslTlsSection_FieldLabel: 'SSL/TLS options',
    System_SmtpSettings_StartTlsEnabled_FieldLabel: 'Enable STARTTLS support for insecure connections',
    System_SmtpSettings_StartTlsRequired_FieldLabel: 'Require STARTTLS support',
    System_SmtpSettings_SslOnConnectEnabled_FieldLabel: 'Enable SSL/TLS encryption upon connection',
    System_SmtpSettings_SslCheckServerIdentityEnabled_FieldLabel: 'Enable server identity check',

    System_SmtpSettings_VerifyServer_Button: 'Verify email server',
    System_VerifySmtpConnection_VerifyServer_Title: 'Verify Email Server',
    System_VerifySmtpConnection_HelpText: 'Where do you want to send the test email?',
    SmtpSettings_Verify_Mask: 'Checking email server {0}',
    SmtpSettings_Verify_Success: 'Email server verification email sent successfully',

    // Admin -> System -> HTTP
    HttpSettings_Text: 'HTTP',
    HttpSettings_Description: 'Manage outbound HTTP/HTTPS configuration',
    System_HttpSettings_Update_Error: 'You do not have permission to configure HTTP',
    System_HttpSettings_Update_Success: 'HTTP system settings $action',
    System_HttpSettings_Proxy_Title: 'HTTP proxy',
    System_HttpSettings_ProxyHost_FieldLabel: 'HTTP proxy host',
    System_HttpSettings_ProxyHost_HelpText: 'No http:// required (e.g. "proxy-host" or "192.168.1.101")',
    System_HttpSettings_ProxyPort_FieldLabel: 'HTTP proxy port',
    System_HttpSettings_Authentication_Title: 'Authentication',
    System_HttpSettings_ExcludeHosts_FieldLabel: 'Hosts to exclude from HTTP/HTTPS proxy',
    System_HttpSettings_ExcludeHosts_HelpText: 'Accepts Java "http.nonProxyHosts" wildcard patterns (one per line, no \'|\' hostname delimiters)',
    System_HttpSettings_HttpsProxy_Title: 'HTTPS proxy',
    System_HttpSettings_HttpsProxyHost_FieldLabel: 'HTTPS proxy host',
    System_HttpSettings_HttpsProxyHost_HelpText: 'No https:// required (e.g. "proxy-host" or "192.168.1.101")',
    System_HttpSettings_HttpsProxyPort_FieldLabel: 'HTTPS proxy port',
    System_HttpSettings_HttpsProxyAuthentication_Title: 'Authentication',

    // Admin -> System -> Bundles
    Bundles_Text: 'Bundles',
    Bundles_Description: 'View OSGI bundles',
    System_BundleList_Filter_EmptyText: 'No bundles matched "$filter"',
    System_BundleList_ID_Header: 'ID',
    System_BundleList_Name_Header: 'Name',
    System_BundleList_SymbolicName_Header: 'Symbolic Name',
    System_BundleList_Version_Header: 'Version',
    System_BundleList_State_Header: 'State',
    System_BundleList_Location_Header: 'Location',
    System_BundleList_Level_Header: 'Level',
    System_BundleList_Fragment_Header: 'Fragment',
    System_Bundles_Details_Tab: 'Summary',
    Bundles_ID_Info: 'ID',
    Bundles_Name_Info: 'Name',
    Bundles_SymbolicName_Info: 'Symbolic Name',
    Bundles_Version_Info: 'Version',
    Bundles_State_Info: 'State',
    Bundles_Location_Info: 'Location',
    Bundles_StartLevel_Info: 'Start Level',
    Bundles_Fragment_Info: 'Fragment',
    Bundles_Fragments_Info: 'Fragments',
    Bundles_FragmentHosts_Info: 'Fragment Hosts',
    Bundles_LastModified_Info: 'Last Modified',
    Bundles_Summary_Info: '{0}',

    // Admin -> System -> Nodes
    Nodes_Toggling_read_only_mode: 'Toggling read-only mode',
    Nodes_Disable_read_only_mode: 'Disable read-only mode',
    Nodes_Disable_read_only_mode_dialog: 'Disable read-only mode?',
    Nodes_Enable_read_only_mode: 'Enable read-only mode',
    Nodes_Enable_read_only_mode_dialog: 'Enable read-only mode?',
    Nodes_Read_only_mode_warning: 'Nexus Repository is in read-only mode',
    Nodes_force_release_dialog: 'Forcibly disable read-only mode?',
    Nodes_force_release: 'Force disable read-only mode',
    Nodes_Quorum_lost_warning: 'Not enough Nexus Repository Manager nodes in the cluster are reachable so quorum cannot be achieved; database is read only. <a href="#admin/system/nodes/clusterreset">Troubleshoot</a>',
    Nodes_OSS_Message: 'You are running a single-node instance of Nexus Repository Manager.',
    Nodes_enable_read_only_mode_dialog_description: 'Are you sure you want to reject additions of new' +
        ' components and changes to configuration?',
    Nodes_disable_read_only_mode_dialog_description: 'Are you sure you want to stop rejecting additions of new' +
        ' components and changes to configuration?',
    Nodes_force_release_warning: 'Warning: read-only mode has been enabled by system tasks. Releasing read-only mode before those tasks are complete may cause them to fail and/or cause data loss.',
    Nodes_force_release_confirmation: 'Are you sure you want to forcibly release read-only mode?',
    Nodes_NodeSettings_Title: 'Edit Node',
    Nodes_NodeSettingsForm_Update_Error: 'You do not have permission to update nodes',
    Nodes_NodeSettingsForm_Update_Success: 'Node updated, node is now named: ',
    Nodes_NodeSettingsForm_ID_FieldLabel: 'Node ID',
    Nodes_NodeSettingsForm_ID_HelpText: 'System-generated node identity',
    Nodes_NodeSettingsForm_Local_FieldLabel: 'Local',
    Nodes_NodeSettingsForm_Local_HelpText: 'Whether the current UI session is connected to the listed node',
    Nodes_NodeSettingsForm_SocketAddress_FieldLabel: 'Socket Address',
    Nodes_NodeSettingsForm_SocketAddress_HelpText: 'The IP address and port number used by the listed node to communicate with the cluster',
    Nodes_NodeSettingsForm_FriendlyName_FieldLabel: 'Node Name',
    Nodes_NodeSettingsForm_FriendlyName_HelpText: 'Custom alias for this node',

    // Admin -> System -> Tasks
    Tasks_Text: 'Tasks',
    Tasks_Description: 'Manage scheduled tasks',
    Tasks_Select_Title: 'Select a Type',
    Task_TaskSelectType_Filter_EmptyText: 'No types matched "$filter"',
    Task_TaskSelectType_Name_Header: 'Type',
    Tasks_Update_Mask: 'Updating task',
    Tasks_Run_Mask: 'Running task',
    Tasks_Stop_Mask: 'Stopping task',
    Task_TaskAdd_Create_Error: 'You do not have permission to create tasks',
    Tasks_Create_Title: 'Create {0} Task',
    Tasks_Create_Success: 'Task created: {0}',
    Task_TaskList_New_Button: 'Create task',
    Task_TaskList_Name_Header: 'Name',
    Task_TaskList_Type_Header: 'Type',
    Task_TaskList_Status_Header: 'Status',
    Task_TaskList_Schedule_Header: 'Schedule',
    Task_TaskList_NextRun_Header: 'Next run',
    Task_TaskList_LastRun_Header: 'Last run',
    Task_TaskList_LastResult_Header: 'Last result',
    Task_TaskList_EmptyState: '<div class="summary">There are no scheduled tasks defined yet<br>' +
        '<span style="font-weight: lighter; font-size: small;">or you don\'t have permission to browse them</span></div>' +
        '<div class="panel nx-subsection"><h3 class="title"><span class="icon"></span>What is a scheduled task?</h3>' +
        '<p>The repository manager allows you to schedule the execution of maintenance tasks. The tasks can ' +
        'carry out regular maintenance steps that will be applied to all repositories or to specific repositories ' +
        'on a configurable schedule or simply perform other system maintenance. For more information, ' +
        'check <a href="http://links.sonatype.com/products/nxrm3/docs/scheduled-task" target="_blank" rel="noopener noreferrer">the ' +
        'documentation</a>.</p></div>',
    Task_TaskList_Filter_EmptyState: 'No scheduled tasks matched "$filter"',
    Task_TaskFeature_Delete_Button: 'Delete task',
    Tasks_Delete_Success: 'Task deleted: {0}',
    Task_TaskFeature_Run_Button: 'Run',
    Task_TaskFeature_Run_Button_Id: 'task_run_button_id',
    Tasks_RunConfirm_Title: 'Confirm?',
    Tasks_RunConfirm_HelpText: 'Run {0} task?',
    Tasks_Run_Success: 'Task started: {0}',
    Tasks_Run_Disabled: 'Task is disabled',
    Task_TaskFeature_Stop_Button: 'Stop',
    Tasks_StopConfirm_Title: 'Confirm?',
    Tasks_StopConfirm_HelpText: 'Stop {0} task?',
    Tasks_Stop_Success: 'Task stopped: {0}',
    TaskFeature_Summary_Title: 'Summary',
    Tasks_Settings_Title: 'Settings',
    Tasks_ID_Info: 'ID',
    Tasks_Name_Info: 'Name',
    Tasks_Type_Info: 'Type',
    Tasks_Status_Info: 'Status',
    Tasks_NextRun_Info: 'Next run',
    Tasks_LastRun_Info: 'Last run',
    Tasks_LastResult_Info: 'Last result',
    Task_TaskSettingsForm_Update_Error: 'You do not have permission to update tasks or task is readonly',
    Tasks_Update_Success: 'Task updated: {0}',
    Task_TaskSettingsForm_Enabled_FieldLabel: 'Task enabled',
    Task_TaskSettingsForm_Enabled_HelpText: 'This flag determines if the task is currently active.  To disable this task for a period of time, de-select this checkbox.',
    Task_TaskSettingsForm_Name_FieldLabel: 'Task name',
    Task_TaskSettingsForm_Name_HelpText: 'A name for the scheduled task',
    Task_TaskSettingsForm_Email_FieldLabel: 'Notification email',
    Task_TaskSettingsForm_Email_HelpText: 'The email address where an email will be sent if the condition below is met',
    Task_TaskSettingsForm_NotificationCondition_FieldLabel: 'Send notification on',
    Task_TaskSettingsForm_NotificationCondition_HelpText: 'Conditions that will trigger a notification email',
    Task_TaskSettingsForm_NotificationCondition_FailureItem: 'Failure',
    Task_TaskSettingsForm_NotificationCondition_SuccessFailureItem: 'Success or Failure',
    Task_TaskScheduleFieldSet_Recurrence_FieldLabel: 'Task frequency',
    Task_TaskScheduleFieldSet_Recurrence_HelpText: 'The frequency this task will run. Manual - this task can only be run manually. Once - run the task once at the specified date/time. Daily - run the task every day at the specified time. Weekly - run the task every week on the specified day at the specified time. Monthly - run the task every month on the specified day(s) and time. Advanced - run the task using the supplied cron string.',
    Task_TaskScheduleFieldSet_Recurrence_EmptyText: 'Select a frequency',
    Task_TaskScheduleFieldSet_Recurrence_ManualItem: 'Manual',
    Task_TaskScheduleFieldSet_Recurrence_OnceItem: 'Once',
    Task_TaskScheduleFieldSet_Recurrence_HourlyItem: 'Hourly',
    Task_TaskScheduleFieldSet_Recurrence_DailyItem: 'Daily',
    Task_TaskScheduleFieldSet_Recurrence_WeeklyItem: 'Weekly',
    Task_TaskScheduleFieldSet_Recurrence_MonthlyItem: 'Monthly',
    Task_TaskScheduleFieldSet_Recurrence_AdvancedItem: 'Advanced (provide a CRON expression)',
    Task_TaskScheduleDaily_StartDate_FieldLabel: 'Start date',
    Task_TaskScheduleHourly_EndDate_FieldLabel: 'Start time',
    Task_TaskScheduleDaily_Recurring_FieldLabel: 'Time to run this task',
    Task_TaskScheduleMonthly_Days_FieldLabel: 'Days to run this task',
    Task_TaskScheduleMonthly_Days_BlankText: 'At least one day should be selected',
    Task_TaskScheduleAdvanced_Cron_FieldLabel: 'CRON expression',
    Task_TaskScheduleAdvanced_Cron_EmptyText: '* * * * * * *',
    Task_TaskScheduleAdvanced_Cron_HelpText: 'A cron expression that will control the running of the task.',
    Task_TaskScheduleAdvanced_Cron_AfterBodyEl: '<div style="font-size: 11px"><p>From left to right the fields and accepted values are:</p>' +
        '<table>' +
        '<thead><tr><th>Field Name</th><th>Allowed Values</th></tr></thead>' +
        '<tbody>' +
        '<tr><td>Seconds</td><td>0-59</td></tr>' +
        '<tr><td>Minutes</td><td>0-59</td></tr>' +
        '<tr><td>Hours</td><td>0-23</td></tr>' +
        '<tr><td>Day of month</td><td>1-31</td></tr>' +
        '<tr><td>Month</td><td>1-12 or JAN-DEC</td></tr>' +
        '<tr><td>Day of week</td><td>1-7 or SUN-SAT</td></tr>' +
        '<tr><td>Year(optional)</td><td>empty, 1970-2099</td></tr>' +
        '</tbody>' +
        '</table>' +
        '<br/>' +
        '<p>Special tokens include: * (all acceptable values), ? (no specific value), - (ranges, e.g. 10-12)</p>' +
        '</div> '
    ,
    Task_TaskScheduleManual_HelpText: 'Without recurrence, this service can only be run manually.',
    Task_Script_Creation_Disabled: '<i>Admin - Execute script</i> task creation is disabled. ' +
        '<a href="https://links.sonatype.com/products/nxrm3/disabled-groovy-scripting">More information</a>',

    // Authentication section
    System_AuthenticationSettings_Username_FieldLabel: 'Username',
    System_AuthenticationSettings_Password_FieldLabel: 'Password',
    System_AuthenticationSettings_WindowsNtlmHostname_FieldLabel: 'Windows NTLM hostname',
    System_AuthenticationSettings_WindowsNtlmDomain_FieldLabel: 'Windows NTLM domain',
    System_AuthenticationSettings_Bearer_Token_FieldLabel: 'Token',
    System_AuthenticationSettings_Bearer_Token_HelpText: 'Include only the token value, not the Bearer prefix.',
    System_AuthenticationSettings_Preemptive_FieldLabel: 'Use pre-emptive authentication',
    System_AuthenticationSettings_Preemptive_HelpText: '<strong>Caution!</strong> Use this only when absolutely ' +
        'necessary. Enabling this option means configured authentication credentials will be sent to the remote URL ' +
        'regardless of whether the remote server has asked for them or not.',

    // HTTP Request section
    System_HttpRequestSettings_UserAgentCustomization_FieldLabel: 'User-agent customization',
    System_HttpRequestSettings_UserAgentCustomization_HelpText: 'Custom fragment to append to "User-Agent" header in HTTP requests',
    System_HttpRequestSettings_Timeout_FieldLabel: 'Connection/Socket timeout',
    System_HttpRequestSettings_Timeout_HelpText: 'Seconds to wait for activity before stopping and retrying the connection',
    System_HttpRequestSettings_Attempts_FieldLabel: 'Connection/Socket retry attempts',
    System_HttpRequestSettings_Attempts_HelpText: 'Total retries if the initial connection attempt suffers a timeout',

    //Nexus Lifecycle -> Server
    Clm_ClmSettings_Permission_Error: 'You do not have permission to configure IQ Server',
    Clm_Text: 'IQ Server',
    Clm_Description: 'Manage IQ Server configuration',
    Clm_Connection_Success: 'Connection to IQ Server verified: {0}',
    Clm_Dashboard_Link_Text: '<span class="x-fa fa-dashboard"></span>IQ Server Dashboard<span class="x-fa fa-external-link"></span>',
    Clm_Dashboard_Description: 'Open Dashboard for Sonatype Repository Firewall and Sonatype Lifecycle',
    Clm_Dashboard_Disabled_Tooltip: 'IQ Server must be enabled first',
    ClmSettings_Html: '<p><a href="http://www.sonatype.com/nexus/product-overview/nexus-lifecycle" target="_blank" rel="noopener">IQ Server</a> ' +
        'can evaluate application and organization policies.</p>' +
        '<p>To enable this feature configure the IQ Server URL, username and password.</p>',

    Clm_SettingsTestResults_Title: 'Applications',
    Clm_SettingsTestResults_EmptyText: 'No applications found',
    Clm_SettingsTestResults_Id_Header: 'Id',
    Clm_SettingsTestResults_Name_Header: 'Name',

    ClmSettings_Enable_FieldLabel: 'Enable IQ Server',
    ClmSettings_Enable_HelpText: 'Whether to use IQ Server',
    ClmSettings_URL_FieldLabel: 'IQ Server URL',
    ClmSettings_URL_HelpText: 'The address of your IQ Server',
    ClmSettings_URL_EmptyText: 'enter a URL',
    ClmSettings_AuthenticationType_FieldLabel: 'Authentication Method',
    ClmSettings_AuthenticationType_Pki: 'PKI Authentication',
    ClmSettings_AuthenticationType_User: 'User Authentication',
    ClmSettings_Username_FieldLabel: 'Username',
    ClmSettings_Username_HelpText: 'User with access to IQ Server',
    ClmSettings_Username_EmptyText: 'enter a name',
    ClmSettings_Password_FieldLabel: 'Password',
    ClmSettings_Password_HelpText: 'Credentials for the IQ Server User',
    ClmSettings_Password_EmptyText: 'enter a password',
    ClmSettings_ConnectionTimeout_FieldLabel: 'Connection Timeout',
    ClmSettings_ConnectionTimeout_HelpText: 'Seconds to wait for activity before stopping and retrying the connection. Leave blank to use the globally defined HTTP timeout.',
    ClmSettings_ConnectionTimeout_EmptyText: 'enter a timeout',
    ClmSettings_Properties_FieldLabel: 'Properties',
    ClmSettings_Properties_HelpText: 'Additional properties to configure for IQ Server',
    ClmSettings_Properties_EmptyText: 'enter properties',
    ClmSettings_Properties_Verify_Button: 'Verify connection',
    ClmSettings_Show_Link_FieldLabel: 'Show IQ Server Link',
    ClmSettings_Show_Link_HelpText: 'Show IQ Server link in Browse menu when server is enabled',

    //Settings form general error
    SettingsForm_Save_Error: 'An error occurred while saving the form'
  },

  /**
   * String bundles.
   *
   * @type {Object}
   */
  bundles: {
    'NX.coreui.migration.Controller': {
      Feature_Text: 'Upgrade',
      Feature_Description: 'Upgrade configuration and content from Sonatype Nexus Repository 2 to Sonatype Nexus Repository 3',

      Activate_Mask: 'Loading',

      Configure_Mask: 'Configuring',
      Configure_Message: 'Upgrade configured',

      Cancel_Confirm_Title: 'Cancel Upgrade',
      Cancel_Confirm_Text: 'Do you want to cancel upgrade?',
      Cancel_Mask: 'Canceling',
      Cancel_Message: 'Upgrade canceled',

      IncompleteCancel_Title: 'Configuration Incomplete',
      IncompleteCancel_Text: 'Upgrade has been partially configured and needs to be reset to continue.',
      IncompleteCancel_Mask: 'Resetting',

      PlanStepDetail_Mask: 'Fetching details'
    },

    'NX.coreui.migration.NoUpgradeHAScreen': {
      Title: 'High Availability Cluster (HA-C) Detected',
      Description: '<p>Upgrading from Nexus Repository Manager 2 while running a HA-C is not available.</p>' +
          '<p>Please run Nexus Repository Manager 3 as a single-node to continue.</p>'
    },

    'NX.coreui.migration.AgentScreen': {
      Title: 'Agent Connection',
      Description: "<p>Configure the connection to remote server's upgrade-agent.<br/>" +
          'The remote server must have an upgrade-agent configured and enabled.</p>',
      Endpoint_FieldLabel: 'URL',
      Endpoint_HelpText: "The base URL of the remote server",
      Token_FieldLabel: 'Access Token',
      Token_HelpText: "The access token from the remote server's upgrade-agent settings",
      FetchSize_FieldLabel: 'Fetch Size',
      FetchSize_HelpText: "Batch size of changes pulled from NXRM2 at once. Lower the value if you are having issues during the Synchronizing step."
    },

    'NX.coreui.migration.AgentStep': {
      Connect_Mask: 'Connecting',
      Connect_Message: 'Connected'
    },

    'NX.coreui.migration.ContentScreen': {
      Title: 'Content',
      Description: '<p>What content from Nexus Repository Manager 2 would you like to transfer?</p>',
      Repositories_FieldLabel: 'Repository configuration and content',
      Configuration_FieldLabel: 'Server configuration'
    },

    'NX.coreui.migration.OverviewScreen': {
      Title: 'Overview',
      Description: '<p>This wizard will help you upgrade from Sonatype Nexus Repository 2.</p>' +
          '<p>Before proceeding with this wizard, thoroughly review our <a href="https://links.sonatype.com/products/nxrm3/docs/upgrade2to3">comprehensive upgrade help documentation</a>.</p>' +
          '<p>You should also ensure that you have met the following <strong>minimum prerequisites for using this wizard</strong>:</p>' +
          '<ul>' +
          '<li>Nexus Repository 2 instance is on the latest version</li>' +
          '<li>Nexus Repository 2 and 3 have the same license type (OSS or Pro)</li>' +
          '<li>Nexus Repository 3 instance is a fresh/clean instance</li>' +
          '<li>All files in the Nexus Repository work directory are owned by the OS user, and there are no zero length files</li>' +
          '<li>Nexus Repository 2 repository and repository group Repository IDs differ by more than just case</li>' +
          '</ul>' +
          '<p><strong>We always recommend testing your upgrade in a test environment before upgrading a production instance</strong>.</p>'
    },

    'NX.coreui.migration.PhaseFinishScreen': {
      Title: 'Finishing',
      Description: '<p>Upgrade is finishing.</p>',
      Abort_Button: 'Abort',
      Done_Button: 'Done'
    },

    'NX.coreui.migration.RepositoryDefaultsScreen': {
      $extend: 'NX.coreui.migration.RepositoryCustomizeWindow',

      Title: 'Repository Defaults',
      Description: '<p>Configure the default settings used for repository upgrade.<br/>' +
          'Per-repository settings may be customized when selecting repositories to upgrade.</p>',
      IngestMethod_HelpText: 'Choose how the repository content should be transferred. The method you choose may not be supported by all repositories.'
    },

    'NX.coreui.migration.RepositoryCustomizeWindow': {
      Title: 'Customize {0}',

      BlobStore_FieldLabel: 'Blob store',
      BlobStore_HelpText: 'Choose where the repository content should be stored',
      BlobStore_EmptyText: 'Choose a blob store',

      IngestMethod_FieldLabel: 'Method',
      IngestMethod_HelpText: 'Choose how the repository content should be transferred',
      IngestMethod_EmptyText: 'Choose a repository content transfer method',
      IngestMethod_Link: 'Hard link (fastest)',
      IngestMethod_Copy: 'Filesystem copy (slow)',
      IngestMethod_Download: 'Download (slowest)'
    },

    'NX.coreui.migration.PlanStepDetailWindow': {
      Title: '{0}',
      EmptyLog: 'No progress',
      Timestamp_Column: 'Timestamp',
      Message_Column: 'Message'
    },

    'NX.coreui.migration.PreviewScreen': {
      Title: 'Preview',
      Description: '<p>Here is a preview of the upgrade configuration.</p>',
      Name_Column: 'Name',
      State_Column: 'State',
      Begin_Button: 'Begin'
    },

    'NX.coreui.migration.PreviewStep': {
      Begin_Confirm_Title: 'Begin Upgrade',
      Begin_Confirm_Text: 'Do you want to begin upgrade?',
      Begin_Mask: 'Upgrade beginning',
      Begin_Message: 'Upgrade begun'
    },

    'NX.coreui.migration.ProgressScreenSupport': {
      Name_Column: 'Name',
      Status_Column: 'Status',
      State_Column: 'State',
      Complete_Column: 'Complete'
    },

    'NX.coreui.migration.ProgressStepSupport': {
      Loading_Mask: 'Loading'
    },

    'NX.coreui.migration.RepositoriesScreen': {
      Title: 'Repositories',
      Description: '<p>Select the repositories to be upgraded.<br/>' +
          'Customize advanced configuration of the upgrade per-repository as needed.</p>',
      Repository_Column: 'Repository',
      Type_Column: 'Type',
      Format_Column: 'Format',
      Supported_Column: 'Supported',
      Status_Column: 'Status',
      Datastore_Column: 'Data store',
      Blobstore_Column: 'Blob store',
      Method_Column: 'Method',
      Action_Tooltip: 'Customize repository options'
    },

    'NX.coreui.migration.RepositoriesStep': {
      $extend: 'NX.coreui.migration.ProgressStepSupport'
    },

    'NX.coreui.migration.RepositoryDefaultsStep': {
      $extend: 'NX.coreui.migration.ProgressStepSupport'
    },

    'NX.coreui.migration.PhasePrepareScreen': {
      Title: 'Preparing',
      Description: '<p>Preparing for upgrade.</p>',
      Abort_Button: 'Abort',
      Continue_Button: 'Continue'
    },

    'NX.coreui.migration.PhasePrepareStep': {
      $extend: 'NX.coreui.migration.ProgressStepSupport',

      Abort_Confirm_Title: 'Abort Upgrade',
      Abort_Confirm_Text: 'Do you want to abort upgrade?',
      Abort_Mask: 'Upgrade aborting',
      Abort_Message: 'Upgrade aborted',

      Continue_Confirm_Title: 'Continue Upgrade',
      Continue_Confirm_Text: 'Do you want to continue upgrade?',
      Continue_Mask: 'Upgrade continuing',
      Continue_Message: 'Upgrade continuing'
    },

    'NX.coreui.migration.PhaseSyncScreen': {
      Title: 'Synchronizing',
      Description: '<p>Upgrade is synchronizing changes.</p>',
      Abort_Button: 'Abort',
      Continue_Button: 'Continue',
      Continue_Button_Pending: '<i class="fa fa-spinner fa-spin fa-fw"></i> Continue'
    },

    'NX.coreui.migration.PhaseSyncStep': {
      $extend: 'NX.coreui.migration.ProgressStepSupport',

      Abort_Confirm_Title: 'Abort Upgrade',
      Abort_Confirm_Text: 'Do you want to abort upgrade?',
      Abort_Mask: 'Upgrade aborting',
      Abort_Message: 'Upgrade aborted',

      Stop_Waiting_Confirm_Title: 'Stop waiting for changes',
      Stop_Waiting_Confirm_Text: 'Any future changes to repositories will not be synchronized. Proceed?',
      Stop_Waiting_Confirm_Mask: 'Finalizing changes',
      Stop_Waiting_Confirm_Message: 'Changes finalized',

      Finish_Mask: 'Upgrade finishing',
      Finish_Message: 'Upgrade finishing'
    },

    'NX.coreui.migration.PhaseFinishStep': {
      $extend: 'NX.coreui.migration.ProgressStepSupport',

      Abort_Confirm_Title: 'Abort Upgrade',
      Abort_Confirm_Text: 'Do you want to abort upgrade?',
      Abort_Mask: 'Upgrade aborting',
      Abort_Message: 'Upgrade aborted',

      Done_Mask: 'Confirming',
      Done_Message: 'Upgrade done',

      Done_Dialog_title: 'Upgrade Complete',
      Done_Dialog_prefix: '<p>Repository content is available immediately for direct download.</p>' +
          '<p>Tasks have been automatically scheduled to build:</p>' +
          '<ul>',
      Done_Dialog_with_browse: '<li>component Browse UI and HTML views</li>',
      Done_Dialog_with_search: '<li>component Search UI and Search REST APIs</li>',
      Done_Dialog_suffix: '</ul>' +
          '<p>All components will not be visible until tasks named "Repo 2 Migration" finish. ' +
          'Monitor status in the Tasks user interface or by examining the associated task log.</p>'
    },

    'NX.coreui.view.ldap.LdapSystemPasswordModal': {
      Title: 'LDAP Server system password',
      Password_FieldLabel: 'Password',
      Password_HelpText: 'The password to bind with',
      Button_OK: 'OK',
      Button_Cancel: 'Cancel'
    }
  }
}, function(self) {
  NX.I18n.register(self);
});
