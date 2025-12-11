# --------------------------------------------------------
# 1. Build Stage
# --------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /sugiura_discordbot

# サブディレクトリ内のpom.xmlをコンテナのルートにコピー
# ホスト側の "sugiura_discordbot/pom.xml" を コンテナの "./pom.xml" に置く
COPY pom.xml .

# 依存関係のダウンロード
RUN mvn dependency:go-offline

# ソースコードをコピー
# ホスト側の "sugiura_discordbot/src" を コンテナの "./src" に置く
COPY src ./src

# ビルド実行
# treeの結果にある "jar-with-dependencies" を生成させる設定がpomにある前提でパッケージング
RUN mvn clean package -DskipTests

# --------------------------------------------------------
# 2. Run Stage
# --------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /sugiura_discordbot

# ビルドステージで生成された "jar-with-dependencies" (Fat Jar) をコピー
# バージョン番号が変わっても対応できるようにワイルドカード(*)を使用していますが、
# "jar-with-dependencies.jar" で終わるファイル（実行可能ファイル）を対象にします。
COPY --from=build /sugiura_discordbot/target/*.jar app.jar

# 実行
CMD ["java", "-jar", "app.jar"]