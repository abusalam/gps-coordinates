# Google Play Store App Listing Specifications

This directory contains the organized folder structure for Google Play Console store listing artifacts.

## Folder Hierarchy

- `text_metadata/`
  - `title.txt` — App Title (Max 30 characters)
  - `short_description.txt` — Short Description (Max 80 characters)
  - `full_description.txt` — Full Description (Max 4,000 characters)
- `graphics/`
  - `icon/` — App Icon (`icon_512x512.png`)
  - `feature_graphic/` — Feature Graphic (`feature_graphic_1024x500.png` or `.jpg`)
  - `screenshots/` — Phone & Tablet Screenshots (`screenshot_1.png`, `screenshot_2.png`, etc.)

---

## Google Play Asset Requirements

### 1. Text Assets
- **App Title**: 
  - Limit: Maximum 30 characters
  - Avoid keywords like "free", "best", "top", or promotional tags in the title.
- **Short Description**:
  - Limit: Maximum 80 characters
  - A concise summary highlighting the primary core feature or hook.
- **Full Description**:
  - Limit: Maximum 4,000 characters
  - Clear, structured overview of app features, functionality, and user benefits.

### 2. Graphical Assets
- **App Icon**:
  - Format: 32-bit PNG (with alpha)
  - Dimensions: 512 x 512 px
  - Max File Size: 1 MB
- **Feature Graphic**:
  - Format: JPEG or 24-bit PNG (no alpha)
  - Dimensions: 1024 x 500 px
  - Max File Size: 15 MB
  - Centered visual storytelling; avoid placing critical text/logos near the outer borders.
- **Phone Screenshots**:
  - Count: Minimum 2, maximum 8
  - Aspect Ratio: 16:9 or 9:16 (aspect ratio between 1:2 and 2:1)
  - Dimensions: Minimum 320 px, maximum 3840 px
  - Format: JPEG or 24-bit PNG (no alpha)
  - Max File Size: 8 MB per file
