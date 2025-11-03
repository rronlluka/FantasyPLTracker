# Football Pitch View Implementation Guide

## Overview
The app now features a beautiful football pitch visualization similar to the official Fantasy Premier League app, displaying teams in their actual formation on a green pitch with authentic markings.

## Key Components

### 1. FootballPitch.kt
Located at: `app/src/main/java/com/fpl/tracker/ui/components/FootballPitch.kt`

This file contains the main pitch visualization components:

#### **FootballPitch Composable**
- Main container that displays the pitch and positions players
- Groups players by position (GK, DEF, MID, FWD)
- Automatically arranges players in rows based on formation

#### **PitchCanvas Composable**
- Draws the football pitch using Canvas API
- Features include:
  - Green pitch background (#00B050)
  - White pitch markings
  - Center circle and center spot
  - Penalty areas (top and bottom)
  - Goal areas
  - Penalty arcs
  - Penalty spots

#### **PlayerCardOnPitch Composable**
- Individual player card displayed on the pitch
- Shows:
  - Captain/Vice-Captain badge (C or V)
  - Team jersey color
  - Player web name
  - Team short name
  - Points with color coding:
    - Green: 10+ points
    - Light green: 6-9 points
    - Red: 0-2 points
    - Grey: 3-5 points
  - Points include captain multiplier (2x for captain)

#### **Team Colors**
The `getTeamColor()` function maps team IDs to their brand colors:
- Arsenal: #063672 (Blue)
- Chelsea: #0057B8 (Blue)
- Liverpool: #C8102E (Red)
- Man City: #6CABDD (Sky Blue)
- Man United: #DA291C (Red)
- And 15 more Premier League teams!

## Layout Structure

```
FootballPitch
├── PitchCanvas (Background)
└── Column (Player positioning)
    ├── Forwards (Top row)
    ├── Midfielders (Second row)
    ├── Defenders (Third row)
    └── Goalkeeper (Bottom row)
```

## Player Card Layout

```
PlayerCardOnPitch
├── Captain Badge (if applicable)
└── Card
    ├── Jersey/Team Color Box
    ├── Player Name
    └── Points (color-coded)
```

## Substitutes Section

The bench players are displayed below the pitch in a horizontal row:
- Position labels (GKP, 1. DEF, 2. MID, 3. FWD)
- Smaller cards with team badge, name, and points
- Team colors represented as circular badges

## Statistics Header

Above the pitch, key stats are displayed:
- **GW Points** (highlighted in green)
- **Total Points**
- **GW Rank**

Additional row shows:
- Transfers made
- Transfer cost
- Points on bench
- Active chip (if any)

## Customization

### Changing Pitch Colors
Edit `PitchCanvas` in `FootballPitch.kt`:
```kotlin
val pitchColor = Color(0xFF00B050) // Change this for different pitch color
val lineColor = Color.White // Change line color
```

### Modifying Player Card Size
Edit `PlayerCardOnPitch`:
```kotlin
modifier = Modifier
    .width(65.dp)  // Change width
    .height(75.dp) // Change height
```

### Adjusting Point Color Thresholds
Edit the color logic in `PlayerCardOnPitch`:
```kotlin
color = when {
    displayPoints >= 10 -> Color(0xFF00FF87) // High points
    displayPoints >= 6 -> Color(0xFF4CAF50)  // Medium points
    displayPoints <= 2 -> Color(0xFFFF5555)  // Low points
    else -> Color(0xFFF0F0F0)                // Default
}
```

## Technical Details

### Canvas Drawing
The pitch is drawn using Jetpack Compose's `Canvas` composable with `DrawScope`:
- `drawRect()` for boxes and borders
- `drawCircle()` for center circle and spots
- `drawLine()` for halfway line
- `drawPath()` with `addArc()` for penalty arcs

### Positioning System
Players are positioned using:
- `Column` for vertical stacking by position
- `Row` with `SpaceEvenly` for horizontal distribution
- Automatic spacing based on number of players in each line

### Formation Detection
The app automatically detects formation based on:
- Element types from API (1=GK, 2=DEF, 3=MID, 4=FWD)
- Grouping players by these types
- Common formations supported:
  - 4-4-2
  - 4-3-3
  - 3-4-3
  - 3-5-2
  - And any other valid FPL formation

## Performance Considerations

1. **Canvas caching**: The pitch is drawn once and reused
2. **Composable recomposition**: Only player data triggers recomposition
3. **Memory efficient**: No images loaded, pure Canvas drawing

## Future Enhancements

Potential improvements for the pitch view:
1. **Drag and drop**: Allow users to rearrange formation
2. **Animations**: Add player entry animations
3. **Player images**: Show actual player photos
4. **Fixture indicators**: Show upcoming fixtures on player cards
5. **Touch interactions**: Tap player for detailed stats
6. **Formation selector**: Switch between different formation views
7. **3D pitch**: Add perspective for more realistic look
8. **Live updates**: Animate point changes during live gameweeks

## Troubleshooting

### Players overlapping
- Check player card width in `PlayerCardOnPitch`
- Adjust `SpaceEvenly` to `SpaceBetween` if needed

### Pitch too small/large
- Modify `height` in `FootballPitch` modifier
- Adjust player card dimensions proportionally

### Team colors not showing
- Verify team ID mapping in `getTeamColor()`
- Add missing team colors for new teams

### Captain badge not appearing
- Check `isCaptain` and `isViceCaptain` flags in data
- Verify multiplier is being applied to points

## Integration

The pitch view integrates with:
- `ManagerFormationViewModel` - Provides player data
- `BootstrapData` - Team and player information
- `LiveGameweek` - Real-time point updates
- `ManagerPicks` - Captain/formation data

## Testing

To test the pitch view:
1. Enter a valid Manager ID
2. Navigate to "View Current Team"
3. Verify:
   - Pitch renders correctly
   - Players appear in formation
   - Points are displayed
   - Captain badges show correctly
   - Team colors match
   - Substitutes appear below pitch

## Credits

Inspired by the official Fantasy Premier League app design, reimagined for Android with Jetpack Compose.

