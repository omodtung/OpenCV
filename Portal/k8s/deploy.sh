#!/bin/bash
set -e

NAMESPACE="portal"

echo "=== Building Docker images ==="
docker build -t portal/identity-service:latest ./portal-identity-service
docker build -t portal/candidate-service:latest ./portal-candidate-service
docker build -t portal/employer-service:latest ./portal-employer-service
docker build -t portal/job-service:latest ./portal-job-service

echo "=== Applying K8s manifests ==="
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/postgres.yaml
kubectl apply -f k8s/base/activemq.yaml

echo "=== Waiting for infra to be ready ==="
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=postgres --timeout=120s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=activemq --timeout=120s

echo "=== Deploying services ==="
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress.yaml

echo "=== Waiting for services ==="
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=identity-service --timeout=180s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=candidate-service --timeout=180s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=employer-service --timeout=180s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=job-service --timeout=180s

echo "=== Done! ==="
kubectl -n $NAMESPACE get pods
kubectl -n $NAMESPACE get ingress