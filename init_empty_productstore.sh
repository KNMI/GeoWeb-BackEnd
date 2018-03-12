#!/bin/bash
productstore=/tmp
mkdir -p ${productstore}/admin/config/
cp ./src/main/java/nl/knmi/geoweb/backend/admin/services.json ${productstore}/admin/config/services.json.dat


mkdir -p ${productstore}/admin/locations/
cp ./src/main/java/nl/knmi/geoweb/backend/admin/locations.json ${productstore}/admin/locations/locations.dat

