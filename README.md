# osgr - Instagram OSINT Reconnaissance Tool

[![GitHub Release](https://img.shields.io/github/v/release/hanyxd/osgr?style=flat-square&color=8a2be2)](https://github.com/hanyxd/osgr/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/hanyxd/osgr/total?style=flat-square&color=8a2be2)](https://github.com/hanyxd/osgr/releases)
[![GitHub Issues](https://img.shields.io/github/issues/hanyxd/osgr?style=flat-square&color=8a2be2)](https://github.com/hanyxd/osgr/issues)
[![License](https://img.shields.io/badge/license-MIT-8a2be2?style=flat-square)](LICENSE)
[![Python Version](https://img.shields.io/badge/python-3.11+-8a2be2?style=flat-square&logo=python&logoColor=white)](https://python.org)
[![Platform](https://img.shields.io/badge/platform-Linux%20%7C%20macOS%20%7C%20Windows-8a2be2?style=flat-square)](https://github.com/hanyxd/osgr)
[![Discord](https://img.shields.io/badge/Discord-Join%20Server-8a2be2?style=flat-square&logo=discord&logoColor=white)](https://discord.gg/yynpznJVkC)

> **🌐 Web Version Available:** [osgr.dev](https://osgr.dev) — Recommended for non-developers

---

## Overview

**osgr** (OSINT Instagram Reconnaissance) is a powerful command-line tool for Instagram open-source intelligence gathering. Built for security researchers, penetration testers, and OSINT investigators.

### Features

- 🔍 **Profile Enumeration** — Extract public profile metadata, bio, followers/following counts
- 📸 **Media Analysis** — Download and analyze posts, stories, reels, highlights
- 👥 **Social Graph Mapping** — Followers, following, mutual connections, interaction analysis
- 🔍 **OSINT Enrichment** — Email/phone extraction, linked accounts, metadata extraction
- 📊 **Export Formats** — JSON, CSV, HTML reports, GraphML for graph visualization
- 🔐 **Session Management** — Session persistence, cookie rotation, rate-limit handling
- 🔌 **Modular Architecture** — Plugin system for custom extractors and exporters

---

## Installation

### Quick Install (Linux/macOS)
```bash
curl -fsSL https://raw.githubusercontent.com/hanyxd/osgr/main/install.sh | bash
```

### From Source
```bash
git clone https://github.com/hanyxd/osgr.git
cd osgr
pip install -e .
```

### Requirements
- Python 3.11+
- Linux, macOS, or Windows (WSL2 recommended)
- Valid Instagram session (cookies/sessionid)

---

## Quick Start

```bash
# Interactive session setup
osgr auth login

# Profile enumeration
osgr profile target_username --output json

# Download all media
osgr media target_username --all --output ./downloads

# Follower analysis
osgr followers target_username --graph graphml --output network.graphml

# Full OSINT report
osgr recon target_username --full --report html --output report.html
```

---

## Documentation

| Topic | Link |
|-------|------|
| **Installation Guide** | [docs/installation.md](docs/installation.md) |
| **CLI Reference** | [docs/cli-reference.md](docs/cli-reference.md) |
| **Configuration** | [docs/configuration.md](docs/configuration.md) |
| **Plugin Development** | [docs/plugins.md](docs/plugins.md) |
| **API Reference** | [docs/api-reference.md](docs/api-reference.md) |

---

## Configuration

Configuration file: `~/.config/osgr/config.yaml`

```yaml
instagram:
  sessionid: "your_session_id"
  csrftoken: "your_csrf_token"
  user_agent: "Instagram 300.0.0.0.0 Android"
  
rate_limits:
  requests_per_minute: 30
  delay_range: [2, 5]
  
output:
  default_format: "json"
  default_dir: "~/osgr_output"
```

---

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Quick Contribution Guide

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/amazing-feature`
3. Make your changes with tests
4. Run linting: `make lint`
5. Submit a Pull Request

---

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

---

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

## Support

- **Issues:** [GitHub Issues](https://github.com/hanyxd/osgr/issues)
- **Discord:** [Join our server](https://discord.gg/yynpznJVkC)
- **Security:** Report vulnerabilities to security@osgr.dev

---

## Acknowledgments

Built with inspiration from [Osintgram](https://github.com/Datalux/Osintgram), [Instaloader](https://instaloader.github.io/), and the OSINT community.

---

<p align="center">
  <strong>Built for OSINT researchers, by OSINT researchers</strong>
</p>
