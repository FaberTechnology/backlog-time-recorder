#!/bin/bash

set -eu

SERIAL_NUMBER="arn:aws:iam::625344915430:mfa/sugahara"
echo -n "Enter token code (6 digits): "
read TOKEN_CODE
if [ -z $TOKEN_CODE ] ; then
    echo "No token code given"
    exit 1
fi
TEMPORARY_TOKEN=`aws sts get-session-token --serial-number $SERIAL_NUMBER --token-code $TOKEN_CODE`

export AWS_ACCESS_KEY_ID=`echo $TEMPORARY_TOKEN | jq -r .Credentials.AccessKeyId`
export AWS_SECRET_ACCESS_KEY=`echo $TEMPORARY_TOKEN | jq -r .Credentials.SecretAccessKey`
export AWS_SESSION_TOKEN=`echo $TEMPORARY_TOKEN | jq -r .Credentials.SessionToken`

echo "Finished!  Enjoy your short life!"
