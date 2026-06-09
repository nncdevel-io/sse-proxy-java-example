# TASKS

マイルストーン: M1
ゴール: Spring Boot 上に3種類の SSE proxy endpoint を実装する

## ワークフロールール

- タスク着手時にステータスを 🚧 に更新する
- タスク完了時にステータスを ✅ に更新する
- DependsOn のタスクがすべて ✅ でないタスクには着手しない

## ステータス表記ルール

| Status | 意味 |
| ---- | ----- |
| ⏳ | 未着手、TODO |
| 🚧 | 作業中、IN_PROGRESS |
| 🧪 | 確認待ち、REVIEW |
| ✅ | 完了、DONE |
| 🚫 | 中止、CANCELLED |

## タスク一覧

| ID | Status | Summary | DependsOn |
| ---- | ---- | ---- | ---- |
| TASK-001 | ⏳ | Maven の Java プロジェクト骨格を作成する | - |
| TASK-002 | ⏳ | Spring Boot と HTTP client 依存関係を追加する | TASK-001 |
| TASK-003 | ⏳ | LLM 接続先の設定モデルを実装する | TASK-002 |
| TASK-004 | ⏳ | SSE proxy 用のリクエストモデルを定義する | TASK-003 |
| TASK-005 | ⏳ | openai-java endpoint を実装する | TASK-004 |
| TASK-006 | ⏳ | HttpClient endpoint を実装する | TASK-004 |
| TASK-007 | ⏳ | RestClient endpoint を実装する | TASK-004 |
| TASK-008 | ⏳ | 3実装のエラー応答と切断時処理を実装する | TASK-005,TASK-006,TASK-007 |
| TASK-009 | ⏳ | Structured Outputs 用の入力項目を追加する | TASK-008 |
| TASK-010 | ⏳ | Docker 実行環境を追加する | TASK-009 |
| TASK-011 | ⏳ | proxy 動作確認用の手順を追加する | TASK-010 |
| TASK-012 | ⏳ | 単体テストと接続確認テストを追加する | TASK-011 |
| TASK-013 | ⏳ | README に起動方法と利用例を記載する | TASK-012 |
| TASK-014 | ⏳ | ビルドとテストで完了確認する | TASK-013 |

## タスク詳細（補足が必要な場合のみ）

### TASK-001

- 補足: 既存リポジトリは README と LICENSE のみのため、新規 Maven 構成を作成する
- 補足: 1つの Spring Boot アプリに3つの endpoint を同居させる
- 注意: CI/CD 設定は含めない

### TASK-003

- 補足: `base-url`、`api-key`、`model` をサーバ側の設定から読む
- 注意: API key をブラウザやログへ出さない
- 注意: ローカル Ollama の既定例は `http://localhost:11434/v1/` とする

### TASK-004

- 補足: 通常テキスト入力と Structured Outputs 用スキーマ入力を扱える形にする

### TASK-005

- 補足: Spring MVC で request を受け、公式 Java SDK で LLM に接続する
- 補足: URL は `/openai-java/responses` とする
- 注意: OpenAI 互換 `base-url` で動くかを接続確認対象に含める

### TASK-006

- 補足: Spring MVC で request を受け、Java の HttpClient で LLM に接続する
- 補足: URL は `/httpclient/responses` とする
- 補足: LLM 側の `/responses` に `stream: true` を送る
- 注意: stateful Responses の機能には依存しない

### TASK-007

- 補足: Spring MVC で request を受け、RestClient で LLM に接続する
- 補足: URL は `/restclient/responses` とする
- 注意: Spring Boot の virtual threads 有効化を前提にする

### TASK-008

- 補足: LLM 側と client 側の切断、非 2xx 応答、JSON 不正を扱う
- 注意: 3つの endpoint は `text/event-stream` を返す
- 注意: OpenAI 互換 server から受けた SSE event を原則そのまま中継する

### TASK-009

- 補足: JSON schema を proxy request から LLM request へ渡せるようにする
- 注意: schema の妥当性検証は最小限に留める

### TASK-010

- 補足: ローカル実行とコンテナ実行の両方を確認できる構成にする

### TASK-011

- 補足: Ollama と OpenAI API の両方を curl で確認できる手順にする
- 補足: 3つの endpoint へ同じ payload を投げて比較する手順を含める
- 注意: `curl -N` で SSE が逐次表示されることを確認条件にする

### TASK-012

- 補足: OpenAI 実 API を叩かないテストを基本にする
- 注意: Ollama または実 API の接続確認は環境変数がある場合のみ実行できる形にする

## Backlog一覧

| ID | Status | Summary | DependsOn |
| ---- | ---- | ---- | ---- |

## Backlog詳細（補足が必要な場合のみ）
