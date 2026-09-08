# 表現の出発点

[English](RECIPES.md) · [ガイド](README.ja.md)

**ROW ERROR → CHROMA ERROR**。読み出しのずれを中程度、色差のずれを小さくして縦の輪郭を眺めます。同じ弱い行が漂います。短い中断が欲しければ欠落を増やします。

**VHS → CRT**。先にテープ帯域とCRTの走査・蛍光体を決め、次にtracking・dropout・小さなconvergence故障を加えます。装置の性格と、その内部故障を分けて探れます。LIVEの動きや音量入力で時間軸を揺らし、Chainを入れ替えず事故を待ちます。

**ADDRESS ERROR → CFA ERROR**。対応カメラで加工DNGを選び、小さなbyte offsetと限定したCFA範囲から始めます。RGBプレビューは近似なので現像したDNGも確認してください。RAWは別露光です。

気に入った経路でRESEEDを押すと、操作値を保って故障位置やbiasが変わります。すぐ再抽選せず、その個体をしばらく観察してください。現在の経路・操作値・個体は保存しますが、名前付きpreset管理はありません。

変化が見えないときはLEVEL、FAULTの有効化、各操作値を確認します。STREAM ERRORは事故の間、静かになる設計です。[操作](USAGE.ja.md)

今のFAULTがどう変化するかはLIVEで設定します。周期・更新間隔は秒単位です。[時間変化の操作](LIVE_FAULT.ja.md)。内部値を直接固定するには、設定で [ADVANCED MODE](ADVANCED_MODE.ja.md) を一括ONにします。
