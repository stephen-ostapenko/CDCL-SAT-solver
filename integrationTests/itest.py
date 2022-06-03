#!/usr/bin/python3

import sys
import os
import subprocess

TMP_FILE = ".tmp_file"

def get_answer(test_suite_prefix):
	fi = open(f"{test_suite_prefix}-ans", "r")
	lines = fi.readlines()
	lines = [line.rstrip() for line in lines]
	ans = list(map(int, lines))
	fi.close()

	return ans

def get_result():
	fi = open(TMP_FILE, "r")
	lines = fi.readlines()
	lines = [line.rstrip() for line in lines]
	fi.close()
	
	if (lines[0] == "satisfiable"):
		interp = [0]
		for line in lines[1:]:
			tokens = line.split()
			x = int(tokens[0])
			val = None
			if (tokens[2] == "true"):
				val = True
			elif (tokens[2] == "false"):
				val = False
			else:
				raise Exception("Unexpected variable value")

			if (len(interp) <= x):
				interp += [False] * (x - len(interp) + 1)

			interp[x] = val

		return 1, interp
	elif (lines[0] == "unsatisfiable"):
		return 0, None
	else:
		raise Exception(f"Unexpected output: {lines[0]}")

def validate_result(interp, file):
	fi = open(file, "r")
	lines = fi.readlines()
	lines = [line.rstrip() for line in lines]
	fi.close()

	for line in lines[1:]:
		tokens = line.split()
		flag = False

		for token in tokens:
			x = int(token)
			if (x == 0):
				continue

			if (x > 0 and interp[x]):
				flag = True
			if (x < 0 and not interp[-x]):
				flag = True

			if (flag):
				continue

		if (not flag):
			return False

	return True

def run_test_suite(test_suite_prefix, fail_after_first_error):
	tests = sorted(os.listdir(f"{test_suite_prefix}-data"))
	ans = get_answer(test_suite_prefix)
	assert(len(tests) == len(ans))

	print(f"[==========] Running {len(tests)} tests from suite \"{test_suite_prefix}\"")
	passed = 0
	for i in range(len(tests)):
		print("[ RUN      ]", f"Test #{tests[i]}")
		subprocess.check_call(f"../build/bin/native/sat-solverDebugExecutable/sat-solver.kexe -q -i {test_suite_prefix}-data/{tests[i]} > {TMP_FILE}", shell =  True)

		print("[   CHECK  ]", f"Test #{tests[i]}")
		sat, interp = get_result()
		fail = False
		if (sat != ans[i]):
			fail = True
		else:
			if (sat and not validate_result(interp, f"{test_suite_prefix}-data/{tests[i]}")):
				fail = True

		#if (fail):
		#	os.system(f"subl {test_suite_prefix}-data/{tests[i]}")

		if (fail_after_first_error):
			assert(not fail)

		if (fail):
			print("[       NO ]", f"Test #{tests[i]}")
		else:
			print("[       OK ]", f"Test #{tests[i]}")
			passed += 1

	print(f"[==========] Passed {passed}/{len(tests)} tests from suite \"{test_suite_prefix}\"")

if __name__ == "__main__":
	args = sys.argv
	script_path = os.path.dirname(args[0])
	os.chdir(script_path)

	print("Building:")
	os.chdir("..")
	subprocess.check_call("./gradlew linkSat-solverDebugExecutableNative", shell = True)
	os.chdir("integrationTests")
	print()

	run_test_suite("small", len(args) >= 2 and args[1] == "--fail")

	if os.path.exists(TMP_FILE):
		os.remove(TMP_FILE)