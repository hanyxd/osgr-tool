# OSGR - Instagram OSINT Reconnaissance Tool

Advanced Instagram profile analysis, story tracking, post analytics, and data export.

## Features

- **Profile Analysis** - Complete profile info (followers, bio, verification, business status, etc.)
- **Story Highlights** - Fetch all story highlights with items and URLs
- **Active Stories** - Current 24h stories with expiration times
- **Post Analytics** - Engagement rates, top hashtags, posting patterns, peak hours
- **Follower/Following Analysis** - Sample analysis with influence metrics
- **Comments Extraction** - Recent comments from posts
- **Multiple Export Formats** - JSON, CSV, Markdown reports

## Installation

```bash
# Requires Python 3.11+ and instaloader
pip install instaloader browser-cookie3

# Login to Instagram in Chrome first (as the session user)
# Then copy osgr to your PATH:
cp osgr /usr/local/bin/osgr
chmod +x /usr/local/bin/osgr
```

## Usage

```bash
osgr <username> [options]

Options:
  --analytics        Analyze recent posts (default: 30)
  --stories          Include active 24h stories
  --followers N      Sample N followers
  --following N      Sample N following
  --comments N       Comments from last N posts
  --full             Everything (posts, highlights, stories, followers, following, comments)
  --save             Save JSON + CSV + Markdown report
  --csv              Save CSV exports
  --json             Output raw JSON to stdout
  --verbose          Verbose output with recent posts

Examples:
  osgr nasa --analytics --stories --save
  osgr username --full --save
  osgr username --followers 100 --csv
  osgr username --json
```

## Output Files

Saved to `/home/user/osgr_output/`:
- `username_TIMESTAMP.json` - Complete structured data
- `username_posts.csv` - Post analytics
- `username_highlights.csv` - Story highlights
- `username_followers.csv` - Followers sample
- `username_report.md` - Human-readable markdown report
