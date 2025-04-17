# Secure File Transfer Application

A multithreaded TCP/IP file transfer application with robust encryption, checksums, and secure messaging capabilities.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Technical Architecture](#technical-architecture)
  - [Message Protocol](#message-protocol)
  - [File Transfer Mechanism](#file-transfer-mechanism)
  - [Encryption Systems](#encryption-systems)
  - [Multi-threading Architecture](#multi-threading-architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [Usage](#usage)
- [Performance Optimizations](#performance-optimizations)
- [Security Considerations](#security-considerations)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)
- [Acknowledgments](#acknowledgments)

## Overview

This secure file transfer application provides end-to-end encrypted file transfers using TCP/IP protocol. It's built with Java and JavaFX, offering a modern graphical user interface while maintaining high performance through extensive multi-threading optimizations.

## Features

- Secure user authentication (login/register)
- End-to-end encrypted file transfers
- Real-time transfer progress monitoring
- HMAC file integrity verification
- Multithreaded processing for optimal performance
- Automatic retry mechanism for failed transfers
- Cross-platform compatibility

## Technical Architecture

### Message Protocol

The application uses a binary message protocol with the following structure:
```
[Command Byte][Size (4 bytes)][Encrypted Payload]
```
- **Command Byte**: A single byte that identifies the type of message.
- **Size**: A 4-byte integer representing the length of the payload.
- **Encrypted Payload**: The actual data, encrypted using a session key.

#### Session Security
- The session key is established during an initial handshake.
- Messages are encrypted using XOR with a rolling session key.
- The session key is rotated periodically to enhance security.

### File Transfer Mechanism

#### Chunked Transfer
- Files are divided into configurable chunks (default size: 64KB).
- Each chunk is encrypted individually and transferred.
- A thread pool is used for parallel processing of chunks.
- Failed chunks are automatically retried.

### Encryption Systems

1. **DES Encryption**
   - Block size: 8 bytes.
   - Parallel block processing is implemented using a thread pool.
   - Key size: 8 bytes.
   - A custom DES implementation with optimizations is used.

2. **HMAC File Verification**
   - Supports parallel chunk processing.
   - Multiple hashing algorithms are available (default: SHA-256).
   - Verification is managed by a thread pool.
   - Real-time progress tracking is provided.

### Multi-threading Architecture

1. **Network Layer**
   - A dedicated sender thread handles outgoing messages.
   - A message collector thread processes incoming data.
   - A non-blocking message queue system is used.

2. **File Processing**
   - A dynamic thread pool is sized based on the number of CPU cores.
   - Large files are processed in parallel chunks.
   - Progress tracking is synchronized.
   - Stream processing is memory-efficient.

3. **Encryption Layer**
   - Block encryption and decryption are performed in parallel.
   - Key operations are managed by a thread pool.
   - Checksum calculations are performed concurrently.

## Getting Started

### Prerequisites

- JDK 17 or newer
- Maven 3.6 or newer
- TCP/IP network connection

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/N-D-Duy/attt-client.git
   cd secure-file-transfer
   ```

2. Build the project:
   ```bash
   mvn clean package
   ```

### Running the Application

#### Using Maven:
```bash
mvn javafx:run
```

#### Using JAR:
```bash
java -jar target/secure-file-transfer.jar
```

## Usage

1. **Launch the application**
2. **Login or register a new account**
   - Enter credentials in the login form
   - For new users, click "Register" and create account
3. **Main interface**
   - Online users are displayed on the left panel
   - Select a user to initiate file transfer
4. **Sending files**
   - Click "Select File" to choose a file
   - Click "Encrypt" to prepare the file
   - Click "Send" to transfer to selected user
5. **Receiving files**
   - Accept or reject incoming file requests
   - Monitor progress in transfer dialog
   - Files are automatically decrypted on completion

## Performance Optimizations

The application is optimized for performance through:
- Parallel file chunk processing
- Dynamic thread pool sizing
- Non-blocking I/O operations
- Memory-efficient stream handling
- Buffered data processing

## Security Considerations

- All file transfers are encrypted using DES
- HMAC verification ensures file integrity
- Session keys are unique for each connection
- Secure random number generation for keys
- Protection against replay attacks

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Commit your changes (`git commit -m 'Add some amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

### Coding Standards

- Follow Java coding conventions
- Write unit tests for new features
- Document public APIs
- Keep methods focused and concise
- Use meaningful variable and method names

## Contact

Project Maintainer - [Duy Nguyen](mailto:nguyenducduypc160903@gmail.com)

## Acknowledgments

- [JavaFX](https://openjfx.io/) for the UI framework
- [Apache Maven](https://maven.apache.org/) for build automation
- All contributors who have helped shape this project

