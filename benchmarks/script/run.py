#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
from configs import *
import subprocess



def main():
    for bm in BENCHMARKS:
        command = [GRADLE, ":benchmarks:run", f"-PappName={bm}"]
        subprocess.call(command, cwd=BASE)


if __name__ == "__main__":
    main()
