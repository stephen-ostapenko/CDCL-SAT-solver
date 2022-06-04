# CDCL-SAT-solver

This SAT-solver works with DIMACS-formatted CNF formulas

## Usage

```
./sat-solver
[-i | --input <DIMACS format file>] [-o | --output <path to output file>]
[-q | --quiet] [-t | --time]
```

## Output

Examples of output:
```
solving formula with 20 clauses

satisfiable
1 <- false
2 <- true
3 <- true
4 <- false
5 <- true
6 <- false

Done in 3 ms
```

```
solving formula with 20 clauses
solving formula with 31 clauses
solving formula with 42 clauses

unsatisfiable

Done in 111 ms
```