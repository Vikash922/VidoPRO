---
title: UI Design System
version: 1.0
related: PRD.md, ARCHITECTURE.md
platform: Android
ui: Jetpack Compose + Material 3
theme: Dark-first
status: Initial Design System
---

# UI Design System

## 1. Purpose

This design system defines the visual language, UI structure, interaction rules, and design tokens for the Android video editing app.

The app should feel:

- Modern
- Dark
- Premium
- Smooth
- Creator-focused
- Beginner-friendly
- Fast and responsive

The design must be original. It can be inspired by modern mobile video editors, but it must not copy existing app assets, branding, or exact UI layouts.

---

## 2. Design Vision

The UI should balance two goals:

### Simple editing

- Easy for beginners
- Clear actions
- Low visual noise
- Fast access to common tools

### Advanced creation

- Timeline clarity
- Layer visibility
- Precise controls
- Room for future keyframes and motion graphics

One-line design direction:

> A dark, minimal, creator-first editing surface with smooth motion and clear controls.

---

## 3. Core Design Principles

### 3.1 Content first

The preview and timeline are the main focus. Tools should not unnecessarily cover content.

### 3.2 Dark editing environment

Video editing apps feel better in dark mode because media preview stands out and eye strain reduces.

### 3.3 Thumb-friendly controls

Primary actions should be reachable with the thumb, especially bottom tools.

### 3.4 Low visual noise

Use subtle borders, soft surfaces, and limited accent colors.

### 3.5 Clear selection state

The selected clip, selected tool, and active playhead must always be visually obvious.

### 3.6 Smooth feedback

Every important action should have visual or haptic feedback.

### 3.7 Progressive disclosure

Beginners see simple tools first. Advanced tools appear only when relevant.

### 3.8 Consistent components

Buttons, sheets, sliders, cards, and dialogs should behave consistently across the app.

---

## 4. Visual Identity

### Mood

- Dark graphite background
- Soft elevated surfaces
- Violet/purple primary accent
- Cyan secondary accent for active states
- Red playhead
- Rounded clip cards
- Subtle shadows and borders

### Look

- Minimal text
- Large touch targets
- Rounded sheets
- Smooth transitions
- High contrast icons
- Clean timeline

---

## 5. Layout System

### Base grid

```text
Spacing unit: 4dp
Small: 8dp
Medium: 12dp
Large: 16dp
Extra large: 24dp
```

### Screen padding

```text
Screen horizontal padding: 16dp
Card padding: 12dp
Sheet content padding: 16dp
Timeline vertical padding: 8dp
```

### Touch target

Minimum touch target:

```text
48dp x 48dp
```

For timeline clip handles:

```text
Minimum visible handle width: 12dp
Minimum touch area: 24dp to 32dp
```

---

## 6. App Structure

### Global layout

```text
Top App Bar
Preview Area
Timeline Area
Bottom Tool Bar
Modal Bottom Sheets
```

### Editor layout priority

```text
1. Preview should be clearly visible.
2. Timeline should be easily accessible.
3. Tools should open in bottom sheets.
4. Export should always be reachable.
```

### Editor screen zones

```text
┌──────────────────────────────┐
│ Top Bar                      │
├──────────────────────────────┤
│ Preview Player               │
├──────────────────────────────┤
│ Timeline                     │
├──────────────────────────────┤
│ Bottom Tools                 │
└──────────────────────────────┘
```

---

## 7. Color System

### 7.1 Base Dark Theme

```json
{
  "color": {
    "background": "#0B0B0F",
    "surface": "#121218",
    "surface_high": "#1A1A22",
    "surface_card": "#1E1E28",
    "surface_sheet": "#16161D",
    "outline": "#2A2A35",
    "outline_strong": "#3A3A48",
    "primary": "#7C5CFF",
    "primary_variant": "#9A7BFF",
    "on_primary": "#FFFFFF",
    "secondary": "#22D3EE",
    "on_secondary": "#04141A",
    "text_primary": "#F5F5F7",
    "text_secondary": "#A0A0AB",
    "text_disabled": "#6B6B76",
    "danger": "#FF4D4F",
    "success": "#22C55E",
    "warning": "#F59E0B"
  }
}
```

### 7.2 Editor-specific Colors

```json
{
  "editor": {
    "timeline_background": "#0E0E13",
    "timeline_ruler": "#6B6B76",
    "playhead": "#FF3B30",
    "playhead_glow": "#FF3B3066",
    "clip_video": "#3B82F6",
    "clip_image": "#8B5CF6",
    "clip_audio": "#22C55E",
    "clip_text": "#F59E0B",
    "clip_overlay": "#22D3EE",
    "clip_selected_border": "#7C5CFF",
    "clip_selected_fill": "#7C5CFF22",
    "snap_line": "#22D3EE",
    "transition_marker": "#9A7BFF"
  }
}
```

### 7.3 State Colors

```json
{
  "state": {
    "pressed": "#FFFFFF14",
    "hover": "#FFFFFF0A",
    "selected": "#7C5CFF22",
    "disabled": "#FFFFFF08",
    "error_background": "#FF4D4F1A",
    "success_background": "#22C55E1A",
    "warning_background": "#F59E0B1A"
  }
}
```

### 7.4 Color Usage Rules

- Primary color is used for main actions and selection.
- Secondary color is used for subtle active indicators, snap lines, and highlights.
- Danger color is used only for destructive actions.
- Do not use too many bright colors at once.
- Timeline clip colors should be distinguishable but not overly saturated.
- Text should maintain high contrast against background.

---

## 8. Typography

### 8.1 Type Scale

```json
{
  "typography": {
    "display": {
      "size": "22sp",
      "weight": "semibold",
      "lineHeight": "28sp"
    },
    "headline": {
      "size": "18sp",
      "weight": "semibold",
      "lineHeight": "24sp"
    },
    "title": {
      "size": "16sp",
      "weight": "medium",
      "lineHeight": "22sp"
    },
    "body": {
      "size": "14sp",
      "weight": "regular",
      "lineHeight": "20sp"
    },
    "caption": {
      "size": "12sp",
      "weight": "regular",
      "lineHeight": "16sp"
    },
    "timeline_time": {
      "size": "10sp",
      "weight": "medium",
      "lineHeight": "14sp"
    }
  }
}
```

### 8.2 Typography Rules

- Use semibold only for important headers.
- Use caption for metadata like duration and timestamp.
- Timeline time labels should be small but readable.
- Avoid more than 3 font weights in one screen.
- Do not use decorative fonts for body text.
- Allow system font scaling where possible.

---

## 9. Spacing System

```json
{
  "spacing": {
    "xxs": "2dp",
    "xs": "4dp",
    "sm": "8dp",
    "md": "12dp",
    "lg": "16dp",
    "xl": "24dp",
    "xxl": "32dp"
  }
}
```

### Usage

```text
Between icon and label: 8dp
Between list items: 12dp
Between sections: 24dp
Inside buttons: 12dp to 16dp
Between timeline tracks: 8dp
Between clips: 2dp to 4dp
```

---

## 10. Radius System

```json
{
  "radius": {
    "small": "8dp",
    "medium": "12dp",
    "large": "16dp",
    "sheet": "24dp",
    "clip": "10dp",
    "card": "16dp",
    "button": "999dp"
  }
}
```

### Usage

```text
Buttons: pill radius or 12dp
Cards: 16dp
Sheets: 24dp top corners
Timeline clips: 10dp
Dialogs: 20dp to 24dp
Inputs: 12dp
```

---

## 11. Elevation and Surfaces

The app should mostly use subtle elevation instead of heavy shadows.

```json
{
  "elevation": {
    "none": "0dp",
    "low": "2dp",
    "medium": "6dp",
    "high": "12dp",
    "sheet": "16dp"
  }
}
```

Rules:

- Dark UI should avoid strong black shadows.
- Use surface color differences more than shadows.
- Bottom sheets can have stronger elevation.
- Timeline should remain flat and focused.
- Selected items should use border/glow instead of heavy elevation.

---

## 12. Iconography

### Icon style

- Rounded
- Outlined by default
- 24dp standard size
- 2dp stroke
- High contrast
- Consistent corner radius

### Icon sizes

```json
{
  "icon": {
    "small": "16dp",
    "standard": "24dp",
    "large": "28dp",
    "toolbar": "24dp",
    "empty_state": "48dp"
  }
}
```

### Icon usage

```text
Top bar icons: 24dp
Bottom tool icons: 24dp
Small metadata icons: 16dp
Empty state icons: 48dp
```

Rules:

- Do not mix too many icon styles.
- Keep icons simple and recognizable.
- Use text labels under bottom tools where useful.
- Destructive icons should use danger color only when active.

---

## 13. Motion System

### 13.1 Duration Tokens

```json
{
  "motion": {
    "fast": "120ms",
    "normal": "200ms",
    "slow": "300ms",
    "sheet_open": "250ms",
    "sheet_close": "200ms",
    "screen_transition": "250ms",
    "selection": "120ms",
    "toast": "150ms"
  }
}
```

### 13.2 Easing Tokens

```json
{
  "easing": {
    "standard": "cubic-bezier(0.4, 0.0, 0.2, 1)",
    "decelerate": "cubic-bezier(0.0, 0.0, 0.2, 1)",
    "accelerate": "cubic-bezier(0.4, 0.0, 1, 1)",
    "spring_drag": "spring(dampingRatio = 0.8, stiffness = 300)",
    "spring_sheet": "spring(dampingRatio = 0.85, stiffness = 400)"
  }
}
```

### 13.3 Motion Rules

- Selection feedback should be fast.
- Bottom sheets should feel smooth but not slow.
- Avoid long animations that block user action.
- Drag interactions should use springs.
- Timeline scroll should feel direct and responsive.
- Avoid animating heavy bitmaps or blur effects.
- Respect reduce motion accessibility where possible.

---

## 14. Animation Categories

### Micro-interactions

```text
Clip selection
Button press
Tool switch
Play/pause toggle
Undo/redo state
Export button loading
```

### Transitions

```text
Screen navigation
Bottom sheet open/close
Dialog appearance
Tool panel switching
```

### Editor feedback

```text
Snap indicator
Clip trim handle activation
Playhead scrubbing
Timeline zoom
Export progress
Success checkmark
```
---

## 15. Component System

### 15.1 Buttons

#### Primary Button

Usage:

- Create project
- Export
- Add media
- Confirm action

Style:

```json
{
  "primary_button": {
    "background": "#7C5CFF",
    "text_color": "#FFFFFF",
    "height": "48dp",
    "radius": "999dp",
    "padding_horizontal": "20dp",
    "text_size": "14sp",
    "weight": "medium",
    "disabled_background": "#2A2A35",
    "disabled_text": "#6B6B76"
  }
}
```

#### Secondary Button

Usage:

- Cancel
- Alternative actions
- Less important actions

Style:

```json
{
  "secondary_button": {
    "background": "#1E1E28",
    "border": "#2A2A35",
    "text_color": "#F5F5F7",
    "height": "48dp",
    "radius": "999dp"
  }
}
```

#### Danger Button

Usage:

- Delete project
- Remove clip
- Clear cache

Style:

```json
{
  "danger_button": {
    "background": "#FF4D4F1A",
    "border": "#FF4D4F33",
    "text_color": "#FF4D4F",
    "height": "48dp",
    "radius": "999dp"
  }
}
```

Rules:

- Only one primary button per screen where possible.
- Export button should be visually strong.
- Destructive actions should require confirmation.
- Disabled state must be clearly visible.

---

### 15.2 Icon Buttons

Usage:

- Back
- Undo
- Redo
- Close
- More options
- Play/pause

Style:

```json
{
  "icon_button": {
    "size": "48dp",
    "icon_size": "24dp",
    "background": "transparent",
    "pressed_background": "#FFFFFF14",
    "selected_background": "#7C5CFF22",
    "radius": "12dp"
  }
}
```

Rules:

- Icon buttons must have minimum 48dp touch area.
- Active icon can use primary color.
- Disabled icon should reduce opacity.

---

### 15.3 Cards

#### Project Card

Content:

- Thumbnail
- Project name
- Duration
- Last edited
- Overflow menu

Style:

```json
{
  "project_card": {
    "background": "#1E1E28",
    "radius": "16dp",
    "border": "#2A2A35",
    "thumbnail_radius": "12dp",
    "padding": "12dp",
    "pressed_state": "#FFFFFF0A"
  }
}
```

Rules:

- Thumbnail should use 16:9 or project aspect ratio.
- Keep metadata subtle.
- Overflow menu should include rename, duplicate, delete.

---

### 15.4 Clip Card

Clip cards appear inside timeline.

Types:

- Video clip
- Image clip
- Audio clip
- Text clip
- Overlay clip

Style:

```json
{
  "clip_card": {
    "height": "56dp",
    "radius": "10dp",
    "video_background": "#3B82F6",
    "image_background": "#8B5CF6",
    "audio_background": "#22C55E",
    "text_background": "#F59E0B",
    "overlay_background": "#22D3EE",
    "selected_border": "#7C5CFF",
    "selected_glow": "#7C5CFF33",
    "handle_width": "12dp",
    "handle_touch_area": "24dp"
  }
}
```

Rules:

- Clip color should indicate clip type.
- Selected clip should have clear border.
- Trim handles should appear only for selected clip.
- Audio clips should show waveform when available.
- Text clips should show short label.
- Video clips should show thumbnails.

---

### 15.5 Chips

Usage:

- Aspect ratio selection
- Filter categories
- Effect categories
- Export presets

Style:

```json
{
  "chip": {
    "background": "#1E1E28",
    "selected_background": "#7C5CFF22",
    "border": "#2A2A35",
    "selected_border": "#7C5CFF",
    "height": "36dp",
    "radius": "999dp",
    "text_size": "12sp"
  }
}
```

---

### 15.6 Sliders

Usage:

- Brightness
- Contrast
- Saturation
- Volume
- Speed
- Effect intensity

Style:

```json
{
  "slider": {
    "track_height": "4dp",
    "thumb_size": "20dp",
    "active_track": "#7C5CFF",
    "inactive_track": "#2A2A35",
    "thumb_color": "#FFFFFF",
    "value_label": true
  }
}
```

Rules:

- Slider value should update preview live.
- Show numeric value where precision matters.
- Use haptic tick for important increments.
- Double tap label can reset value.

---

### 15.7 Text Inputs

Usage:

- Project name
- Text layer content
- Export filename

Style:

```json
{
  "text_input": {
    "background": "#121218",
    "border": "#2A2A35",
    "focus_border": "#7C5CFF",
    "radius": "12dp",
    "height": "52dp",
    "text_size": "14sp"
  }
}
```

Rules:

- Use clear labels.
- Show inline errors.
- Avoid full-screen keyboard covering important controls.

---

### 15.8 Dialogs

Usage:

- Delete confirmation
- Exit editor confirmation
- Export settings warning

Style:

```json
{
  "dialog": {
    "background": "#16161D",
    "radius": "20dp",
    "padding": "20dp",
    "title_size": "16sp",
    "body_size": "14sp"
  }
}
```

Rules:

- Destructive dialogs should use danger button.
- Keep dialog text short.
- Do not use dialogs for minor feedback.

---

### 15.9 Bottom Sheets

Usage:

- Tools
- Filters
- Effects
- Audio
- Text styling
- Export settings

Style:

```json
{
  "bottom_sheet": {
    "background": "#16161D",
    "top_radius": "24dp",
    "drag_handle": true,
    "min_height": "220dp",
    "max_height": "70% screen height",
    "elevation": "16dp"
  }
}
```

Rules:

- Sheet should not fully hide preview unless necessary.
- Drag handle should be visible.
- Sheet should close on outside tap if non-critical.
- Tool sheets should preserve last scroll position.

---

### 15.10 Snackbars and Toasts

Usage:

- Undo action
- Clip deleted
- Export started
- Minor errors

Style:

```json
{
  "snackbar": {
    "background": "#1A1A22",
    "text_color": "#F5F5F7",
    "action_color": "#7C5CFF",
    "radius": "12dp",
    "margin": "16dp"
  }
}
```

Rules:

- Use snackbar with undo for delete actions.
- Do not block timeline with permanent snackbars.
- Use toast only for very short feedback.

---

### 15.11 Loading Components

#### Shimmer

Usage:

- Home project list
- Media picker thumbnails
- Timeline thumbnails

#### Spinner

Usage:

- Export preparing
- Media import processing

#### Progress bar

Usage:

- Export progress
- Background generation

Rules:

- Prefer shimmer for list loading.
- Use linear progress for export.
- Do not show loading if action takes less than 300ms.

---

## 16. Home Screen Design

### Structure

```text
┌──────────────────────────────┐
│ App Logo / Title             │
├──────────────────────────────┤
│ New Project Button           │
├──────────────────────────────┤
│ Recent Projects              │
│ [Project Card]               │
│ [Project Card]               │
│ [Project Card]               │
└──────────────────────────────┘
```

### Layout rules

- New Project button should be primary.
- Project cards in grid:
  - 2 columns on compact phones
  - 3 columns on larger screens
- Empty state should be centered.
- Keep home screen simple and fast.

---

## 17. Editor Screen Design

### Structure

```text
┌──────────────────────────────┐
│ Back | Project Name | Export │
├──────────────────────────────┤
│                              │
│ Preview Player               │
│                              │
├──────────────────────────────┤
│ Timeline                     │
├──────────────────────────────┤
│ Tools                        │
└──────────────────────────────┘
```

### Top Bar

Elements:

- Back button
- Project name
- Undo
- Redo
- Export button

Rules:

- Undo/redo should be disabled visually when unavailable.
- Export button should be visually primary.
- Project name can be tappable to rename.

### Preview Area

Elements:

- Video surface
- Play/pause button
- Current time
- Total duration

Rules:

- Preview should maintain project aspect ratio.
- Controls should overlay lightly and auto-hide during playback.
- Selected layer handles may appear on preview later.

### Bottom Tool Panel

Tools:

```text
Edit
Audio
Text
Effects
Filters
Speed
Overlay
Keyframe
```

Rules:

- Icons with small labels.
- Selected tool highlighted.
- Horizontal scroll if too many tools.
- Tapping tool opens relevant bottom sheet.

---

## 18. Timeline Visual Design

### Timeline Structure

```text
Time Ruler
Track 1: Video/Image clips
Track 2: Overlay/Text clips
Track 3: Audio clips
Playhead overlay
```

### Visual rules

- Timeline background should be darker than main surface.
- Clips should be horizontally scrollable.
- Playhead should be bright red and always visible.
- Snap lines should appear only when near snap point.
- Selected clip should have border and slight glow.
- Trim handles should appear only on selected clip.
- Timeline should show time labels based on zoom level.

### Timeline dimensions

```json
{
  "timeline": {
    "track_height": "56dp",
    "track_spacing": "8dp",
    "ruler_height": "24dp",
    "clip_radius": "10dp",
    "playhead_width": "2dp",
    "playhead_handle_size": "12dp",
    "minimum_clip_width": "24dp"
  }
}
```

### Timeline states

```text
Default
Selected
Dragging
Trimming
Snapping
Playing
Disabled
Error/missing media
```

### Missing media state

If media file is missing:

- Show warning icon
- Use muted background
- Show message on tap
- Do not crash timeline

---

## 19. Tool Panels and Sheets

### Edit Sheet

Actions:

- Split
- Delete
- Duplicate
- Speed
- Rotate
- Crop later

### Audio Sheet

Actions:

- Add audio
- Volume
- Fade in
- Fade out
- Extract audio later

### Text Sheet

Actions:

- Add text
- Edit text
- Font
- Size
- Color
- Alignment
- Animation later

### Effects Sheet

Actions:

- Effect categories
- Effect presets
- Intensity slider
- Reset

### Filters Sheet

Actions:

- Filter presets
- Intensity slider
- Compare press-and-hold

Rules:

- Sheet should show clear title.
- Use horizontal lists for presets.
- Use sliders for numeric values.
- Keep reset visible when value changes.

---

## 20. Empty, Loading, and Error States

### Empty Home

Title:

```text
Create your first project
```

Subtitle:

```text
Tap New Project to start editing.
```

Button:

```text
New Project
```

### Empty Timeline

Title:

```text
No clips yet
```

Subtitle:

```text
Add video or images to start editing.
```

### Loading

Use:

- Shimmer for project list
- Spinner for import
- Linear progress for export

### Error

Common messages:

```text
This file could not be imported.
This format is not supported yet.
Export failed. Please try again.
Not enough storage to export video.
Permission needed to access media.
```

Rules:

- Error messages should be friendly.
- Provide action where possible.
- Do not show technical stack trace.

---

## 21. Gesture System

### Global gestures

```text
Tap: select/open
Long press: multi-select or preview
Drag: move/scrub
Pinch: zoom timeline
Swipe down: close sheet
```

### Timeline gestures

```text
Tap clip: select clip
Drag clip: move clip
Drag edge: trim clip
Drag playhead: seek
Pinch: zoom timeline
Scroll: pan timeline
```

### Preview gestures

```text
Tap: show/hide controls
Double tap: toggle play/pause optional
Drag selected layer: move layer later
Pinch selected layer: scale layer later
Rotate selected layer: rotate layer later
```

Rules:

- Gestures should not conflict accidentally.
- Trim handles should have larger touch area than visual size.
- Drag should include slight haptic feedback when snapping.

---

## 22. Haptic Feedback

Use subtle haptics only.

Events:

```text
Clip selected: light
Clip snapped: light
Split action: medium
Delete action: medium
Export success: success pattern
Export failure: error pattern
Long press: light
```

Rules:

- Do not overuse haptics.
- Respect system haptic settings.
- Avoid haptic on every scroll frame.

---

## 23. Accessibility

### Touch

- Minimum touch target: 48dp
- Important controls should not be too close
- Timeline handles should have expanded touch bounds

### Text

- Maintain contrast ratio
- Support system font scaling where possible
- Avoid extremely small text except timeline ruler

### Icons

- Provide content descriptions
- Active states should not depend only on color
- Use labels where icons are unclear

### Motion

- Keep animations short
- Provide reduce motion support later
- Avoid flashing effects

### Color blindness

- Do not rely only on color to show selection
- Use border, glow, or label as additional indicator


---

24. Export Screen Design

Structure

┌──────────────────────────────┐
│ Back       Export Video      │
├──────────────────────────────┤
│                              │
│ Video Preview / Thumbnail    │
│                              │
├──────────────────────────────┤
│ Resolution   [ 1080p ▼ ]     │
│ Frame Rate   [ 30 fps ▼ ]    │
│ Quality      [ High ▼ ]      │
│ Estimated Size: ~45 MB       │
├──────────────────────────────┤
│ [       Export Video       ] │
└──────────────────────────────┘

Export Options

Support:

Resolution:
- 720p
- 1080p
- 1440p
- 4K (device supported)

Frame Rate:
- 24 fps
- 30 fps
- 60 fps

Quality:
- Low
- Medium
- High
- Custom / Advanced (optional)

Exporting State

When export is in progress, transition to a focused progress view.

┌──────────────────────────────┐
│         Exporting...         │
├──────────────────────────────┤
│                              │
│      [ Circular Progress ]   │
│           65%                │
│                              │
│ Rendering video...           │
│ Please keep the app open.    │
│                              │
├──────────────────────────────┤
│ [       Cancel Export      ] │
└──────────────────────────────┘

Success State

┌──────────────────────────────┐
│                              │
│      [ Success Checkmark ]   │
│                              │
│  Video Exported Successfully │
│       Saved to Gallery       │
│                              │
├──────────────────────────────┤
│ [ Share ] [ Open ] [ Done ]  │
└──────────────────────────────┘

Error State

┌──────────────────────────────┐
│                              │
│        [ Error Icon ]        │
│                              │
│      Export Failed           │
│ Something went wrong while   │
│ rendering your video.        │
│                              │
│ [ Retry ]        [ Cancel ]  │
└──────────────────────────────┘

Rules

- Export button must be the primary action.
- Export button must be disabled while export is running.
- Prevent accidental double-tap exports.
- Show estimated file size whenever possible.
- Show clear export progress.
- Cancel button must remain clearly visible during export.
- Export must run outside the UI thread.
- Never freeze or block the UI during rendering.
- Preserve project state if export is cancelled or fails.
- Save successfully exported videos to the appropriate gallery/media location.
- Show a clear success or error state.

---

25. Media Picker Design

Structure

┌──────────────────────────────┐
│ Close       Add Media        │
├──────────────────────────────┤
│ [ Videos ] [ Images ] [Audio]│
├──────────────────────────────┤
│ [Thumb] [Thumb] [Thumb]      │
│ [Thumb] [Thumb] [Thumb]      │
│ [Thumb] [Thumb] [Thumb]      │
├──────────────────────────────┤
│ Selected: 2 items            │
│ [       Add to Timeline    ] │
└──────────────────────────────┘

Media Categories

Videos
Images
Audio

Grid

- Use 3 or 4 columns depending on screen width.
- Use large, properly sized thumbnails.
- Maintain consistent spacing.
- Preserve aspect ratio.
- Use rounded corners.
- Avoid loading original full-resolution media into the grid.

Video Thumbnail

Show:

[ Thumbnail ]
    00:18

Duration badge should remain readable on dark media.

Selection State

Selected:

┌──────────────┐
│       ✓      │
│   Thumbnail  │
│              │
└──────────────┘

Rules:

- Show a clear selection checkmark.
- Dim unselected items slightly when multi-select is active.
- Selected items should have a visible primary-color border or overlay.
- Show selected item count.
- Disable "Add to Timeline" when nothing is selected.
- Preserve selection while scrolling.
- Use stable media IDs.

Permission State

If storage/media permission is denied:

┌──────────────────────────────┐
│                              │
│       [ Media Icon ]         │
│                              │
│   Media Access Required      │
│                              │
│ Allow access to your photos  │
│ and videos to add media.     │
│                              │
│       [ Allow Access ]       │
└──────────────────────────────┘

Rules:

- Handle permissions gracefully.
- Never show a broken or empty grid without explanation.
- Provide a clear action to request permission again.
- Handle unsupported media types safely.

---

26. Settings Screen Design

Structure

┌──────────────────────────────┐
│ Back       Settings          │
├──────────────────────────────┤
│ General                      │
│  Default Export Quality  >   │
│  Hardware Acceleration   [x] │
│                              │
│ Storage                      │
│  Clear Cache             >   │
│  Cache Size: 120 MB          │
│                              │
│ About                        │
│  Version 1.0.0               │
│  Privacy Policy          >   │
└──────────────────────────────┘

Settings Groups

General

Default Export Quality     >
Hardware Acceleration      [ON/OFF]
Default Frame Rate         >
Haptic Feedback             [ON/OFF]

Storage

Cache Size: 120 MB
Clear Cache                 >

About

Version 1.0.0
Privacy Policy              >
Terms of Service            >

Rules

- Use standard Material 3 list items.
- Group settings logically.
- Use switches for binary settings.
- Use navigation/list items for selection settings.
- Destructive actions such as Clear Cache must show a confirmation dialog.
- Do not use random custom UI patterns for standard settings.
- Keep settings simple and easy to scan.

Clear Cache Confirmation

┌──────────────────────────────┐
│ Clear Cache?                 │
│                              │
│ This will remove temporary   │
│ cached files. Your projects  │
│ and media will not be deleted│
│                              │
│ [ Cancel ]       [ Clear ]   │
└──────────────────────────────┘

---

27. Jetpack Compose Implementation Guide

Theme Setup

Use Material 3 "ColorScheme" for the dark theme.

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF121218),
    surfaceVariant = Color(0xFF1A1A22),
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFFA0A0AB),
    error = Color(0xFFFF4D4F),
    outline = Color(0xFF2A2A35)
)

Custom Shapes

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

Typography Setup

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

Compose Rules

- Always use "MaterialTheme.colorScheme".
- Always use "MaterialTheme.typography".
- Do not hardcode colors inside feature Composables.
- Use defined spacing and radius tokens.
- Use "Modifier.clip()" with defined shapes.
- Use "animate*AsState" for simple micro-interactions.
- Use "ModalBottomSheet" or "BottomSheetScaffold" for tool panels.
- Keep feature Composables as stateless as possible.
- Hoist state to the appropriate ViewModel/state holder.
- Avoid heavy calculations inside composition.

---

28. Design Tokens and Material 3 Mapping

Use design tokens instead of random hardcoded values.

Spacing

object AppSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

Usage:

Use AppSpacing.md instead of 12.dp directly in feature UI.
Use AppSpacing.lg instead of 16.dp directly.

Radius

object AppRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val sheet = 24.dp
    val clip = 10.dp
    val card = 16.dp
    val button = 999.dp
}

Icon Size

object AppIconSize {
    val small = 16.dp
    val standard = 24.dp
    val large = 28.dp
    val emptyState = 48.dp
}

Touch Target

Minimum interactive touch target: 48dp

Material 3 Mapping

primary              -> main actions, selected states
secondary            -> active indicators, snap lines
background           -> app background
surface              -> cards and sheets
surfaceVariant       -> elevated surfaces
error                -> destructive actions and errors
outline              -> borders and dividers
onSurface            -> primary text
onSurfaceVariant     -> secondary text

Rules:

- Do not override Material components inconsistently.
- Use semantic color names.
- Keep editor-specific colors separate from generic Material theme colors.
- Do not introduce random colors in feature screens.

Editor-specific Colors

object EditorColors {
    val timelineBackground = Color(0xFF0E0E13)
    val playhead = Color(0xFFFF3B30)
    val snapLine = Color(0xFF22D3EE)

    val clipVideo = Color(0xFF3B82F6)
    val clipImage = Color(0xFF8B5CF6)
    val clipAudio = Color(0xFF22C55E)
    val clipText = Color(0xFFF59E0B)
    val clipOverlay = Color(0xFF22D3EE)

    val selectedBorder = Color(0xFF7C5CFF)
    val selectedFill = Color(0x227C5CFF)
}

Usage:

Timeline UI -> EditorColors
Generic app UI -> MaterialTheme.colorScheme
Typography -> MaterialTheme.typography
Spacing -> AppSpacing
Radius -> AppRadius
Icons -> AppIconSize

---

29. Component Implementation Notes

Primary Button

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
)

Rules:

- Height: 48dp
- Radius: pill / 999dp
- Primary color
- Clear disabled state
- Show loading spinner when action is running
- Prevent double-tap actions through state control

Icon Button

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false
)

Rules:

- Minimum touch target: 48dp
- Always provide "contentDescription".
- Selected state uses primary tint.
- Disabled state must be visually obvious.

Bottom Sheet

@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
)

Rules:

- Top radius: 24dp
- Visible drag handle
- Title and close action
- Standard sheet transition
- Avoid full-screen height unless necessary
- Use consistent internal spacing

Clip Card

@Composable
fun ClipCard(
    clip: ClipUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onTrimStart: () -> Unit,
    onTrimEnd: () -> Unit
)

Rules:

- Show type-based background.
- Show border when selected.
- Show trim handles only when selected.
- Use thumbnail for video/image clips.
- Use waveform for audio clips when implemented.
- Missing media should show a warning state.

Other Reusable Components

Create reusable components for:

AppPrimaryButton
AppSecondaryButton
AppIconButton
AppChip
AppSlider
AppBottomSheet
AppCard
ProjectCard
ClipCard
ToolItem
EmptyState
LoadingState
ErrorState

Do not duplicate the same UI implementation across multiple screens.

---

30. Screen-wise UI Specifications

Splash Screen

Background: app background
Logo: centered
Animation: fade only
Duration: minimal

Rules:

- No heavy animation.
- Quickly transition to Home.
- Avoid unnecessary loading delays.

Home Screen

Top: logo/title
Primary action: New Project
Content: recent project grid
Empty state: centered

Rules:

- Responsive grid.
- Rounded project thumbnails.
- Project overflow menu.
- Delete confirmation.
- Loading state while projects are being loaded.
- Empty state when no projects exist.

New Project Sheet

Aspect ratio chips:
- 9:16
- 16:9
- 1:1
- 4:5

Project name input optional
Create button primary
Visual ratio preview

Rules:

- Default ratio: 9:16.
- Show visual preview of selected ratio.
- Keep sheet compact.
- Create button should be the primary action.

Media Picker

Tabs:
- Videos
- Images
- Audio

Grid:
- thumbnails
- duration badge
- selection check

Rules:

- Large thumbnails.
- Smooth scrolling.
- Clear permission state.
- Unsupported items handled safely.
- Multi-selection supported.

Editor Screen

Top Bar
Preview Area
Timeline Area
Bottom Tool Panel

Rules:

- Export always visible.
- Undo/redo near the top bar.
- Tool panel horizontally scrollable when needed.
- Preview should remain visible and should not be excessively covered.
- Timeline should remain thumb-friendly.
- Selected clips must be visually obvious.

Export Screen

Video Preview
Resolution
Frame Rate
Quality
Estimated Size
Export Button
Progress State
Success/Error State

Rules:

- Export button disabled during export.
- Cancel button visible during export.
- Progress must be clear.
- Success state provides Share/Open/Done.
- Error state provides Retry/Cancel.

---

31. Responsive and Adaptive Design

Primary target: mobile phones.

The UI must also adapt to foldables and tablets.

Compact — Phones

Bottom navigation / bottom tool bar
Full-screen bottom sheets
Single-column or 2-column project grid
Compact timeline

Medium — Foldables / Small Tablets

Slightly taller timeline
3-column project grid
Wider bottom sheets
Side sheets where appropriate

Expanded — Tablets

4+ column project grid
Side panel for tools where appropriate
Preview and timeline can use additional available space

Rules:

- Use "WindowSizeClass" in Jetpack Compose.
- Do not stretch the timeline excessively on tablets.
- Keep touch targets at least 48dp.
- Keep tool controls reachable.
- Do not simply scale the entire phone UI.
- Adapt layout based on available width.

---

32. Timeline UI Implementation Notes

Timeline State

data class TimelineUiState(
    val currentTimeMs: Long,
    val durationMs: Long,
    val zoom: Float,
    val selectedClipId: String?,
    val tracks: List<TrackUiModel>,
    val snappingEnabled: Boolean
)

Rendering Approach

Use:

Canvas:
- time ruler
- playhead
- snap indicators

LazyRow or custom layout:
- timeline clips

Box overlay:
- playhead
- snap lines
- selection indicators

Rules:

- Separate static and dynamic drawing layers.
- Avoid recomputing clip positions every frame.
- Cache clip rectangles.
- Draw only visible clips.
- Use stable IDs.
- Keep playhead rendering separate from clip rendering.
- Avoid full timeline recomposition when only the playhead moves.

Clip Position

fun clipX(
    startTimeMs: Long,
    pixelsPerMs: Float
): Float {
    return startTimeMs * pixelsPerMs
}

fun clipWidth(
    durationMs: Long,
    pixelsPerMs: Float
): Float {
    return durationMs * pixelsPerMs
}

Selection Visual

Default:
- clip background
- subtle border

Selected:
- primary border
- slight glow
- trim handles visible

Dragging:
- slight scale optional
- snap line visible

Missing media:
- warning icon
- muted appearance

Timeline Interaction

Support:

Tap clip
Long press clip
Drag clip
Trim start
Trim end
Horizontal scrolling
Pinch / zoom
Playhead scrubbing
Snapping
Multi-track editing

Rules:

- Interactions must feel immediate.
- Do not add unnecessary animation to timeline movement.
- Snap feedback should be visible but subtle.
- Do not block touch input with overlays unnecessarily.

---

33. Animation, Performance and Smoothness

Animation

Use Compose animation APIs.

Recommended specs:

val fastTween = tween<Float>(
    durationMillis = 120
)

val normalTween = tween<Float>(
    durationMillis = 200
)

val sheetTween = tween<Float>(
    durationMillis = 250
)

Spring for drag interactions:

val dragSpring = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

Animation usage:

Selection: fastTween
Bottom sheet: sheetTween
Screen transition: normalTween
Drag: spring
Success check: normalTween
Progress: linear / fastTween

Rules:

- Animations must remain short and interruptible.
- Do not animate layout size heavily during scrolling.
- Avoid blur effects on low-end devices.
- Avoid unnecessary scale/rotation effects.
- Never sacrifice timeline responsiveness for visual effects.

UI Performance

- Keep composables stateless where possible.
- Use stable state objects.
- Use "remember" for calculated values.
- Use "derivedStateOf" for scroll-based values.
- Avoid heavy work inside composition.
- Avoid unnecessary lambda/object creation.
- Use "LazyColumn" / "LazyRow" / "LazyVerticalGrid" for large lists.

Timeline Performance

- Draw only the visible timeline range.
- Cache thumbnails.
- Cache clip layout calculations.
- Avoid full-list recomposition on playhead movement.
- Separate playhead layer from clip layer.
- Use stable clip IDs.
- Avoid creating large objects every frame.
- Keep drag operations lightweight.

Image Loading

Use Coil for thumbnails.

Rules:

- Use fixed thumbnail sizes.
- Request appropriately sized images.
- Cache thumbnails.
- Avoid loading original frames into the timeline.
- Use crossfade only where it does not affect scrolling performance.

General Performance

Target:
- Smooth 60fps UI
- Responsive touch interactions
- Minimal unnecessary recomposition
- No UI-thread blocking

Avoid:

- Heavy blur
- Excessive shadows
- Huge bitmap allocations
- Full-resolution timeline thumbnails
- Heavy animations during scrolling
- Synchronous media processing on the main thread

---

34. UI/UX and Design QA Checklist

Before marking the UI complete:

Visuals

[ ] Dark theme consistent across all screens
[ ] No pure black (#000000) background unless specifically required
[ ] Text readable on all surfaces
[ ] Colors follow design tokens
[ ] Icons aligned and correctly sized
[ ] Spacing follows AppSpacing
[ ] Radius follows AppRadius
[ ] No random colors
[ ] No inconsistent component styling

Interaction

[ ] All touch targets are at least 48dp
[ ] Press states are visible
[ ] Selected states are obvious
[ ] Bottom sheets have drag handles
[ ] Destructive actions use confirmation dialogs
[ ] Buttons have disabled states
[ ] Undo/redo state is clear
[ ] Haptic feedback is subtle

Timeline

[ ] Timeline scrolling is smooth
[ ] Playhead is clearly visible
[ ] Clip selection is obvious
[ ] Trim handles work correctly
[ ] Snap feedback is visible
[ ] Dragging feels responsive
[ ] Missing media state exists
[ ] Timeline does not unnecessarily recompose

States

[ ] Empty state exists
[ ] Loading state exists
[ ] Error state exists
[ ] Permission denied state exists
[ ] Export progress exists
[ ] Export success state exists
[ ] Export error state exists

Responsive

[ ] Small phones tested
[ ] Large phones tested
[ ] Foldable/medium layout considered
[ ] Tablet layout considered
[ ] No clipped text
[ ] No overlapping controls
[ ] Timeline remains usable on larger screens

Performance

[ ] Heavy work is not performed on UI thread
[ ] Images are cached
[ ] Timeline thumbnails are optimized
[ ] Animations remain smooth
[ ] No unnecessary recompositions
[ ] No excessive overdraw
[ ] Low-end device performance tested

---

35. AI Prompts for UI Implementation

Use "UI_DESIGN_SYSTEM.md" as the single source of truth for all UI implementation. Do not invent colors, spacing, radius, typography, component styles, or interaction patterns outside this design system unless explicitly required.

Theme and Design Tokens Prompt

Use UI_DESIGN_SYSTEM.md as the single source of truth.

Generate the complete Jetpack Compose Material 3 dark theme for the application.

Create:
- Color.kt
- Type.kt
- Shape.kt
- Theme.kt
- Spacing.kt
- DesignTokens.kt
- EditorColors.kt

Use the exact colors, typography, spacing, radius, icon sizes, and animation values defined in UI_DESIGN_SYSTEM.md.

Do not introduce random colors or spacing values.

Use semantic names instead of raw hex values inside feature Composables.
Reusable Components Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Generate reusable Jetpack Compose components for:

- AppPrimaryButton
- AppSecondaryButton
- AppIconButton
- AppChip
- AppSlider
- AppBottomSheet
- AppCard
- ProjectCard
- ClipCard
- ToolItem
- EmptyState
- LoadingState
- ErrorState

Implement:
- Default state
- Pressed state
- Disabled state
- Selected state where applicable
- Loading state where applicable
- Accessibility content descriptions
- Minimum 48dp touch targets

Use MaterialTheme, AppSpacing, AppRadius, EditorColors, and the defined typography tokens.

Avoid duplicated UI code.
Home Screen Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create the Home Screen in Jetpack Compose.

Include:
- Top App Bar
- App logo/title
- New Project primary button
- Recent Projects
- Responsive LazyVerticalGrid
- Project Card
- Project overflow menu
- Delete confirmation dialog
- Loading state
- Empty state
- Error state

Use WindowSizeClass for responsive layouts.

Do not invent any new visual styles.
New Project Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create a Jetpack Compose New Project ModalBottomSheet.

Include:
- Project name input
- Aspect ratio selection
- 9:16
- 16:9
- 1:1
- 4:5
- Visual ratio preview
- Create button

Default aspect ratio must be 9:16.

Use the defined spacing, radius, typography, colors, and components.

Keep the sheet compact and mobile-friendly.
Editor Screen Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create the main video editor screen in Jetpack Compose.

Structure:

Top Bar
↓
Preview Area
↓
Timeline Area
↓
Bottom Tool Panel

Top Bar:
- Back
- Project title
- Undo
- Redo
- Export

Preview:
- Maintain project aspect ratio
- Keep media visually dominant
- Avoid unnecessary UI overlays

Timeline:
- Ruler
- Tracks
- Clips
- Playhead
- Selection
- Trim handles
- Snapping

Bottom Tools:
- Edit
- Audio
- Text
- Filters
- Effects
- Overlay
- Speed
- More

Use ModalBottomSheet for detailed tool panels.

Optimize the screen for smooth 60fps interaction.

Timeline UI Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create a high-performance Jetpack Compose timeline for a mobile video editor.

Include:
- Time ruler
- Multiple tracks
- Video clips
- Image clips
- Audio clips
- Text clips
- Playhead
- Clip selection
- Trim handles
- Dragging
- Scrubbing
- Snapping
- Horizontal scrolling
- Zoom
- Missing media state

Use Canvas for the ruler and playhead where appropriate.

Use LazyRow or a custom layout for clips where appropriate.

Requirements:
- Stable clip IDs
- Visible-range rendering
- Cached clip rectangles
- Cached thumbnails
- Separate playhead rendering
- Minimal recomposition
- Smooth 60fps interaction

Do not perform heavy calculations inside composition.
Media Picker Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create the Add Media screen in Jetpack Compose.

Include:
- Videos tab
- Images tab
- Audio tab
- 3/4-column responsive grid
- Media thumbnails
- Video duration badges
- Multi-selection
- Selection checkmark
- Selected item count
- Add to Timeline button
- Permission denied state
- Empty state
- Loading state

Use Coil for image/video thumbnails.

Handle Android media permissions gracefully.
Bottom Sheet Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create reusable ModalBottomSheet panels for:

- Edit
- Audio
- Text
- Filters
- Effects
- Overlay
- Speed
- Export settings

Each sheet must include:
- Drag handle
- Title
- Close button
- Consistent spacing
- Material 3 components
- Dark theme
- Defined radius
- Accessible controls

Do not create a different visual style for each sheet.
Export Screen Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create the complete Export Screen in Jetpack Compose.

Include:
- Video preview/thumbnail
- Resolution selector
- FPS selector
- Quality selector
- Estimated file size
- Export button
- Export progress
- Cancel export
- Success state
- Error state
- Share
- Open
- Done
- Retry

Export must never block the UI thread.

Disable duplicate export actions while rendering.

Show clear progress and preserve project state if export fails or is cancelled.
Settings Prompt
Use UI_DESIGN_SYSTEM.md as the single source of truth.

Create the Settings Screen in Jetpack Compose.

Sections:

General:
- Default Export Quality
- Hardware Acceleration
- Default Frame Rate
- Haptic Feedback

Storage:
- Cache Size
- Clear Cache

About:
- Version
- Privacy Policy
- Terms of Service

Use Material 3 list items.

Clear Cache must display a confirmation dialog.

Keep the settings screen simple, clean, and consistent with the main application theme.
Final UI Optimization Prompt
Review the entire application's Jetpack Compose UI against UI_DESIGN_SYSTEM.md.

Check:

- Colors
- Typography
- Spacing
- Radius
- Components
- Touch targets
- Dark theme
- Responsive layout
- Timeline performance
- Animation performance
- Loading states
- Empty states
- Error states
- Permission states
- Export states
- Accessibility
- Recomposition
- Image loading
- Low-end device performance

Fix every inconsistency you find.

Do not redesign the application or introduce new visual styles.

UI_DESIGN_SYSTEM.md must remain the single source of truth.

Prioritize:
1. Smooth interaction
2. Clear hierarchy
3. Consistency
4. Performance
5. Accessibility
6. Premium visual quality


36. Final Design Rules
Follow these rules strictly throughout the application:
Keep the UI dark, clean, premium, and focused.
Keep preview and timeline as the main focus.
Use the primary color only for important actions, selection, and key indicators.
Use bottom sheets for detailed tools.
Avoid cluttered screens.
Keep animations short, smooth, and interruptible.
Use consistent spacing and radius tokens.
Make selection states obvious.
Make timeline interactions responsive.
Keep touch targets at least 48dp.
Never block the UI thread with media processing.
Optimize thumbnails and timeline rendering.
Use Material 3 for generic application UI.
Use EditorColors for editor-specific elements.
Do not introduce random colors or spacing.
Keep media/content visually more prominent than the surrounding UI.
Test on small and low-end Android devices.
Use responsive layouts for different screen sizes.
Provide loading, empty, error, permission, and success states.
Prioritize real usability over unnecessary visual effects.
37. Design Token Summary
{
  "theme": "dark",
  "primary": "#7C5CFF",
  "secondary": "#22D3EE",
  "background": "#0B0B0F",
  "surface": "#121218",
  "surface_variant": "#1A1A22",
  "surface_card": "#1E1E28",
  "text_primary": "#F5F5F7",
  "text_secondary": "#A0A0AB",
  "error": "#FF4D4F",
  "outline": "#2A2A35",
  "playhead": "#FF3B30",
  "snap_line": "#22D3EE",
  "clip_video": "#3B82F6",
  "clip_image": "#8B5CF6",
  "clip_audio": "#22C55E",
  "clip_text": "#F59E0B",
  "clip_overlay": "#22D3EE",
  "selected_border": "#7C5CFF",
  "spacing_unit": "4dp",
  "touch_target": "48dp",
  "radius_small": "8dp",
  "radius_medium": "12dp",
  "radius_card": "16dp",
  "radius_clip": "10dp",
  "radius_sheet": "24dp",
  "radius_button": "999dp",
  "animation_fast": "120ms",
  "animation_normal": "200ms",
  "animation_sheet": "250ms"
}