#!/usr/bin/env bash
kubectl create configmap ingestion-manager-conf --from-file=../conf/$1/application_ext.conf