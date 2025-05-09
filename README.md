# MobApp-Storage Inspector

**Make mobile app storage analysis effortless.**

An all-in-one, cross-platform application that enables users to inspect mobile application storage files, databases, and more. It runs on Windows, macOS, and Linux. Ideal for developers, security testers, and forensics experts on Windows, macOS, and Linux.

<img width="1312" alt="image" src="https://github.com/user-attachments/assets/ee6c68ef-0602-45be-90fe-2e2e4dd16d5f" />

## Table of Contents
- [Why Use It](#why-use-it)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)


## Why Use It

Manual mobile app storage inspection is tedious and error-prone:
- **String Extraction**: `strings` generates noisy, hard-to-parse output.
- **SQL Commands**: `sqlite3` requires complex syntax and error handling.
- **Grep Searches**: Crafting regex for `grep` is slow and yields overwhelming results.
- **Objection Limits**: Excels in runtime analysis but lacks robust static file inspection.
- **Excel Struggles**: Poor handling of large datasets, requiring complex schemas and filters.
- **Relational Databases**: Demand upfront schema definitions, slowing analysis.

**MobApp-Storage Inspector** solves these:
- **Auto Strings**: Instantly extracts readable text from binaries.
- **No-SQL Database Access**: Browse SQLite tables without writing queries.
- **Fast Search**: Find files or content using plain English, no regex needed.
- **Intuitive UI**: Replaces command-line complexity with a graphical interface.

## Features

| Feature | Advantage |
|---------|----------|
| File Tree | Effortless directory navigation. |
| SQLite Viewer | Browse and query databases without SQL. |
| Text Viewer | Syntax highlighting for XML, JSON, and more. |
| Image Viewer | Zoom and inspect metadata. |
| Binary Analysis | Hex view with auto-extracted strings. |
| Fast Search | Quickly locate files or content. |
| Flexible Layout | Multiple column and text-based data views. |
| Custom Window Titles | Personalized titles for each window. |
| Preferences Window | Ergonomic settings with minimal effort. |
| Custom Filters | User-defined data post-processing. |
| Light/Dark Modes | Comfortable viewing for long sessions. |
| Cross-Platform | Compatible with Windows, macOS, Linux. |
| Performance | Minimal system resource usage for efficiency. |

## Installation

### Prerequisites
- Java 17+ (verify with `java -version`). Install from [Adoptium](https://adoptium.net/) if needed.

### Quick Start
1. Download the latest JAR from [Releases](https://github.com/thecybersandeep/mobapp-storage-inspector/releases).
2. Run:
   ```bash
   java -jar MobApp-Storage-Inspector.jar
   ```

### Build from Source
1. Clone:
   ```bash
   git clone ttps://github.com/thecybersandeep/mobapp-storage-inspector
   cd MobApp-Storage-Inspector
   ```
2. Build:
   ```bash
   mvn clean package
   ```
3. Run:
   ```bash
   java -jar output/MobApp-Storage-Inspector.jar
   ```

## Usage

1. **Browse**: Navigate app storage using the left tree view.
2. **Inspect**: Click files to view:
   - **SQLite Databases**: Browse tables, run queries.
   - **Text Files**: Syntax-highlighted display.
   - **Images**: Zoom, view metadata.
   - **Binaries**: Hex view with extracted strings.
3. **Search**: Use the search bar to find files or content.

**Supported Files**:
- Databases: `.db`, `.sqlite`, `.sqlite3`
- Text: `.xml`, `.json`, `.txt`, `.plist`, etc.
- Images: `.jpg`, `.png`, `.gif`, etc.
- Binaries: Any file with hex view.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Java Error | Install Java 17+ (`java -version`). |
| Permission Denied | Run as admin or check folder permissions. |
| UI Scaling | Adjust system display settings. |

## Contributing

Love the tool? Help make it better!
- **Report Issues**: File bugs or ideas
- **Submit PRs**: Fork, code, and send pull requests.
Open an [Issue](https://github.com/thecybersandeep/mobapp-storage-inspector/issues).

Thought for 4 seconds


For writeup on tool uses see here: [Extract & Analyze Mobile App Storage in Seconds](https://thecybersandeep.medium.com/extract-analyze-mobile-app-storage-in-seconds-8e0585c0ff57)

