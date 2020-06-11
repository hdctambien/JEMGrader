# JEM Grader
> A CLI to automatically grade student java programming assignments

Most unit testing frameworks are designed with that idea that there is only one version of a project that needs to be built & tested. However, when teaching computer science you will have many versions of the same project, all with the same file names and all programmed to different levels of correctness.

This project is designed to compile/run/evaluate a project several times and compile the results into a single csv file.

## Features

This project makes it easy to:
* JUnit Testing
* Output Testing

## Structure of Student Code

If your students wrote their code in the file Foo.java then JEM Grader expects the student files to be stored in the following structure:

```
/Path/To/Labs/LabName/StudentName1/Foo.java
                     /StudentName2/Foo.java
                     /StudentName2/Foo.java
```

The `labPath` in this case would be `/Path/To/LabName`

The `testFile` would be `Foo`

Conveniently, this folder structure is exactly the same as how the [Turn CS In](https://github.com/hdctambien/turncsin) web application stores student assignments.

## Output Testing

You can grade assignments by comparing their output to an expected output file.

This method requires a text file that contains the expected output.

```
/Path/To/Tests/LabName/Foo.out
```

The `testPath` in this case would be `/Path/To/Tests/LabName`

If there are any other starter-code files that the student's project requires, you should put those in this folder as well.

```
java -jar UILGrader.jar labPath testPath testFile [timeout] > grades.csv
```

You can optionally set the `timeout` which limits how long to let student programs run (in milliseconds). This is used primarily to mitigate infinite-loops in student code but it will also catch algorithms with horrific performance. Default: 5000ms (5 seconds)

The results of the tests will be stored in the grades.csv file. You can name this file whatever you like.

Each student file will be assigned one of the following grades:

* P - Output matched expected output (empty last lines are ignored)
* F - Output did not match expected output (this includes runtime errors)
* C - Compilation Error
* T - Program timed out

> This program is named UILGrader because this is the method we use to access the correctness of the hands-on portion of UIL programming competitions.

## JUnit Testing

This uses JUnit 4.13 to test student code. You will need to put your UnitTest java file and any starter-code in a folder.

```
/Path/To/Tests/LabName/FooTest.java
```

The `testPath` in this case would be `/Path/To/Tests/LabName`

The testFile would be `FooTest`

```
java -jar JUnitGrader.jar labPath testPath testFile [timeout] > grades.csv
```

See `Output Testing` for details about the `timeout` argument.

The results of the tests will be stored in the grades.csv file. You can name this file whatever you like.

Each student file will be assigned several data points: #Pass, #Fail, and %Pass. In several situations the #Pass field will be populated with a letter.

* C - Compilation error
* T - Timeout error

## Log Files

When you grade your student's assignments, several log files can be created in your folder of student files. You can use these to validate/understand student grades.

* output.log - The output produced by this program (this includes JUnit test data if using JUnitGrader)
* compile.log - Any compile errors produced by this program (if no compile errors, this file isn't created)

## Licensing

This project is licensed under MIT license. A short and simple permissive license with conditions only requiring preservation of copyright and license notices. Licensed works, modifications, and larger works may be distributed under different terms and without source code.
