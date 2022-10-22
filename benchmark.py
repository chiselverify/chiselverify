import os
import subprocess
import time
import sys
import numpy as np


def run_test(testname: str, n: int):
  print(f"Running test {testname} for {n} iterations: ", end="")
  res = []
  for i in range(n):
    if i % 5 == 0:
      print(".", end ="", flush=True)

    start = time.time()
    os.system(f"sbt \'testOnly {testname}\'")
    end = time.time()
    res.append(end-start)
  print()
  return res

if __name__ == "__main__":
  # Evaluate sbt startup time on current machine
  N_startups = 3
  startups = []
  for i in range(N_startups):
    start = time.time()
    os.system(f"sbt exit")
    end = time.time()
    startups.append(end-start)

  startup = np.mean(startups)
  print(f"SBT startup time: {startup}s")

  print("Running tests")

  # Run benchmark
  N = 50
  rnd = run_test("examples.leros.random.AluRandomTest", N)
  #edge = run_test("edge_test", N)
  print(f"RANDOM test: Average: {np.mean(rnd) - startup}, stdev: {np.std(rnd)}")
  #print(f"EDGE test  : Average: {np.mean(edge)}, stdev: {np.std(edge)}")
