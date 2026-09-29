#!/bin/bash
echo "Packaging SpeedrunnerSwap source code..."
zip -r SpeedrunnerSwap_v4.3.7_full_source.zip . -x "*.git*" "target/*" "*/target/*" "*.zip"
echo "Source code packaged to SpeedrunnerSwap_v4.3.7_full_source.zip"
