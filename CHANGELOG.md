# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [1.0.1] - 2026-05-22

### Security

- Upgrade `com.google.guava:guava` from `22.0` to `32.0.1-jre` to fix CVE vulnerabilities

### Changed

- Replace `mockito-all:1.10.19` with `mockito-core:5.11.0`
- Upgrade `jacoco-maven-plugin` from `0.8.2` to `0.8.12`
- Migrate deprecated `org.mockito.Matchers` to `org.mockito.ArgumentMatchers`