#!/usr/bin/env python
# from conans import ConanFile, tools
from conans.tools import download

class JsonForModernCppConan(ConanFile):
    name = "jsonformoderncpp"
    version = "2.1.1"
    description = "JSON for Modern C++ parser and generator from https://github.com/nlohmann/json"
    license = "MIT"
    url = "https://github.com/vthiery/conan-jsonformoderncpp"
    author = "Vincent Thiery (vjmthiery@gmail.com)"

    def source(self):
        download("https://github.com/nlohmann/json/releases/download/v%s/json.hpp" % self.version, "json.hpp")

    def build(self):
        # as there is no LICENSE file, lets extract and generate it.
        # It is useful for package consumers, so they can collect all license from all dependencies
        tmp = tools.load("json.hpp")
        license_contents = tmp[2:tmp.find("*/", 1)]
        tools.save("LICENSE", license_contents)

    def package(self):
        self.copy("*.hpp", dst="include")
        self.copy("LICENSE")

    def package_info(self):
        self.cpp_info.libdirs = []
        self.cpp_info.bindirs = []
