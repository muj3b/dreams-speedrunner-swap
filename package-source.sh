#!/bin/bash
echo "Packaging SpeedrunnerSwap source code..."
zip -r SpeedrunnerSwap_v4.3_full_source.zip . -x "*.git*" "target/*" "*.zip"
echo "Source code packaged to SpeedrunnerSwap_v4.3_full_source.zip"