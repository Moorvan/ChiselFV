# ChiselFV
A Formal Verification Framework for Chisel

---

The instructions can be referenced from the examples in the cases package. For more complex practice cases, you can see in: 

https://github.com/Moorvan/RISCV-Formal-Chisel


## Deps

The main deps are:
- SymbiYosys. You can get it from: https://github.com/YosysHQ/oss-cad-suite-build
- sbt

Or you can use Dockerfile to build the verification environment.

```shell
docker build -t chiselfv:v0 .
```

## Cases

### Memory

Memory is an important part of hardware development, and Chisel provides a library to construct Memory directly, making it easy to get a memory module with a parameterized size and width.
We can quickly use 15 lines to get a parameterized memory in Chisel.

```scala
class Memory(c: Int, w: Int) extends Module {
  val nw = log2Ceil(c)
  val io = IO(new Bundle {
    val wrEna = Input(Bool())
    val wrData = Input(UInt(w.W))
    val wrAddr = Input(UInt(nw.W))
    val rdAddr = Input(UInt(nw.W))
    val rdData = Output(UInt(w.W))
  })
  val mem = Mem(c, UInt(w.W))

  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }
  io.rdData := mem.read(io.rdAddr)
}
```

Next, we need to verify the correctness of the memory module. To verify the correctness of the memory module, first, we need to describe its properties of memory module. We can describe the property as at any time, any address $addr$ can be written with $data_1$, and then after any time $t$, if there is no write operation to the same address during this period, $data_2$ read from the address $addr$ should be the same as $data_1$. Then, in the multi-ported Memory, we need to verify the hardware property as: at any time, any write port $i$ can write $data_1$ to any address $addr$, and then after any time $t$, if there is no write operation to the same address during this period, $data_2$ read from the address should be the same as $data_1$.

Next, we need to describe the property in Chisel. We first consider a simple version, which is the property description of the Memory module. Note that there are several arbitrary variables: $addr$, $data_1$, and $t$. When we formalize the property, we can remove the two arbitrary variables and only keep $addr$ to simplify the verification complexity. We can use the fact that the input determines the write data, so the write data itself has the "any" meaning, and the time $t$ can be removed in the same way. Then, the updated property can be written as: at any time, for any variable $addr$, when the write address is $addr$, record the write data $data_1$, and then assume that there is no write operation to the same address during this period until the read address is $addr$, read the data $data_2$ and assert $data_1 = data_2$. 

```scala
class Memory(c: Int, w: Int) extend Module with Formal {
  ...
  // the IO and the implementation are omitted.
  
  // Formal verification
  val flag_value = WireInit(0.U(1.W))
  val addr = anyconst(nw)
  val flag = initialReg(1, 0)
  val data = Reg(UInt(w.W))

  flag.io.in := flag_value
  when (io.wrAddr === addr & io.wrEna) {
    flag_value := 1.U
    data := io.wrData
  }
  when (io.rdAddr === addr && flag.io.out === 1.U) {
    assert(data === io.rdData)
  }
}
```

Finally, we can run the formal verification with the following command in sbt console:

```shell
> runMain cases.Memory
```

The formal verification call is:

```scala
  Check.kInduction(() => new Memory(1024, 8))
```

We successfully proved that the property holds in the model.

### LVT-based multi-ported memory

Design and implementation details can be found at: https://github.com/VerificaticationStudio/MultPortedRAM.

Here, our focus is not on the design itself, so we abstract the multi-ported Memory. We are concerned about whether the multi-ported Memory can meet our requirements, so its internal implementation can be shielded, and we are only concerned about its input and output. The basic IO of a multi-ported memory with m write ports, and n read ports is shown below:

<img src="https://raw.githubusercontent.com/Moorvan/PictureHost/main/chiselfv/mpmemoryio.png" height="300" />

And the definition in Chisel is:

```scala
class MultiPortedMemory(m: Int, n: Int, size: Int, w: Int) extends Module {
  val addrW: Int = log2Ceil(size)
  val io = IO(new Bundle{
    val wrAddr = Input(Vec(m, UInt(addrW.W)))
    val wrData = Input(Vec(m, UInt(w.W)))
    val wrEna = Input(Vec(m, Bool()))

    val rdAddr = Input(Vec(n, UInt(addrW.W)))
    val rdData = Output(Vec(n, UInt(w.W)))
  })

  ...
  // The implementation is omitted.
}
```

