#!/bin/bash

function unknown_parameter() {
  echo -e "[\e[0;31merror\e[0m] Unknown parameter passed: $1"
  exit 1
}
