#!/bin/bash
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -inform PEM -in private.pem -out src/main/resources/private_key.pem -nocrypt
openssl rsa -in private.pem -outform PEM -pubout -out src/main/resources/public_key.pem
rm private.pem
