# Player Detail Dialog Feature

## ✨ New Feature Implemented!

You can now click on any player card to see a detailed popup with comprehensive statistics and league-specific data!

## 📊 What's Included

### 1. **Latest Match Summary**
Shows the most recent match performance:
- Yellow/Red cards
- Goals scored
- Assists made
- Saves (for goalkeepers)
- Minutes played
- Bonus points
- **Total points** for that match

### 2. **Ownership Statistics**
League-specific metrics:
- **Starts league**: % of league managers who started the player
- **Owned league**: % of league managers who own the player
- **Owned overall**: Overall FPL ownership %
- **Captain count**: How many captained this player
- **Price**: Current player value

### 3. **Who Started This Player**
Complete list showing:
- All team names that started the player
- Total count of starts
- Up to 10 teams shown (with "... and X more" if needed)

### 4. **Who Captained This Player**
Special section showing:
- ⭐ All teams who made this player captain
- Total captain count
- Team names highlighted

### 5. **Previous Fixtures Table**
Last 5 gameweeks showing:
- GW number
- Opponent
- Minutes played
- Points scored

### 6. **Upcoming Fixtures**
Next 3 fixtures with:
- GW number
- Home/Away indicator
- **Difficulty rating** (color-coded):
  - 🟢 Green: Easy (1-2)
  - 🟡 Yellow: Medium (3)
  - 🔴 Red: Hard (4-5)

## 🎯 How to Use

### From League Standings
1. View league standings
2. Click on any manager's row
3. View their team formation
4. **Click on any player card on the pitch**
5. Detailed popup appears!

### What You'll See
- Player's name and position
- Team badge with colors
- All the sections listed above
- **OK button** to close

## 🔍 Data Sources

According to the [FPL API guide](https://medium.com/@frenzelts/fantasy-premier-league-api-endpoints-a-detailed-guide-acbd5598eb19):

### API Endpoints Used:
1. **`element-summary/{element_id}/`** - Player detailed data
   - Previous fixtures
   - Upcoming fixtures
   - Historical performance

2. **`entry/{manager_id}/event/{event_id}/picks/`** - Each manager's picks
   - Used to calculate league ownership
   - Determines who started/benched/captained

3. **`bootstrap-static/`** - General player data
   - Player names, teams, positions
   - Overall ownership percentage
   - Current price

## 💡 Key Features

### Real-Time Aggregation
- Scans **top 50 managers** in the league
- Calculates accurate ownership %
- Lists all who started or captained
- Shows bench count

### Visual Design
- **Purple header** (#37003C) matching FPL branding
- **Team color badge** for player's club
- **Color-coded difficulty** for fixtures
- **Organized sections** with different backgrounds
- **Clean cards** for each data category

### Performance Statistics
Detailed breakdown of:
- **Points sources** (goals, assists, saves, etc.)
- **Match performance** (minutes, cards, bonus)
- **Historical trends** (last 5 games)
- **Future outlook** (next 3 fixtures)

## 📱 User Experience

### Click Flow
```
League Standings
    ↓
Click Manager Row
    ↓
View Formation on Pitch
    ↓
Click Any Player Card
    ↓
Detailed Popup Opens!
    ↓
Scroll through sections
    ↓
Click OK to close
```

### Information Hierarchy
1. **Header**: Player identity
2. **Latest Match**: Most important recent data
3. **Ownership**: League context
4. **Started By**: Who uses this player
5. **Captained By**: Popular captain choice?
6. **Previous**: Performance history
7. **Upcoming**: Future planning

## 🎨 Visual Examples

### Latest Match Card
```
Latest Match
━━━━━━━━━━━━━━━━━━
Yellow card:        -1
4 saves:             1
Played 90 min:       2
Bonus (10 bps):      0
━━━━━━━━━━━━━━━━━━
Total Points:        2
```

### Ownership Card
```
Ownership
━━━━━━━━━━━━━━━━━━
Starts league:    4.2%
Owned league:     4.2%
Owned overall:   10.2%
Captain count:      0
Price:          £5.7M
```

### Started By Card
```
Started By (2)
━━━━━━━━━━━━━━━━━━
• MrQifa
• Team Phoenix
```

### Captained By Card
```
Captained By (1)
━━━━━━━━━━━━━━━━━━
⭐ The Invincibles
```

## 🚀 Technical Implementation

### Concurrent Data Fetching
```kotlin
// For each manager in league (top 50):
1. Fetch their picks for current gameweek
2. Check if they have this player
3. Determine if started/benched/captained
4. Aggregate all data
5. Calculate percentages
```

### Caching Strategy
- Bootstrap data cached in ViewModel
- Player details fetched on demand
- League stats calculated when dialog opens
- Efficient parallel processing

### Error Handling
- Graceful fallback if API fails
- Shows 0% if data unavailable
- Skips managers with errors
- Continues processing others

## 📊 Statistics Calculated

### For Each Player:
- **Starts Count**: How many started (position 1-11)
- **Bench Count**: How many benched (position 12-15)
- **Captain Count**: How many captained
- **Vice-Captain Count**: How many vice-captained
- **Starts %**: (starts / total managers) × 100
- **Owned %**: ((starts + bench) / total managers) × 100

## 🎯 Use Cases

### 1. **Captain Analysis**
- See which players are popular captains
- Check if your league agrees with your choice
- Find differential captain options

### 2. **Ownership Research**
- Understand league meta
- Find differentials (low ownership)
- See template players (high ownership)

### 3. **Performance Tracking**
- Review recent form
- Check consistency
- Plan transfers based on fixtures

### 4. **League Strategy**
- See who your rivals captain
- Find unique picks nobody has
- Plan ahead based on fixtures

## ✅ Benefits

### For Users
- 📊 All data in one place
- 🎯 League-specific insights
- 📈 Performance history
- 🔮 Future planning
- ⚡ Quick access

### For Strategy
- 🎲 Find differentials
- 🏆 Popular captain choices
- 📉 Identify weak links
- 🔄 Transfer targets
- 🎯 Ownership trends

## 🔮 Future Enhancements

Potential additions:
- **Price change tracking**
- **Transfer trends** (in/out)
- **Form indicators**
- **Comparison with similar players**
- **Expected points** for upcoming fixtures
- **Injury/suspension news**
- **Share dialog** as image
- **Add to watchlist** feature

## 🎊 Ready to Use!

The feature is fully implemented and ready! Just:
1. **Build and run** the app
2. Navigate to **league standings**
3. Click any **manager**
4. View their **team formation**
5. **Click any player card**
6. Enjoy the **detailed popup**!

Your FPL tracking experience just got even more comprehensive! 🏆⚽📊

