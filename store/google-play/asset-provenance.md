# 画像の作成記録

更新日: 2026-09-09

- `images/icon.png`: 所有者の既存ベクターロゴから512×512 RGBA PNGへ出力。アプリのadaptive iconと同じ配色・形状。
- `images/featureGraphic.png`: 2026-09-07に組み込みImageGenで新規生成・編集し、1024×500 RGB PNGへ出力。元画像は `source/feature-graphic.png`。撮影結果ではなく広告用アート。
- `images/phoneScreenshots`: 2026-09-09のfdroidDebug 1.1.0をAndroid Emulator API 37、カメラ `emulated`、1080×1920で実行。日英それぞれの実UIをADBで取得。01 CLEAN、02 ROW ERROR、03 3段チェーン、04 調整。画像合成・UIの描き足しなし。
- 撮影用テストシーンはAOSP `Scene.cpp`（Copyright 2012 AOSP、Apache-2.0）の生成パターン。[素材監査](../../docs/audit/ASSETS.md)を参照。
- 旧Toren1BD室内スクリーンショットは、この公開用リポジトリのファイル・Git履歴に取り込んでいません。

## フィーチャー画像の最終プロンプト

Tool: built-in image_gen / edit（前版の紹介画像を編集対象として使用）。2026-09-07、サンセリフと余白を中心に再構成。

Use case: style-transfer. Asset type: Google Play feature graphic for 5igna1, wide 1024x500 composition. Input image is the edit target. Redesign it to feel lean, minimal and contemporary. Replace the large serif wordmark with crisp neutral sans-serif typography, medium weight, precise spacing, no serifs at all. Exact text: "5igna1" (digit five, lowercase i, g, n, a, digit one). Retain the small secondary text "GLITCH CAMERA" in a restrained sans-serif. Preserve the near-black and pale acid lime brand palette and the idea of imaging signal corruption, but simplify the elaborate glass prism to a single narrow abstract optical plane on the right with just a few horizontal displaced cyan/lime pixels. Plenty of calm negative space; disciplined Swiss editorial alignment. Flat black background, no ornate texture, no extra slogans, no badges, no phone mockup, no gradients behind the type. Keep text and key elements within central 80% safe area. Final opaque landscape artwork. It should look like a concise modern camera instrument brand.

## Play ConsoleでのAI素材申告 — 2026-09-09

作成記録に従い、生成・編集したfeatureGraphic.pngだけをAI生成素材として申告しました。所有者のベクターロゴから作ったicon.pngと、実際のアプリから取得した日英8枚のスクリーンショットにはAI生成ラベルを付けていません。
