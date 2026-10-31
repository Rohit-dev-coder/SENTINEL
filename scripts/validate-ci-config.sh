#!/bin/bash

echo "Validating CI configuration..."

# Check if CI workflow file exists
if [ ! -f ".github/workflows/ci.yml" ]; then
  echo "ERROR: CI workflow file not found"
  exit 1
fi

# Validate YAML syntax
if command -v yamllint &> /dev/null; then
  yamllint .github/workflows/ci.yml
  if [ $? -ne 0 ]; then
    echo "ERROR: YAML validation failed"
    exit 1
  fi
else
  echo "WARNING: yamllint not available, skipping YAML validation"
fi

# Check for required jobs
if ! grep -q "build-and-test" .github/workflows/ci.yml; then
  echo "ERROR: build-and-test job not found"
  exit 1
fi

if ! grep -q "docker-build" .github/workflows/ci.yml; then
  echo "ERROR: docker-build job not found"
  exit 1
fi

if ! grep -q "infrastructure-test" .github/workflows/ci.yml; then
  echo "ERROR: infrastructure-test job not found"
  exit 1
fi

echo "✓ CI workflow file structure is valid"
echo "✓ All required jobs are defined"

# Validate Maven configuration
echo "Validating Maven configuration..."

if [ ! -f "pom.xml" ]; then
  echo "ERROR: Root pom.xml not found"
  exit 1
fi

# Validate XML syntax
if command -v xmllint &> /dev/null; then
  xmllint --noout pom.xml
  if [ $? -ne 0 ]; then
    echo "ERROR: XML validation failed"
    exit 1
  fi
else
  echo "WARNING: xmllint not available, skipping XML validation"
fi

# Check for required plugins
if ! grep -q "maven-surefire-plugin" pom.xml; then
  echo "ERROR: maven-surefire-plugin not found"
  exit 1
fi

if ! grep -q "jacoco-maven-plugin" pom.xml; then
  echo "ERROR: jacoco-maven-plugin not found"
  exit 1
fi

echo "✓ Maven configuration is valid"
echo "✓ Required plugins are configured"

# Validate Docker configuration
echo "Validating Docker configuration..."

if [ ! -f "docker-compose.yml" ]; then
  echo "ERROR: docker-compose.yml not found"
  exit 1
fi

# Check for required services
if ! grep -q "redis:" docker-compose.yml; then
  echo "ERROR: Redis service not found"
  exit 1
fi

if ! grep -q "postgresql:" docker-compose.yml; then
  echo "ERROR: PostgreSQL service not found"
  exit 1
fi

# Check for Dockerfiles
if [ ! -f "sentinel-gateway/Dockerfile" ]; then
  echo "ERROR: Gateway Dockerfile not found"
  exit 1
fi

if [ ! -f "sentinel-admin/Dockerfile" ]; then
  echo "ERROR: Admin Dockerfile not found"
  exit 1
fi

echo "✓ Docker configuration is valid"
echo "✓ Required services and Dockerfiles are present"

# Validate test configuration
echo "Validating test configuration..."

if [ ! -d "sentinel-common/src/test" ]; then
  echo "ERROR: Common module test directory not found"
  exit 1
fi

if [ ! -d "sentinel-admin/src/test" ]; then
  echo "ERROR: Admin module test directory not found"
  exit 1
fi

if [ ! -d "infrastructure/src/test" ]; then
  echo "ERROR: Infrastructure test directory not found"
  exit 1
fi

# Check for test files
if [ $(find . -name "*Test.java" | wc -l) -eq 0 ]; then
  echo "ERROR: No test files found"
  exit 1
fi

echo "✓ Test configuration is valid"
echo "✓ Test directories and files are present"

echo ""
echo "✅ All CI configuration validations passed!"