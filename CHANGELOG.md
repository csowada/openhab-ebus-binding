# Changelog
All notable changes to this project will be documented in this file.

## Unreleased 

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