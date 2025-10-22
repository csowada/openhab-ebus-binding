# Changelog
All notable changes to this project will be documented in this file.

## Unreleased

## [5.0.0] - 2025-10-22
### Changed
- Update to openHAB 5.x verision
- Update eBUS core to version 1.1.14
- Update eBUS configuration to version 1.1.10

## [4.0.20] - 2025-01-31
### Changed
- Update eBUS core to version 1.1.13
  - Revert change from v1.1.9 to encode master data again
  - Update project dependencies
- Update eBUS configuration to version 1.1.9
  - Fix Wolf CWL 300/400 issue for setter fan_stepX
  - Add Cooling State to controller.d_values_rc2
- Update project pipelines, license header to 2025

## [4.0.19] - 2023-09-14
### Fixed
- Fix KAR builds for openHAB 4.0.3

## [4.0.18] - 2023-09-11
### Fixed
- Update binding feature to openHAB 4.0.2
- Remove version dependency in feature.xml

## [4.0.17] - 2023-08-27 (only snapshot release)
### Fixed
- Compile for openHAB 4.0 and Java 17 only
- Fix Binding configuration for custom JSON files

## [3.4.16] - 2023-03-25
### Changed
- Update dependencies to openHab 3.4.0 release
- Add openHAB 4.0 compatibility
- Update eBUS core to version 1.1.10
- Update eBUS configuration to version 1.1.8
- Fix some SonarCloud issues

## [3.2.14] - 2021-12-30
### Changed
- Update dependencies to openHab 3.2.0 release

## [3.1.13] - 2021-06-28
### Changed
- Update dependencies to openHab 3.1.0 release

## [3.1.12] - 2021-06-27
### Changed
- Fix polling isue
- Move to commons lang3 lib
- Smaller changes

## [3.0.11] - 2021-01-31
### Changed
- Removed repository entry in feature.xml to prevent compile time openhab version dependencies

## [2.50.11] - 2021-01-31
### Changed
- Removed repository entry in feature.xml to prevent compile time openhab version dependencies

## [3.0.10] - 2020-12-27
### Added
- Apply all code changes to support openHAB 3.0
### Changed
- Cherry-picked all changes from v2.50.10

## [2.50.10] - 2020-12-27
### Changed
- Bumped ``ebus core`` and ``ebus configuration`` lib to version 1.1.4
- Adjusted some methods to catch new exceptions from ebus core lib

### Fixed
- Bumped to new ebus libraries to fix a not changing connection status

## [2.50.9] - 2020-12-25
### News

🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

Warning: This version should be more robust due to many fixes, but it is possible that this version is not working well. Don't worry, be helpful and report a bug 😉

🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥

This is the first release from the single bindig repository that is not a fork of the official openHAB Addons2 repository anymore. It is now much easier to only update the binding without any impact of other official bindings. The Maven files are copied from the openHAB repo the keep the binding aligned with the official developments.

### Added
- Added support for build-in``SerialPortManager`` from openhab as new default serail driver
- Builds are now directly build on GitHub with Actions

### Changed
- Bumped ``ebus core`` and ``ebus configuration`` lib to version 1.1.3
- Added ``Nullable`` support to the project

### Fixed
- Fixed wrong ``ebusd`` driver instance check
- Fixed several issues based on the ``Nullable`` checks
- Fixed several compiler and test warnings