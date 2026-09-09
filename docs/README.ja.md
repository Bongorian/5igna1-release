# 5igna1 ガイド

開発ブランチでは、初期値OFFの[実験的な端末連動](EXPERIMENTAL_SIGNALS.ja.md)を追加しています。FAULT別の5入力感度と、ブレ・熱雑音・スミアを扱います。公開済み1.3.0には含まれません。

撮るところから、表現を探るところまで。初回起動時のチュートリアルを含む1.3.0 / code 11の操作と機能を案内します。1.0.0とは操作が異なります。

[作品の紹介へ](../README.ja.md) · [English quick guide](GETTING_STARTED.md)

## まず使ってみる

| 知りたいこと | ガイド |
|---|---|
| APKを入れる・Obtainiumで更新する | [インストールと更新](INSTALLATION.md) |
| 最初の一枚を撮る | [はじめての撮影](GETTING_STARTED.ja.md) |
| 気になる表現から試す | [表現のレシピ](RECIPES.ja.md) |
| 操作をひととおり確認する | [操作リファレンス](USAGE.ja.md) |
| JPEG・DNG・動画の違いを知る | [保存形式の選び方](FORMATS.ja.md) |
| 写らない・保存できない・更新できない | [困ったとき](TROUBLESHOOTING.md) |

## もう少し深く

- [13 FAULTとFAULT POINTの仕様](EFFECTS.ja.md)
- [LIVE FAULTの時間変化・端末入力](LIVE_FAULT.ja.md)
- [ADVANCED MODEの内部パラメータ](ADVANCED_MODE.ja.md)
- [発熱・負荷対策と推奨設定](PERFORMANCE.ja.md)
- [実験的なRAW動画](RAW_VIDEO.ja.md)
- [プライバシーポリシー](PRIVACY.ja.md)
- [検証済みの範囲と制限](VALIDATION.md)

## 開発・配布に関わる人へ

[内部設計](ARCHITECTURE.md) · [RAWパイプライン](raw-pipeline.md) · [ビルド](building.md) · [実機検証](DEVELOPMENT.md) · [Contributing](../CONTRIBUTING.md)

[公開・更新手順](RELEASING.md) · [配布タスクリスト](LAUNCH_TASKS.md) · [F-Droid readiness](FDROID_READINESS.md) · [素材監査](audit/ASSETS.md) · [依存監査](audit/dependencies.json)
