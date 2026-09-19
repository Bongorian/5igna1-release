# Controls reference

[Guides](README.md) · [日本語](USAGE.ja.md) · [First photograph](GETTING_STARTED.md)

Learn how to capture, adjust effects and view your saved work.

## Find the action you need

| Screen | Purpose | Meaning of an action |
|---|---|---|
| Camera workspace | Prepare your next photo/video | Tap the image to request autofocus again; swipe sideways to switch PHOTO/VIDEO |
| FAULT/LIVE edit preview | Try uncommitted settings in the image | ✓ keeps changes; ×, Back, outside dismissal or leaving the app discards them |
| Saved-media viewer | Browse recorded work and its settings | Zoom photos, browse work and play video |
| TAP | Process an image/video as a new source | Enable Experimental features, select a source and capture a new output |

A camera tap does not set the AF region to the tapped coordinates. It requests autofocus again and shows a central indicator; it does not override PRO manual focus. The viewfinder has no pinch-based digital zoom.

## Camera workspace layout

The top toolbar has light, location, audio, format, resolution and Settings, with readiness below it. **PHOTO/VIDEO** tabs sit above the shutter. Saved media is at bottom left, the shutter in the center and front/rear switching at bottom right. Focal-length buttons in the image select lenses on that side. Only cameras exposed by the device are selectable; RAW support and sizes follow the selected camera.

Expand the **FAULT** bar using its chevron for the selected chain, +, shuffle and LEVEL. With LIVE ON, Pause, Trigger and Reset time are also shown. Expand FAULT if these controls are hidden. In landscape the image is on the left and controls on the right; collapsing the controls keeps the shutter accessible.

## Choose faults and inspect the edit preview

1. Open + and tap catalog rows to add/remove faults. Removing all faults gives a clean image.
2. Open individual adjustments with a row’s sliders icon or a selected chip on the capture screen. The image above shows your draft.
3. STREAM ERROR offers Digital/Analog FPV; MEDIA offers VHS, DVD, Digital thru and Analog thru; DISPLAY offers CRT, Digital thru, Network and LED. Controls follow the model.
4. Confirm with ✓ to return to capture, or cancel with ×/Back. Finish or discard editing before capturing.

Processing order is fixed. Selected and applied counts may differ because RAW, TAP or other conditions bypass stages; bypassed stages remain listed with reasons. **LEVEL is overall amount**; 0% bypasses processing. Capture-screen LEVEL changes are immediate; editor changes are drafts. An explicit capture-format change made inside an editor survives cancellation of effect edits. [Effects and models](EFFECTS.md).

## Random chain, RESEED and Reset

| Location/action | What changes | When it is kept |
|---|---|---|
| Tap capture-screen shuffle | Normally 2–5 available faults, controls, seeds and overall LEVEL | Immediately; no cancelable editor opens |
| Hold capture-screen shuffle | Pattern/identity seeds of currently available, seed-using faults | Immediately; chain, LEVEL and controls are retained |
| Shuffle in chain editor | Preview a new random chain and values | ✓ keeps it; × restores the state from before editing |
| Individual SEED entry/RESEED | That fault’s pattern/identity | ✓ keeps it; controls are retained |
| Individual Reset | Default controls; clear internal fixed overrides | ✓ keeps it; structural seed is retained |

**To try before choosing, open + and shuffle inside the editor.** Randomization generates settings once per action; LIVE controls variation over time. Randomization, RESEED and chain editing are unavailable during recording. Fewer than two faults may be selected when fewer are available. LEVEL 0, bypassed stages and models that do not use seeds can explain why RESEED has no visible effect. [Seeds and reproduction limits](SEEDS.md).

## Use LIVE on the capture screen

1. Select a fault and set LEVEL above 0. Turn **LIVE ON** in the FAULT bar.
2. Expand FAULT. **Pause** holds the fault state while the camera keeps moving; **Resume** continues it.
3. **Trigger** adds a temporary fault. Its appearance depends on the fault and controls. Triggering while held retains the event until resume/reset.
4. **Reset time** returns the fault clock to its start and clears a manual trigger. It does not reset the chain or its controls.
5. The sliders icon beside LIVE opens speed, variation and cycle settings. Inspect changes in the image above, then ✓ to keep or × to discard.

LIVE OFF means neither clean output nor a completely static pattern. Effects such as Analog FPV have intrinsic motion. Use LEVEL 0% to bypass processing. FAULT STATE event percentages describe temporary events; 0% does not remove fixed processing or intrinsic motion.

Start by trying time controls without adding inputs. Motion, audio and other device reactions are optional. LIVE and Pause return to OFF on cold launch; other settings persist. Capture-screen time controls work during ordinary video recording; finish detailed editing beforehand. [Timing, inputs and TIME ECHO](LIVE_FAULT.md).

## Formats, capture and camera adjustments

Tap the top format label to switch JPG/RAW on supported cameras. Use the size at the top or hold PHOTO/VIDEO to select resolution; holding a tab does not switch mode. The shutter or a volume key captures a photo or starts/stops recording.

Video normally uses MP4. Enable RAW video switching in Settings to also select RAW ZIP where supported. RAW ZIP saves unprocessed, silent DNG sequences. RAW photos apply supported sensor faults; the RGB preview is an approximation. JPG saves processed output; ADVANCED ON retains displayed frames. [Formats](FORMATS.md).

Enable **Settings → Modes → PRO shooting mode** to expose supported real-camera exposure, WB, focus and other controls. Changes save immediately. This differs from ADVANCED fault-internal adjustments. [PRO shooting](PRO_CAMERA.md).

Going Home, changing apps or locking the screen ends recording and saves the recorded portion. Returning never restarts it. Video mode suppresses confirmation vibration regardless of audio settings; photo mode retains it. [Recording and permissions](RECORDING.md).

## Saved-media viewer

Open it from the bottom-left thumbnail, including before your first capture.

| Scope | Placement and actions |
|---|---|
| Whole viewer | Top × closes; Open photo chooses another photo |
| Browsed collection | Previous/next arrows and item count below the header, only for multiple items |
| Current work | Date/format, SIGNAL and external-open ↗ below the work |
| Current video | Playback, time and seeking below the work |

Pinch a photo to enlarge, drag to pan and tap to reset zoom. With zoom reset, swipe sideways to browse or down to close. Tapping a video toggles playback. DNG uses an available thumbnail; use ↗ below the work for files requiring another app.

JPG, DNG and MP4 are saved in `DCIM/5igna1`; RAW ZIP is saved in `Download/5igna1`. [Privacy](PRIVACY.md).

## Exchange settings through photos

1. Choose **Open photo** at the top of the viewer and select a received photo. Experimental features and TAP are not required.
2. Open **SIGNAL** below the photo and review its recorded chain/controls. Opening alone changes no capture settings.
3. Choose **Shoot with this photo’s settings** to apply and return to the camera. For your own saved captures, **Use these settings** applies the record.

This restores chain, LEVEL, controls, seeds, detailed parameters and the Experimental switch when recorded. Capture format, resolution and LIVE settings stay unchanged. The source remains untouched. Missing/incomplete/unsupported settings cannot be applied; the app does not reconstruct them from appearance.

**Exchange original files.** Social apps and re-exporting may remove metadata. The subject, input media, LIVE time and device inputs cannot all be reconstructed, so matching settings do not guarantee an identical image. Saved MP4 from 1.7.0 onward records initial recording settings, not the history of later changes.

## Use TAP to process the photo itself

TAP lets you create new work from an existing photo or video. Enable Experimental features, choose TAP and select media with Android’s picker. The source enters after READOUT, before DATA, bypassing SENSOR/READOUT. Capture a new JPG/MP4 with the shutter; the original is unchanged.

Source playback and output recording are separate. You can pause source video and still record evolving faults. Leaving the screen stops both source playback and recording. [TAP and Experimental features](EXPERIMENTAL_SIGNALS.md).

## Settings, updates and troubleshooting

Settings save immediately; closing does not cancel them. PRO, ADVANCED, LIGHT and EXPERT serve different purposes. Start with recommended resolution. [Advanced adjustments](ADVANCED_MODE.md) · [Workload and performance modes](PERFORMANCE.md).

In Feedback, **Review diagnostic information** displays version, device, GPU, capture mode, models and other diagnostic fields. **Copy diagnostic information** is optional. This is separate from photo settings metadata: nothing is written into work, automatically sent or attached.

The Google Play edition checks updates in Settings → App. It distinguishes checking, available, none currently available and unable to check. Availability depends on the account and eligible release; DEV cannot check the release listing. F-Droid/GitHub editions omit this service.

Replay the twelve-step guide from **Settings → Quick start**. Only LEVEL and format have isolated practice controls; capture settings and work remain unchanged. The camera pauses during the guide and resumes when closed. It works offline in English, Japanese and Simplified Chinese.

If camera reconnection fails, follow the prompt to tap the viewfinder and retry. Recording never restarts automatically. [Troubleshooting](TROUBLESHOOTING.md).
