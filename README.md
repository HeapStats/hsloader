# HSLoader

HSLoader is data loader for HeapStats snapshot and resource log to Elasticsearch.
This tool can be run for Elasticsearch 5.0 or later.


## How to use

```
$ java -jar hsloader.jar [options] file1 file2 ...
```


## Options

* --help
  * Help message
* --mode
  * Parser mode. ```snapshot``` or ```log``` .
  * snapshot is by default
* --host
  * Hostname of Elasticsearch
  * localhost is by default
* --port
  * HTTP port of Elasticsearch
  * 9200 is by default
* --bulk
  * Number of bulk transport
  * 1000 is by default
* --timezone
  * Timezone of snapshot
  * System default is by default
* --timeout
  * Timeout in second
  * 60 is by default


## Sample dashboard

```kibana-dashboard.json``` in this repository provides sample dashboard of HSLoader. You can use it on Kibana 5.4 or later.
If you use this dashboard, you need to define indices as below:

* HeapStats snapshots
  * heapstats-snapshot-summary-*
  * heapstats-snapshot-objects-*
  * heapstats-snapshot-refs-*
* HeapStats resources
  * heapstats-resource-log-*
  * heapstats-resource-diff-*

------------

HSLoader は HeapStats のスナップショットとリソースログを Elasticsearch にロードするためのアプリケーションです。
Elasticsearch 5.0 以降に対応しています。


## 使用方法

```
$ java -jar hsloader.jar [options] file1 file2 ...
```


## オプション

* --help
  * ヘルプメッセージ
* --mode
  * パーサーモード。 ```snapshot``` または ```log``` 。
  * デフォルト値は snapshot
* --host
  * Elasticsearch のホスト名
  * デフォルト値は localhost
* --port
  * Elasticsearch のポート番号
  * デフォルト値は 9200
* --bulk
  * 一度に投入するデータ量（バルク転送）
  * デフォルト値は 1000
* --timezone
  * スナップショット取得時間のタイムゾーン指定
  * デフォルトはシステムのタイムゾーン
* --timeout
  * タイムアウト（秒）
  * デフォルトは 60


## サンプルダッシュボード

ソースに含まれる ```kibana-dashboard.json``` を Kibana 5.4 以降にインポートすることで HSLoader のサンプルダッシュボードが利用可能です。
その際、Kibana で以下のインデックスを定義するようにしてください。

* HeapStats snapshots
  * heapstats-snapshot-summary-*
  * heapstats-snapshot-objects-*
  * heapstats-snapshot-refs-*
* HeapStats resources
  * heapstats-resource-log-*
  * heapstats-resource-diff-*

