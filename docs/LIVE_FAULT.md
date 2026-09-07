# LIVE FAULT

A changing layer over your manual settings. LIVE FAULT uses internal random values and time; it does not need movement or read the microphone, motion sensors, temperature, or CPU load.

[All guides](README.md) · [Creative recipes](RECIPES.md) · [日本語](LIVE_FAULT.ja.md)

## Start and return

LIVE FAULT is off at launch. Turn it on from the capture screen. A temporary change returns to your manual settings when it ends; turning LIVE off does the same. Manual selections and values are not overwritten by the automatic changes.

If you open a saved capture in an external viewer and return, the previous ON/OFF state is retained and processing resumes. The app remembers configuration values, but a fresh launch still starts with LIVE off.

## Controls

| Control | What it changes |
|---|---|
| Interval | Checks for a new change every 0.5–10 seconds. |
| Probability | Chance per check. 0% never triggers; 100% triggers at every check. |
| Duration | Holds a change for 0.15–5 seconds. A new event can replace its target. |
| Change amount | How far the automatic target moves from the manual values. 0% keeps them unchanged. |
| Smoothing | Transition into and out of a change. 0% switches immediately. |
| Chain switching | When allowed, selects a temporary compatible chain. Off keeps the manually selected stages. |

Start with chain switching off to learn what the selected effects do over time.

## Temporary chains

Minimum and maximum chain length can be set from 1–16 stages, defaulting to 2–4. The available effect count limits the actual length. More stages increase GPU work and can reduce frame rate.

SENSOR FAIL enters the random candidate set only when it is already in the manual chain. “Keep original display stages” starts on: an existing VHS/TERMINAL stage remains and another display stage is not added. Their parameters may still vary. Turning this option off lets the display stage be selected again with the rest of the chain.

Preserved display stages can take precedence over a lower configured stage-count maximum.

## What gets saved

The LIVE row shows the actual chain, stage count, and overall strength. Tap it to inspect the chain. The manual strength slider keeps its baseline value.

Preview and normal video share the values for a frame. A photo takes a snapshot of the values at capture. CLEAN, original RAW, and RAW video bypass processing. Processed RAW applies only its supported stages, so its chain can be shorter.
