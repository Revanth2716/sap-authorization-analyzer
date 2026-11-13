#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# SAP Authorisation & Access Governance Analyzer - convenience runner (Linux / macOS)
#
#   ./run.sh            build, test and start the analyzer
#   ./run.sh test       run the test suite only
#   ./run.sh package    build without starting
#   ./run.sh clean      remove build output
# ---------------------------------------------------------------------------
set -e
cd "$(dirname "$0")"

case "${1:-run}" in
  test)    mvn -B test ;;
  package) mvn -B clean package ;;
  clean)   mvn -B clean ;;
  run)
    echo "Building the SAP Authorisation & Access Governance Analyzer..."
    mvn -B clean package
    echo
    echo "Starting on http://localhost:8087"
    echo "Sign in with security.admin / demo1234 for the full view."
    echo
    java -jar target/sap-authorization-analyzer.jar
    ;;
  *) echo "usage: $0 [run|test|package|clean]"; exit 1 ;;
esac
