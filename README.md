# PaperYT (Android / yt-dlp GUI)

yt-dlpをAndroidで細かく操作したい中級者向けのアプリ雛形です。

## 実装済み
- 設定画面で以下を保存 (DataStore)
  - カスタムコマンドON/OFF
  - カスタムコマンド文字列
  - ダウンロード先ディレクトリ
  - 拡張子
  - メタデータ埋め込み
  - SponsorBlock除去
- 起動時にクリップボードを確認し、YouTubeリンクならURL欄に自動入力
- 確認画面相当のUIで、プリセット（最高画質、音声のみ、動画のみ）を切り替え
- 設定値 + プリセットからyt-dlpの実行コマンドを組み立ててプレビュー
- GitHub Actionsでdebug build / （main向け）署名付きrelease buildの土台を用意

## これからの拡張候補
- Termux連携 or 内蔵実行エンジンで実際にyt-dlpを実行
- 通知で進捗表示、再試行キュー、失敗履歴
- 共有メニューからのURL取り込み
- カスタムアイコン設定UI

## ローカルビルド
```bash
gradle :app:assembleDebug
```

## 署名付きビルド
1. `signing.properties.example` を参考に環境変数を設定
2. `ANDROID_KEYSTORE_PATH` 等を指定して `gradle :app:assembleRelease`
