Update the OpenAirBuds Android app to remove duplicate device title text in the dashboard screen and fix the earbud seal fit test by referencing the QuickBuds repository.

Working directory: f:\Project GAMMA
Integrity mode: [OPEN: Which restrictions apply? Default: (e) None of these: no restrictions, any approach that works]

Reference material: https://github.com/spizganed/QuickBuds

## Requirements

### R1. Dashboard UI Text Cleanup
In `BudsDashboardScreen.kt`, keep the "realme Buds Air 6" title text in the top bar above the earbuds image and remove the duplicate device title text displayed below the earbuds image in the hero section.

### R2. Air Bud Seal Fit Test Implementation
Fix the earbud fit test (seal fit test) functionality so that initiating and stopping the test transmits the correct Oppo/realme protocol commands and correctly parses incoming fit test events/state updates, matching the reference implementation in https://github.com/spizganed/QuickBuds.

### R3. Reference Repository Protocol Cross-Verification
[OPEN: Should the team verify only the fit test protocol commands against QuickBuds, or also cross-verify other protocol features?]

## Acceptance Criteria

### UI Acceptance Criteria
- [ ] In `BudsDashboardScreen.kt`, the device title "realme Buds Air 6" is retained in the top app bar above the earbuds image.
- [ ] The duplicate device title text below the earbuds image in `NothingHeroSection` is removed.

### Fit Test Acceptance Criteria
- [ ] Fit test start and stop command structures match the opcode, payload format, and sequence handling defined in the QuickBuds reference implementation.
- [ ] Fit test response/event handling correctly processes earbud seal status and updates state.
- [ ] [OPEN: Verification resources and mechanism - programmatic unit tests or physical device testing?]
