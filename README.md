# osgr - Instagram OSINT Reconnaissance Tool

![GitHub stars](https://img.shields.io/github/stars/hanyxd/osgr-tool?style=social)
![GitHub forks](https://img.shields.io/github/forks/hanyxd/osgr-tool?style=social)
![GitHub issues](https://img.shields.io/github/issues/hanyxd/osgr-tool)
![GitHub license](https://img.shields.io/github/license/hanyxd/osgr-tool)
![Python](https://img.shields.io/badge/python-3.8+-blue)
![Platform](https://img.shields.io/badge/platform-linux%20%7C%20windows%20%7C%20macos-lightgrey)

Advanced Instagram profile analysis, story tracking, post analytics, and data export for OSINT investigations.

## ⚡ Quick Start (30 seconds)

```bash
# 1. Install dependencies
pip install instaloader browser-cookie3

# 2. Clone & install
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr

# 3. Login to Instagram in Chrome (use burner account!)
#    Open Chrome → instagram.com → login

# 4. Run your first scan
osgr nasa --analytics --stories --save
```

That's it! Results saved to `~/osgr_output/`

---

## 📦 Cross-Platform Installation

### Linux (All Distributions)

#### Debian/Ubuntu/Mint/Pop!_OS/Kali/Parrot
```bash
# System packages
sudo apt update && sudo apt install -y python3 python3-pip python3-venv git chromium-browser

# Python dependencies
pip3 install instaloader browser-cookie3

# Clone & install
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

#### Fedora/RHEL/CentOS/AlmaLinux/Rocky
```bash
sudo dnf install -y python3 python3-pip python3-virtualenv git chromium
pip3 install instaloader browser-cookie3
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

#### Arch/Manjaro/EndeavourOS/Garuda
```bash
sudo pacman -S python python-pip git chromium
pip install instaloader browser-cookie3
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

#### openSUSE (Tumbleweed/Leap)
```bash
sudo zypper install python3 python3-pip git chromium
pip3 install instaloader browser-cookie3
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

#### Alpine Linux
```bash
apk add python3 py3-pip git chromium
pip3 install instaloader browser-cookie3
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

#### NixOS
```bash
# Add to configuration.nix or use nix-shell
nix-shell -p python3 python3Packages.instaloader python3Packages.browser-cookie3 chromium git
# Or permanent:
# environment.systemPackages = with pkgs; [ python3 python3Packages.instaloader python3Packages.browser-cookie3 chromium git ];
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

#### Using Virtual Environment (Recommended for All Distros)
```bash
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
python3 -m venv venv
source venv/bin/activate
pip install instaloader browser-cookie3
# Run with venv python
./osgr nasa --analytics --stories --save
```

---

### Windows (10/11)

#### Option A: Native Windows (PowerShell)

**Prerequisites:**
1. Install [Python 3.11+](https://python.org/downloads) ✓ "Add Python to PATH"
2. Install [Git for Windows](https://git-scm.com/download/win)
3. Install [Chrome](https://www.google.com/chrome/) (required for cookies)

```powershell
# Open PowerShell as Administrator

# 1. Install Python packages
pip install instaloader browser-cookie3

# 2. Clone repository
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool

# 3. Run with python (no chmod needed on Windows)
python osgr nasa --analytics --stories --save

# 4. Optional: Add to PATH for global 'osgr' command
# Copy osgr to a folder in your PATH, or create a batch wrapper:
echo python "%~dp0osgr" %* > C:\Windows\osgr.bat
# Now you can run: osgr nasa --analytics
```

#### Option B: WSL2 (Windows Subsystem for Linux) — **Recommended**

WSL2 gives you full Linux compatibility:

```powershell
# 1. Enable WSL2 (run in PowerShell Admin)
wsl --install
# Restart computer, then open Ubuntu from Start menu

# 2. Inside Ubuntu terminal, follow Linux (Debian/Ubuntu) instructions above:
sudo apt update && sudo apt install -y python3 python3-pip python3-venv git chromium-browser
pip3 install instaloader browser-cookie3
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr

# 3. Login to Chrome INSIDE WSL (or use Windows Chrome with cookie sharing)
# For Windows Chrome cookie access, install browser-cookie3 with Windows support:
pip install browser-cookie3[windows]
```

**WSL2 + Windows Chrome Cookie Access:**
```bash
# In WSL, point to Windows Chrome cookie database
export CHROME_COOKIE_PATH="/mnt/c/Users/YOUR_USERNAME/AppData/Local/Google/Chrome/User Data/Default/Network/Cookies"
# osgr will auto-detect if browser-cookie3[windows] is installed
```

---

### macOS (Intel & Apple Silicon)

```bash
# 1. Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Install dependencies
brew install python3 git chromium

# 3. Python packages
pip3 install instaloader browser-cookie3

# 4. Clone & install
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr

# 5. Login to Chrome on macOS, then run
osgr nasa --analytics --stories --save
```

---

## 🐳 Docker (Any Platform)

```bash
# Build image
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool

# Create Dockerfile
cat > Dockerfile << 'EOF'
FROM python:3.11-slim
RUN apt-get update && apt-get install -y chromium git && rm -rf /var/lib/apt/lists/*
RUN pip install instaloader browser-cookie3
WORKDIR /app
COPY . .
RUN chmod +x osgr
ENTRYPOINT ["python", "osgr"]
EOF

docker build -t osgr .
# Run (need to mount Chrome cookie dir)
docker run -it --rm \
  -v ~/osgr_output:/root/osgr_output \
  -v ~/.config/chromium:/root/.config/chromium:ro \
  osgr nasa --analytics --stories --save
```

---

## 🔧 Browser Cookie Configuration

| Browser | Linux Path | Windows Path | macOS Path |
|---------|------------|--------------|------------|
| Chrome | `~/.config/google-chrome/Default/Cookies` | `%LOCALAPPDATA%\Google\Chrome\User Data\Default\Network\Cookies` | `~/Library/Application Support/Google/Chrome/Default/Cookies` |
| Chromium | `~/.config/chromium/Default/Cookies` | `%LOCALAPPDATA%\Chromium\User Data\Default\Network\Cookies` | `~/Library/Application Support/Chromium/Default/Cookies` |
| Brave | `~/.config/BraveSoftware/Brave-Browser/Default/Cookies` | `%LOCALAPPDATA%\BraveSoftware\Brave-Browser\User Data\Default\Network\Cookies` | `~/Library/Application Support/BraveSoftware/Brave-Browser/Default/Cookies` |
| Edge | `~/.config/microsoft-edge/Default/Cookies` | `%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Network\Cookies` | `~/Library/Application Support/Microsoft Edge/Default/Cookies` |

**Auto-detection works for Chrome/Chromium/Brave/Edge on all platforms.**

For non-default profiles, set environment variable:
```bash
export BROWSER_COOKIE3_CHROME_PROFILE="Profile 2"
osgr nasa --analytics
```

---

## 🚀 Running Examples

```bash
# Quick profile scan
osgr nasa

# Full OSINT package with exports
osgr target_username --full --save

# Just analytics + stories
osgr nasa --analytics --stories --save

# Follower network mapping
osgr target --followers 200 --following 200 --csv

# Post engagement deep-dive
osgr target --posts 50 --analytics --verbose

# Likers analysis (MUST follow target first in Chrome!)
osgr target --likes 10 --save

# JSON for scripting
osgr target --json | jq '.profile | {username, followers, following, posts_count}'
```

---

## 📁 Output Structure

```
~/osgr_output/
├── target_20260711_143022.json      # Full JSON data
├── target_posts.csv                  # Posts (shortcode, likes, comments, hashtags, etc.)
├── target_highlights.csv             # Story highlights
├── target_followers.csv              # Follower sample
├── target_following.csv              # Following sample
├── target_likes.csv                  # Post likers
└── target_report.md                  # Human-readable report
```

---

## ⚠️ Important Notes

| Platform | Notes |
|----------|-------|
| **All** | Use **burner Instagram account** — automation triggers challenges |
| **Windows** | Run PowerShell as Admin for `ln -s` equivalent; WSL2 preferred |
| **Linux** | Chrome/Chromium must be installed for cookie extraction |
| **macOS** | Grant Terminal "Full Disk Access" in System Preferences → Security for cookie reading |
| **Docker** | Mount host Chrome cookie directory for authentication |

---

## 📋 Requirements Summary

| Component | Linux | Windows | macOS |
|-----------|-------|---------|-------|
| Python | 3.8+ | 3.11+ | 3.8+ |
| pip packages | `instaloader`, `browser-cookie3` | Same | Same |
| Browser | Chrome/Chromium/Brave/Edge | Chrome/Edge | Chrome/Brave/Edge |
| Git | ✓ | ✓ | ✓ |
| Shell | bash/zsh/fish | PowerShell/WSL2 | bash/zsh |

---

## License

MIT — Use responsibly for educational/OSINT purposes only.