Name:       test-rpm
Version:    5.6.7
Release:    1
Summary:    Test RPM
Group:      Testing
License:    Testing
Source0:    foo-bar.tar.gz

BuildArch:  noarch
BuildRoot: %{_tmppath}/%{name}-root

%description
Test RPM

%prep
%setup -c

%build

%install
rm -rf ${RPM_BUILD_ROOT}
mkdir -p ${RPM_BUILD_ROOT}/tmp
install -m 644 foo.txt ${RPM_BUILD_ROOT}/tmp
install -m 644 bar.txt ${RPM_BUILD_ROOT}/tmp

%files
/tmp/foo.txt
/tmp/bar.txt


