# SSE Proxy Java Example

ストリーミング LLM API レスポンスを Server-Sent Events (SSE) として中継する
Java 実装例です。

## 概要

このアプリは、ストリーミング LLM レスポンスを中継する Spring Boot ベースの
BFF です。クライアントは LLM API を直接呼び出さず、このアプリを呼び出します。
このアプリはサーバー側に保持した API key を使って、OpenAI 互換の `/responses`
endpoint へリクエストを転送します。

3つの proxy endpoint は同じ request body を受け取り、いずれも
`text/event-stream` を返します。違いは、上流 LLM API を呼び出す Java client
の実装です。

| 上流 client | Endpoint | 確認できること |
| ---- | ---- | ---- |
| OpenAI Java SDK | `/openai-java/responses` | 公式 SDK の stream event を SSE に戻す実装 |
| JDK `HttpClient` | `/httpclient/responses` | JDK HTTP client で上流 SSE bytes をそのまま中継する実装 |
| Spring `RestClient` | `/restclient/responses` | Spring の blocking HTTP client で上流 SSE bytes をそのまま中継する実装 |

LLM 接続設定はサーバー側の環境変数から読みます。ブラウザや request body から
API key は受け取りません。

## はじめかた

Ollama などのローカル OpenAI 互換 server を使う場合は、次のように起動します。

```bash
LLM_BASE_URL=http://localhost:11434/v1 \
LLM_API_KEY=ollama \
LLM_MODEL=llama3.2 \
mvn spring-boot:run
```

OpenAI API を使う場合は、API key を環境変数として渡します。

```bash
LLM_BASE_URL=https://api.openai.com/v1 \
LLM_API_KEY="$OPENAI_API_KEY" \
LLM_MODEL=gpt-5-mini \
mvn spring-boot:run
```

別の terminal から SSE chunk が流れることを確認します。

```bash
curl -N http://localhost:8080/httpclient/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'
```

## 要件

- Java 21 以降
- Maven 3.9 以降
- 任意: Docker
- 任意: Ollama などの OpenAI 互換 Responses API server

## 設定

| 環境変数 | 既定値 | 説明 |
| ---- | ---- | ---- |
| `LLM_BASE_URL` | `http://localhost:11434/v1` | OpenAI 互換 API の base URL |
| `LLM_API_KEY` | `ollama` | 上流 server へ送る API key |
| `LLM_MODEL` | `llama3.2` | request が `model` を省略したときに `/responses` へ送る model 名 |

Spring Boot virtual threads は `src/main/resources/application.yml` で有効化しています。

## ローカル起動

```bash
LLM_BASE_URL=http://localhost:11434/v1 \
LLM_API_KEY=ollama \
LLM_MODEL=llama3.2 \
mvn spring-boot:run
```

既定では <http://localhost:8080> で待ち受けます。

## Docker 起動

```bash
mvn package
docker build -t sse-proxy-java-example .
docker run --rm -p 8080:8080 \
  -e LLM_BASE_URL=http://host.docker.internal:11434/v1 \
  -e LLM_API_KEY=ollama \
  -e LLM_MODEL=llama3.2 \
  sse-proxy-java-example
```

OpenAI API を使う場合は、API key を image に埋め込まず、環境変数として渡します。

```bash
docker run --rm -p 8080:8080 \
  -e LLM_BASE_URL=https://api.openai.com/v1 \
  -e LLM_API_KEY="$OPENAI_API_KEY" \
  -e LLM_MODEL=gpt-5-mini \
  sse-proxy-java-example
```

## Request 例

同じ payload を3つの endpoint に投げて、SSE の出力を比較できます。
`curl -N` を使うと chunk が到着したタイミングで逐次表示されます。

```bash
curl -N http://localhost:8080/openai-java/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'

curl -N http://localhost:8080/httpclient/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'

curl -N http://localhost:8080/restclient/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'
```

`HttpClient` と `RestClient` の endpoint は、上流 event を受け取った形のまま
中継します。`openai-java` endpoint は typed SDK stream event を受け取り、
SSE の `data:` event として返します。

## 性能比較

Ollama などのローカル server に向けて、同じ request body を各 endpoint へ
繰り返し送ると、end-to-end の所要時間を比較できます。OpenAI API に対して
大量に実行すると費用や rate limit の影響があるため、まずローカル環境で確認します。

順序による偏りを避けるため、3つの endpoint の実行順を入れ替えながら測ります。

```bash
/bin/bash <<'BASH'
payload='{"input":"Write one short sentence about SSE."}'
per_round=20
orders=(
  "openai-java httpclient restclient"
  "openai-java restclient httpclient"
  "httpclient openai-java restclient"
  "httpclient restclient openai-java"
  "restclient openai-java httpclient"
  "restclient httpclient openai-java"
)

for order in "${orders[@]}"; do
  echo "== ${order} =="
  for endpoint in $order; do
    i=0
    while [ "$i" -lt "$per_round" ]; do
      curl -sS -o /dev/null "http://localhost:8080/$endpoint/responses" \
        -w "${endpoint} %{time_total}\n" \
        -H "Content-Type: application/json" \
        -d "$payload" || exit 1
      i=$((i + 1))
    done
  done
done
BASH
```

2026-06-10 にローカル Ollama (`llama3.2`) へ各 endpoint 120回ずつ投げた結果は
次のとおりです。

| Endpoint | 回数 | 失敗 | 平均秒 | 最小秒 | 最大秒 |
| ---- | ----: | ----: | ----: | ----: | ----: |
| `/restclient/responses` | 120 | 0 | 0.465 | 0.296 | 0.734 |
| `/httpclient/responses` | 120 | 0 | 0.494 | 0.334 | 1.868 |
| `/openai-java/responses` | 120 | 0 | 0.930 | 0.313 | 24.083 |

この実測では `RestClient` と JDK `HttpClient` はほぼ同等で、OpenAI Java SDK 経由は
外れ値の影響で平均が大きくなりました。結果は上流 model の速度、初回 load、
CPU/GPU、同時実行数の影響を受けます。client 実装だけを比較したい場合は、実 LLM
ではなく固定応答を返す OpenAI 互換 mock server を使ってください。

1万回など大きい回数で見る場合は、`per_round` を増やします。

```bash
per_round=1667
```

6通りの順序で実行するため、`per_round=1667` では各 endpoint 約1万回になります。

固定順で簡単に見るだけなら、次のようにも実行できます。

```bash
payload='{"input":"Write one short sentence about SSE."}'
count=100

for endpoint in openai-java httpclient restclient; do
  echo "== ${endpoint} =="
  i=0
  while [ "$i" -lt "$count" ]; do
    curl -sS -o /dev/null "http://localhost:8080/$endpoint/responses" \
      -H "Content-Type: application/json" \
      -d "$payload" || exit 1
    i=$((i + 1))
  done
done
```

## Structured Outputs 例

`text` object を渡すと、Responses API の Structured Outputs request を転送できます。

```bash
curl -N http://localhost:8080/restclient/responses \
  -H 'Content-Type: application/json' \
  -d '{
    "input": "Return a greeting.",
    "text": {
      "format": {
        "type": "json_schema",
        "name": "greeting",
        "schema": {
          "type": "object",
          "properties": {
            "message": { "type": "string" }
          },
          "required": ["message"],
          "additionalProperties": false
        },
        "strict": true
      }
    }
  }'
```

## Ollama 確認

OpenAI 互換 API として動く Ollama server を起動してから、このアプリを起動します。

1つ目の terminal で Ollama server を起動し、そのまま起動しておきます。

```bash
ollama serve
```

2つ目の terminal で、使用する model を pull します。

```bash
ollama pull llama3.2
```

同じ terminal で Java アプリを起動します。

```bash
LLM_BASE_URL=http://localhost:11434/v1 \
LLM_API_KEY=ollama \
LLM_MODEL=llama3.2 \
mvn spring-boot:run
```

起動後、上記の `curl -N` コマンドを実行します。

## OpenAI API 確認

```bash
LLM_BASE_URL=https://api.openai.com/v1 \
LLM_API_KEY="$OPENAI_API_KEY" \
LLM_MODEL=gpt-5-mini \
mvn spring-boot:run
```

起動後、Request 例の3つの endpoint を同じ request body で確認します。
