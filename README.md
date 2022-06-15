# ChiselFV
A Formal Verification Framework for Chisel

---

The instructions can be referenced from the examples in the cases package. For more complex practice cases, you can see in: 

https://github.com/Moorvan/RISCV-Formal-Chisel


## Deps

The main deps is:
- SymbiYosys. You can get from:
https://github.com/YosysHQ/oss-cad-suite-build
- sbt

Or you can use Dockerfile to build the verification environment.

```shell
docker build -t chiselfv:v0 .
```