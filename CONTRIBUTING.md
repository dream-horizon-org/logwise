# Contributing to logwise

Thank you for your interest in contributing to logwise! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Submitting Pull Requests](#submitting-pull-requests)
- [Development Guidelines](#development-guidelines)
- [Commit Guidelines](#commit-guidelines)

## Code of Conduct

By participating in this project, you agree to maintain our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/logwise.git
   cd logwise
   ```
3. **Add the upstream repository** as a remote:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/logwise.git
   ```
4. **Create a new branch** for your feature or fix:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix-name
   ```

## How to Contribute

### Reporting Bugs

Before creating a bug report, please check the existing issues to avoid duplicates.

When creating a bug report, include:
- A clear and descriptive title
- Steps to reproduce the behavior
- Expected behavior
- Actual behavior
- Environment details (OS, versions, etc.)
- Any relevant logs or screenshots

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md).

### Suggesting Enhancements

Enhancement suggestions help us improve the project. Please include:
- A clear and descriptive title
- Detailed description of the enhancement
- Use cases and examples
- Potential implementation approaches (optional)

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md).

### Submitting Pull Requests

1. **Update your fork** with the latest changes:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```

2. **Push to your fork** and create a pull request:
   ```bash
   git push origin feature/your-feature-name
   ```

3. **Fill out the pull request template** with details about your changes.

## Development Guidelines

- **Follow existing code style** and conventions
- **Write clear, self-documenting code**
- **Add comments** for complex logic
- **Update documentation** when adding new features
- **Write tests** for new functionality
- **Ensure all tests pass** before submitting

### Code Formatting

For contributions to the Spark project, please ensure your code is properly formatted using Google Java Format:

- **Before opening a PR**, run `mvn fmt:format` in the `spark` directory to automatically format your code
- The formatting check runs automatically on pull requests via GitHub Actions
- PRs with formatting issues will fail the formatting check and cannot be merged until fixed
- You can verify formatting locally by running `mvn fmt:check` in the `spark` directory

## Commit Guidelines

Use clear and descriptive commit messages:

- Use the imperative mood: "Add feature" not "Added feature"
- Start with a capital letter
- Keep the first line under 72 characters
- Reference issues and pull requests when applicable

Examples:
```
Add Kafka consumer configuration for high-throughput logging
Fix memory leak in log aggregation module
Update documentation for deployment guide
```

## Roles and Responsibilities

- **Triager**: Reviews and labels new issues, requests missing details, deduplicates, and assigns initial priority.
- **Maintainer**: Reviews and merges PRs, ensures tests/docs/quality gates, manages releases, and enforces branch protection.

These roles may be held by the same person in small teams but must be documented for accountability.

## Review Process

- All submissions require review and approval
- Reviewers may request changes
- Address feedback promptly
- Be respectful and constructive in discussions

Thank you for contributing to logwise! 🎉

