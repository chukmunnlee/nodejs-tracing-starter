#!/bin/bash
kubectl create token chaos-dashboard --duration=86400s -nchaos-mesh
