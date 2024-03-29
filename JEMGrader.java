import java.io.*;
import java.nio.file.*;

import picocli.CommandLine.*;

public abstract class JEMGrader {
  /** Hooks to allow processing before and after the compile and run steps */
  public abstract void setup();

  public abstract void beforeCompile(JavaRunner jr, File dir);

  public abstract void beforeExecute(JavaRunner jr, File dir);

  public abstract void afterExecute(JavaRunner jr, File dir);

  public abstract void afterCompileError(JavaRunner jr, File dir);

  public abstract void afterTimeoutError(JavaRunner jr, File dir);

  public abstract void afterEverything(File dir);

  public abstract void cleanup();

  @Parameters(index = "0", description = "The folder of student folders.")
  private String pathToStudentFiles;

  @Parameters(index = "1", description = "Path to test/answer files.")
  private String pathToTests;

  @Parameters(index = "2", description = "Student file with main method.")
  private String fileToCompile;

  @Option(names = { "-t", "--timeout" }, description = "How many millisecond to allow a program to run. Default 5000.")
  private int timeout = 5000;

  @Option(names = { "-p", "--policy" }, description = "Path to policy file to apply to code being graded.")
  private String policy = null;

  // Should student/test files be moved to a temp folder (true), or should the code just be run from the student folder?
  @Option(names = { "--inplace" }, description = "Should student & test files be copied to a temp folder before compiling & running (default: true")
  private boolean skipTempFolder;

  public void setUseTempFolder(boolean useTempFolder) {
    this.skipTempFolder = !useTempFolder;
  }  

  public void setPathToStudentFiles(String path) {
    pathToStudentFiles = path;
  }

  public void setPathToTests(String path) {
    pathToTests = path;
  }

  public String getPathToTests() {
    return pathToTests;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public void setFileToCompile(String filename) {
    fileToCompile = filename;
  }

  public String getFileToCompile() {
    return fileToCompile;
  }

  /**
   * Compiles and runs code in each student folder
   *
   * 1. Creates a temp folder to do work in 2. Calls test on each folder in the
   * student folder 3. Deletes the temp folder
   */
  public int go() {
    File tempFolderFolder = null;
    if (!skipTempFolder) {
      // Create a folder to store temp folders    
      tempFolderFolder = new File("tmp" + System.currentTimeMillis());
      if (!tempFolderFolder.exists() || !tempFolderFolder.isDirectory()) {
        tempFolderFolder.mkdir();
      }
    }

    File testDir = null;
    if (null != pathToTests) {
      testDir = new File(pathToTests);
    }

    setup();

    File labDir = new File(pathToStudentFiles);
    File[] studentDirs = labDir.listFiles();
    if (null != studentDirs) {
      for (File studentDir : studentDirs) {
        if (studentDir.isDirectory()) {
          test(studentDir, testDir, tempFolderFolder);
        }
      }
    }
    else {
      System.out.println("No student folders found");
      //System.out.println("No student folders found in " + pathToStudentFiles);
    }

    if (null != tempFolderFolder) {
      deleteDir(tempFolderFolder);
    }

    cleanup();

    return 0;
  }

  /**
   * Instantiates a JavaRunner object to execute java code
   *
   * This method should be overloaded if you need to add files to the classpath
   */
  public JavaRunner getJavaRunner(File dir) {
    return new JavaRunner(dir, fileToCompile, timeout, policy);
  }

  /**
   * Compiles and runs the code in the specified studentDir
   *
   * 1. Creates a temp folder inside the specified temp folder (which makes that a
   * folder of temp folders) 2. Copies all files from the specified testDir into
   * the temp folder 3. Copies all student files from the specified student folder
   * 4. Compiles the code 5. Runs the code 6. Deletes the temp folder
   *
   * If there is a compile error, then a compile.log file is created in the
   * student folder that contains the Ststem.err stream If there is no compile
   * error, the an output.log file is created in the student folder taht contains
   * the System.out stream
   */
  private void test(File studentDir, File testDir, File tempFolderFolder) {
    if (null == tempFolderFolder) {
      compileAndRun(studentDir, studentDir);
    }
    else {
      copyAndCompileAndRun(studentDir, testDir, tempFolderFolder);
    }
  }

  /**
   * Create a temp folder inside tempFolderFolder to copy studentDir and testDir files into, then compile&run those files
   * Copy the output log files from the temp folder into studentDir
   * 
   * @param studentDir
   * @param testDir
   * @param tempFolderFolder
   */
  public void copyAndCompileAndRun(File studentDir, File testDir, File tempFolderFolder)
  {
    // Create temp folder to hold this student's files for compiling
    String tempDirName = tempFolderFolder.getPath() + File.separator + "temp-" + studentDir.getName() + "-"
    + System.currentTimeMillis();
    File tempDir = new File(tempDirName);

    try {
    // create temp dir for student files
    tempDir.mkdir();

    // copy files from testDir
    if (null != testDir) {
    for (File file : testDir.listFiles()) {
      File dest = new File(tempDir.getPath() + File.separator + file.getName());
      Files.copy(file.toPath(), dest.toPath());
    }
    }

    // copy files from studentDir
    for (File file : studentDir.listFiles()) {
    File dest = new File(tempDir.getPath() + File.separator + file.getName());
    Files.deleteIfExists(dest.toPath()); // If student has a file with same name as a test file, use the student version
    Files.copy(file.toPath(), dest.toPath());
    }

    // delete old compile log
    File compileLogDest = new File(studentDir.getPath() + File.separator + "compile.log");
    Files.deleteIfExists(compileLogDest.toPath());

    // delete old output log
    File outputLogDest = new File(studentDir.getPath() + File.separator + "output.log");
    Files.deleteIfExists(outputLogDest.toPath());

    // delete old error log
    File errLogDest = new File(studentDir.getPath() + File.separator + "error.log");
    Files.deleteIfExists(errLogDest.toPath());

    JavaRunner jr = compileAndRun(tempDir, studentDir);

    if (jr.wasLastCompileSuccessful()) {
    // copy output.log to studentDir
    File output = jr.getOutputLog();
    if (output.exists()) {
      try {
        Files.copy(output.toPath(), outputLogDest.toPath());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // copy error.log to studentDir
    File err = jr.getErrorLog();
    if (err.exists() && err.length() > 0) {
      try {
        Files.copy(err.toPath(), errLogDest.toPath());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    }
    else {
      // copy compile.log to studentDir
      File compileLog = jr.getCompileLog();
      if (compileLog.exists()) {
        Files.copy(compileLog.toPath(), compileLogDest.toPath());
      }
    }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // delete temp folder
      deleteDir(tempDir);
    }
  }

  /**
   * Compile and run the code in runCodeDir.
   * 
   * @param runCodeDir The folder that contains the Java code being executed (along with starter code, etc...)
   * @param studentSourceDir The folder that contains the student's original source files
   * @throws IOException
   */
  public JavaRunner compileAndRun(File runCodeDir, File studentSourceDir) {
    // Compile Files
    JavaRunner jr = getJavaRunner(runCodeDir);

    beforeCompile(jr, studentSourceDir);

    boolean successfulCompile = jr.compile();

    if (successfulCompile) 
    {
      // allow modifications to the JavaRunner before executing the code
      beforeExecute(jr, studentSourceDir);

      // Run the code!
      jr.execute(true);

      // process results
      if (jr.timedOut()) 
      {
        afterTimeoutError(jr, studentSourceDir);
      } 
      else {
        afterExecute(jr, studentSourceDir);
      }
    } 
    else // compile error
    {
      afterCompileError(jr, studentSourceDir);
    }

    afterEverything(studentSourceDir);

    return jr;
  }

  /** Recusively deletes the specified folder */
  public static boolean deleteDir(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDir(file);
      }
    }
    return directoryToBeDeleted.delete();
  }
}
