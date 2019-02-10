#!/usr/bin/env bash
SRC_DIR="../../essem-reporter-base/src/main/proto"
DST_DIR="../src/main/java/"
protoc -I=$SRC_DIR --java_out=$DST_DIR $SRC_DIR/EssemReport.proto
