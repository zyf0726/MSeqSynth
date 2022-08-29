# MSeqSynth

## About

MSeqSynth is a research prototype that synthesizes sequences of calls to the public methods (and constructors) in data structure classes, such that the method call sequence constructs a target heap state satisfying a given specification.

MSeqSynth can be useful in both test generation tasks and (bounded) verification tasks. In the task of test generation, the synthesized call sequence could be used as a unit test for covering specific program branches or triggering specific types of errors. In the task of verification, the synthesized call sequence could be regard as a witness for determining the existence of a *reachable* heap state satisfying the given specification.

## Dependencies

MSeqSynth must be run on a Windows 10 machine currently, and has several dependencies as follows:

- [Eclipse IDE for Java Developers](https://www.eclipse.org/downloads/packages/release/2022-06/r/eclipse-ide-java-developers) (of version 2022-06) is used for building and running MSeqSynth
- [Z3](https://github.com/Z3Prover/z3) with its Java APIs is used for solving constraints
- [JBSE](https://github.com/pietrobraione/jbse) is used as a symbolic execution engine for Java programs
- [Java 8](https://www.oracle.com/java/technologies/java8.html) because JBSE must be built and run on a JDK version 8 - neither less, nor more
- [Javassist](http://www.javassist.org/) is used by JBSE for all the bytecode manipulation tasks

A compiled jar of customized JBSE (`jbse-0.10.0-SNAPSHOT.jar`) and a patched version of Javassist (`javassist.jar`) are included in the `libs` subdirectory of MSeqSynth's home directory. These two dependencies can be automatically resolved by Gradle and included in the build path.

To satisfy the remaining dependencies, you might need to download and unzip/install the following files:

- `Eclipse IDE for Java Developers` from https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/2022-06/R/eclipse-java-2022-06-R-win32-x86_64.zip
- `Z3 pre-built binaries` from https://github.com/Z3Prover/z3/releases/download/z3-4.11.0/z3-4.11.0-x64-win.zip
- `JDK 8` from https://www.oracle.com/java/technologies/downloads/#java8-windows

## Installation

See [INSTALL.md](./INSTALL.md).

## Usage Examples

After building the project MSeqSynth, you can try to run several examples in the package `examples`. For example, run `examples.demo.NodeLauncher` and see whether it synthesizes (and outputs in the console) a method call sequence satisfying the following specification:
- `o` is the input parameter of the method under test `@DemoTest`,
- `o`, `o.next`, and `o.next.next` are fresh objects,
- `o.next.next.next` is null,
- `o.value` - `o.next.value` = 100,
- `o.next.value` - `o.next.next.value` = 200,
- `o.value` + `o.next.next.value` = 800

Here is an example output sequence:

```
Node o0 = create(225, false)
Node o1 = addBefore(o0, 550)
addAfter(o0, 250)
@DemoTest(o1)
```