package chiselFv

import chisel3.RawModule
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage, DesignAnnotation}
import firrtl.annotations.Target

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

object Check {
  def Hello() = {
    println("hello")
  }

  private def sby(mode: String = "prove", engines: String = "smtbmc boolector", depthStr: String, files: Array[String], module: String) = {
    s"""[options]
       |mode $mode
       |$depthStr
       |
       |[engines]
       |$engines
       |
       |[script]
       |read -sv ${files.reduce((a, b) => a + " " + b)}
       |prep -top $module -nordff
       |
       |[files]
       |${files.reduce((a, b) => a + "\n" + b)}
       |""".stripMargin
  }

  private def btorGenYs(files: String, top: String, targetFilename: String = "") = {
    s"""read -sv $files
       |prep -top $top -nordff
       |flatten
       |memory -nomap
       |hierarchy -check
       |setundef -undriven -init -expose
       |write_btor -s ${if (targetFilename == "") top else targetFilename}.btor2
       |""".stripMargin
  }

  private def aigGenYs(files: String, top: String, targetFilename: String = "") = {
    s"""read -sv $files
       |prep -top $top -nordff
       |flatten
       |memory -nomap
       |setundef -undriven -init -expose
       |delete -output
       |techmap
       |abc -fast -g AND
       |write_aiger -zinit ${if (targetFilename == "") top else targetFilename}.aig
       |""".stripMargin
  }

  def bmc[T <: RawModule](dutGen: () => T, depth: Int = 20) = {
    check(dutGen, "bmc", depth)
  }

  def kInduction[T <: RawModule](dutGen: () => T, depth: Int = 20) = {
    check(dutGen, "prove", depth)
  }

  def pdr[T <: RawModule](dutGen: () => T, depth: Int = 20) = {
    check(dutGen, "abcPdr", depth)
  }

  def modelChecker(path: String): ProcessBuilder = {
    new ProcessBuilder("modelchecker", path)
  }

  def hybridCEC(path: String): ProcessBuilder = {
    new ProcessBuilder("hybrid_cec", path)
  }

  def checkWith[T <: RawModule](dutGen: () => T, method: String, processBuilder: String => ProcessBuilder, parserGen: (() => T, String, String) => Unit) = {
    parserGen(dutGen, "_" + method, "")
    val mod = modName(dutGen)
    val dirName = mod + "_" + method
    val dir = new File(dirName)

    val file = s"$mod.aig"

    val checkProcess: Process = processBuilder(file).directory(dir).start()

    processResultHandler(checkProcess, mod + "_" + method, dirName)
  }

  private def check[T <: RawModule](dutGen: () => T, mode: String, depth: Int) = {
    generateRTL(dutGen, "_" + mode)
    val mod = modName(dutGen)
    val sbyFileName = s"$mod.sby"
    val dirName = mod + "_" + mode

    val dir = new File(dirName)
    val files = dir.listFiles.filter(_.getName.endsWith(".sv")).map(_.getName)

    if (dir.listFiles.exists(_.getName.equals(mod))) {
      new ProcessBuilder("rm", "-rf", "./" + mod).directory(dir).start()
    }

    var modeStr = "bmc"
    var engines = "smtbmc boolector"
    if (mode.equals("prove")) {
      modeStr = "prove"
    } else if (mode.equals("abcPdr")) {
      modeStr = "prove"
      engines = "abc pdr"
    }
    val depthStr = "depth " + depth


    val sbyFileContent = sby(modeStr, engines, depthStr, files, mod)
    new PrintWriter(dirName + "/" + sbyFileName) {
      write(sbyFileContent)
      close()
    }

    val sbyProcess = new ProcessBuilder("sby", sbyFileName).directory(dir).start()


    processResultHandler(sbyProcess, mod + "_" + mode, dirName)
  }

  private def checkLine(line: String) = {
    val errorEncountered = line.toLowerCase.contains("error")
    val assertFailed = line.toLowerCase.contains("assert failed")
    val coverFailed = line.toLowerCase.contains("unreached cover statement")

    val message = if (coverFailed) {
      "Cover failed"
    } else if (assertFailed) {
      "Assert failed"
    } else if (errorEncountered) {
      "Error encountered"
    } else {
      ""
    }
    if(message != "") {
      println(message)
      false
    } else {
      true
    }
  }


  def generateRTL[T <: RawModule] (dutGen: () => T, targetDirSufix: String = "_build", outputFile: String = "") = {
    val name = modName(dutGen)
    val targetDir = name + targetDirSufix
    val arg = new ArrayBuffer[String]
    arg ++= Array("--target-dir", targetDir)
    val state = new ChiselStage()
    val rtl = state.emitSystemVerilog(dutGen(), arg.toArray)

    val suffix = "sv"
    val currentPath = Paths.get(System.getProperty("user.dir"))
    val out = if (outputFile.isEmpty) {
      name + "." + suffix
    } else {
      outputFile
    }
    val filePath = Paths.get(currentPath.toString, targetDir, out)
    new PrintWriter(filePath.toString) {
      print(rtl);
      close()
    }
//    rtl
  }

  private def processResultHandler(process: Process, name: String, dir: String): Unit = {
    val output = io.Source.fromInputStream(process.getInputStream).getLines.mkString("\n")
    val error = io.Source.fromInputStream(process.getErrorStream).getLines.mkString("\n")

    if (error != "") {
      println("Error: " + error)
    }

    new PrintWriter(dir + "/" + name + ".log") {
      write(output)
      close()
    }

    new PrintWriter(dir + "/" + name + ".err") {
      write(error)
      close()
    }

    var flag = true
    for (line <- output.linesIterator) {
      breakable {
        if (!checkLine(line)) {
          flag = false
          break()
        }
      }
    }
    if (flag) {
      println(name + " successful")
    } else {
      println(name + " failed")
    }
  }

  def generateBtor[T <: RawModule] (dutGen: () => T, targetDirSufix: String = "_btor_gen", outputFile: String = "")  = {
    val name = modName(dutGen)
    val targetDir = name + targetDirSufix
    generateRTL(dutGen, targetDirSufix, outputFile)
    val currentPath = Paths.get(System.getProperty("user.dir"))
    val filePath = Paths.get(currentPath.toString, targetDir, name + ".ys")
    val files = new File(targetDir).listFiles.filter(_.getName.endsWith(".sv")).map(_.getName).mkString(" ")

    new PrintWriter(filePath.toString) {
      print(btorGenYs(files, name, name));
      close()
    }
    var dir = new File(targetDir)
    val yosysProcess = new ProcessBuilder("yosys", filePath.toString).directory(dir).start()

    processResultHandler(yosysProcess, "yosys_parse", targetDir)

  }

  def generateAIG[T <: RawModule](dutGen: () => T, targetDirSufix: String = "_aig_build", outputFile: String = "") = {
    val name = modName(dutGen)
    val targetDir = name + targetDirSufix
    generateRTL(dutGen, targetDirSufix, outputFile)
    val currentPath = Paths.get(System.getProperty("user.dir"))
    val filePath = Paths.get(currentPath.toString, targetDir, name + ".ys")
    val files = new File(targetDir).listFiles.filter(_.getName.endsWith(".sv")).map(_.getName).mkString(" ")

    new PrintWriter(filePath.toString) {
      print(aigGenYs(files, name, name));
      close()
    }
    var dir = new File(targetDir)
    val yosysProcess = new ProcessBuilder("yosys", filePath.toString).directory(dir).start()

    processResultHandler(yosysProcess, "yosys_parse", targetDir)
  }


  def modName[T <: RawModule] (dutGen: () => T): String = {
    val annos = ChiselGeneratorAnnotation(dutGen).elaborate
    val designAnno = annos.last
    designAnno match {
      case DesignAnnotation(dut) => dut.name
    }
  }


}
