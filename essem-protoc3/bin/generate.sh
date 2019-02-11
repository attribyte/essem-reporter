#!/usr/bin/env bash
SRC_DIR="../src/main/proto"
DST_DIR="../src/main/java/"
protoc3 -I=$SRC_DIR --java_out=$DST_DIR $SRC_DIR/EssemReport.proto
