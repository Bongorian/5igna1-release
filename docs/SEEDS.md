# SEED and RESEED

[Guides](README.md) · [日本語](SEEDS.ja.md)

A **SEED** identifies a fault's pattern. Tap its number in either editor mode to enter a signed 64-bit integer, or tap **RESEED** to generate a new one. The field updates immediately; Apply saves the change and Back/× cancels it. Values outside −9223372036854775808 through 9223372036854775807 are rejected without changing the draft.

| Action | Changes | Retains |
|---|---|---|
| Enter SEED / selected-fault RESEED | One structural identity | Route, LEVEL, controls, fixed internal values and fixed EVENT SEED |
| Hold the main shuffle button | Seeds of currently active, seed-dependent faults | Inactive RAW/TAP/experimental stages and all controls |
| Tap shuffle | A random route, controls and identities | Other unselected saved parameters |
| Reset selected fault | Default controls; removes fixed overrides and EVENT SEED | Structural SEED |

Reseeding is unavailable during recording. COLOR MAP, MOTION BLUR, SMEAR and Digital thru do not offer structural reseeding because those selected models do not use it. Zero LEVEL, a temporarily inactive event, or fixed downstream values can also make a changed seed have no visible effect.

**EVENT SEED**, in ADVANCED MODE for incident-generating faults, controls the incident sequence separately. AUTO derives it from the structural seed and the current processing session. A fixed EVENT SEED stays fixed when structural SEED changes; AUTO returns it to session-dependent behavior. This is different from the reduced `identitySeed` / `eventSeed` rendering parameters in the internal catalog.

Fixed internal values take precedence over generated values. RESEED preserves those deliberate choices; return the relevant FIX values to AUTO if they should follow the new identity. It does not reset the fault clock or change source playback.

The same structural seed can reproduce a spatial pattern with the same settings. Reproducing incident timing also requires the same fixed EVENT SEED, fault time, inputs and controls. Reproducing the complete image additionally requires the same source samples. A seed does not recreate a changing camera scene, and TIME ECHO has its own randomized timing.
