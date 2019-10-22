package org.sonatype.nexus.repository.conda.internal.hosted;

import groovy.lang.Tuple2;
import org.apache.commons.lang.StringUtils;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.conda.internal.hosted.metadata.PackageDesc;
import org.sonatype.nexus.repository.storage.Asset;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class Utils {

    public static Tuple2<String, PackageDesc> toPackageDesc(Asset asset) {

        NestedAttributesMap attributes = asset.formatAttributes();
        PackageDesc packageDesc = new PackageDesc();

        packageDesc.setSize(asset.size().longValue());
        packageDesc.setMd5(asset.getChecksum(HashAlgorithm.MD5).toString());
        packageDesc.setSha256(asset.getChecksum(HashAlgorithm.SHA256).toString());
        packageDesc.setTimestamp(Long.parseLong(attributes.get("timestamp", 0).toString()));

        packageDesc.setArch(attributes.get("arch", "").toString());
        packageDesc.setBuild_number(Integer.parseInt(attributes.get("build_number", "").toString()));
        packageDesc.setBuild(attributes.get("build", "").toString());
        packageDesc.setName(attributes.get("name", "").toString());
        packageDesc.setSubdir(attributes.get("subdir", "").toString());
        packageDesc.setVersion(attributes.get("version", "").toString());
        packageDesc.setNoarch(attributes.get("noarch", "python").toString());
        packageDesc.setLicense(attributes.get("license", "").toString());

        if (attributes.contains("license_family"))
            packageDesc.setLicense_family(attributes.get("license_family").toString());

        packageDesc.setDepends(Arrays.asList(attributes.get("depends", "").toString().split(";")));

        String fineName = CondaPath.build(asset.name()).getFileName();
        return new Tuple2<>(fineName, packageDesc);
    }


    public static void toAttributes(PackageDesc packageDesc, NestedAttributesMap attributesMap) {
        attributesMap.set("arch", StringUtils.isEmpty(packageDesc.getArch()) ? "noarch" : packageDesc.getArch());
        attributesMap.set("build", packageDesc.getBuild());
        attributesMap.set("build_number", packageDesc.getBuild_number());
        attributesMap.set("noarch", packageDesc.getNoarch());
        attributesMap.set("version", packageDesc.getVersion());
        attributesMap.set("subdir", packageDesc.getSubdir());
        attributesMap.set("depends", packageDesc.getDepends().stream().collect(Collectors.joining(";")));
        attributesMap.set("name", packageDesc.getName());
        attributesMap.set("license", packageDesc.getLicense());
        attributesMap.set("timestamp", Instant.now().toEpochMilli());

        if (isNotEmpty(packageDesc.getLicense_family())) {
            attributesMap.set("license_family", packageDesc.getLicense_family());
        }
    }
}
