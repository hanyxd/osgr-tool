# osgr - Instagram OSINT Reconnaissance Tool

Advanced Instagram profile analysis, story tracking, post analytics, and data export for OSINT investigations.

## Features

- **Profile Intelligence** — Username, bio, followers/following, verification, business category, profile pictures
- **Post Analytics** — Likes, comments, hashtags, mentions, locations, engagement rates, posting patterns
- **Story Tracking** — Active 24h stories + saved highlights with full metadata
- **Likes Analysis** — Who liked specific posts (requires following target account)
- **Follower/Following Sampling** — Export follower/following lists with metadata
- **Comments Extraction** — Recent comments from posts
- **Multiple Export Formats** — JSON, CSV, Markdown reports

## Installation

```bash
# Requires Python 3.8+ and instaloader
pip install instaloader browser-cookie3

# Clone and install
git clone https://github.com/hanyxd/osgr-tool
cd osgr-tool
chmod +x osgr
sudo ln -s $(pwd)/osgr /usr/local/bin/osgr
```

## Authentication

Login to Instagram in **Chrome** as your investigation account (burner recommended), then osgr will automatically use those cookies:

```bash
# 1. Open Chrome → instagram.com → login
# 2. Run osgr (it auto-detects Chrome session)
osgr target_username
```

## Usage

```bash
osgr <username> [options]
```

| Option | Description |
|--------|-------------|
| `--stories` | Fetch active 24h stories |
| `--analytics` | Analyze last 30 posts (engagement, hashtags, timing) |
| `--posts N` | Number of posts to analyze (default: 30) |
| `--followers N` | Fetch N followers sample |
| `--following N` | Fetch N following sample |
| `--comments N` | Fetch comments from last N posts |
| `--likes N` | Get likers for last N posts (requires following target) |
| `--full` | Everything (posts + stories + highlights + followers + following + comments + likes) |
| `--save` | Save JSON + CSV + Markdown report |
| `--csv` | Save CSV exports only |
| `--json` | Output raw JSON to stdout |
| `--verbose, -v` | Verbose output with recent posts |
| `--debug` | Debug mode |

## Examples

```bash
# Basic profile + highlights
osgr nasa

# Profile + active stories
osgr nasa --stories

# Full analysis with exports
osgr nasa --analytics --stories --save

# Deep investigation
osgr target --full --save

# Get 100 followers + 50 following
osgr target --followers 100 --following 50 --csv

# Get likers for last 5 posts (must follow target first)
osgr target --likes 5 --save

# Raw JSON for piping
osgr target --json | jq '.profile.followers'
```

## Output Files

Saved to `~/osgr_output/`:
- `username_TIMESTAMP.json` — Complete structured data
- `username_posts.csv` — Posts with all metadata
- `username_highlights.csv` — Story highlights flattened
- `username_followers.csv` — Follower sample
- `username_report.md` — Human-readable Markdown report

## Requirements

- Python 3.8+
- `instaloader` — `pip install instaloader`
- `browser-cookie3` — `pip install browser-cookie3`
- Chrome browser (for session cookies)
- Instagram account (use burner!)

## Legal & Ethics

- **Educational/OSINT purposes only**
- Use burner accounts — Instagram may challenge/lock automation
- Respect rate limits and terms of service
- Don't use on private accounts without permission
- Check local laws regarding data collection

## License

MIT