package chiselFv

import chisel3.RawModule
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage, DesignAnnotation}

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

object Check {
  def Hello() = {
    println("hello")
  }

  def sby(mode: String = "prove", engines: String = "smtbmc boolector", depthStr: String, files: Array[String], module: String) = {
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

  def bmc[T <: RawModule](dutGen: () => T, depth: Int = 20) = {
    check(dutGen, "bmc", depth)
  }

  def kInduction[T <: RawModule](dutGen: () => T, depth: Int = 20) = {
    check(dutGen, "prove", depth)
  }

  def pdr[T <: RawModule](dutGen: () => T, depth: Int = 20) = {
    check(dutGen, "abcPdr", depth)
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
    val output = io.Source.fromInputStream(sbyProcess.getInputStream).getLines.mkString("\n")
    val error = io.Source.fromInputStream(sbyProcess.getErrorStream).getLines.mkString("\n")

    new PrintWriter(dirName + "/" + "sby.log") {
      write(output)
      close()
    }

    new PrintWriter(dirName + "/" + "sby.err") {
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
      println(mode + " successful")
    } else {
      println(mode + " failed")
    }
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

  def modName[T <: RawModule] (dutGen: () => T): String = {
    val annos = ChiselGeneratorAnnotation(dutGen).elaborate
    val designAnno = annos.last
    designAnno match {
      case DesignAnnotation(dut) => dut.name
    }
  }


}
