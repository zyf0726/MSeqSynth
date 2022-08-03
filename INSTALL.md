## Working under Eclipse on Windows 10

1. Clone the MSeqSynth git repository.
2. To avoid conflicts we advise to import MSeqSynth (with JBSE) under an empty workspace.
3. JBSE uses the reserved `sun.misc.Unsafe` class, a thing that Eclipse forbids by default. To avoid Eclipse complaining about that you must modify the workspace preferences as follows: From the main menu choose Window > Preferences. On the left panel select Java > Compiler > Errors/Warnings, then on the right panel open the option group "Deprecated and restricted API", and for the option "Forbidden reference (access rules)" select the value "Warning" or "Info" or "Ignore".
4. Be sure that Eclipse is aware of your installed JDK and JRE environments (main menu, Windows > Preferences, then select Java > Installed JREs, and add all the home directories of all the JDK and JRE environments you have installed). Note that select the JDK of version 8 to be the default JRE.
5. Set the Java home for Gradle to be the home directory of the installed JDK version 8 (main menu, Windows > Preferences, then select Gradle and fill the "Java home" in the option group "Advanced Options").
6. From the main menu select File > Import. In the "Select an Import Wizard" window that pops up choose the Gradle > Existing Gradle Project wizard and press the Next button twice. In the "Import Gradle Project window" that is displayed, enter in the Project root directory field the path to the MSeqSynth cloned git repository, and then press the Finish button to confirm. Now your workspace should have two Java projects named `jbse` and `MSeqSynth`.
7. Be sure that an Z3 executable `z3.exe` is available (download from  [https://github.com/Z3Prover/z3/releases](https://github.com/Z3Prover/z3/releases)). You need to modify line 49 of class `jbse.dec.DecisionProcedureTest` in the source directory `src/test/java` of project jbse, and assign the variable `SMT_SOLVER_PATH` with the path to the Z3 executable on your development machine. So does line 60 of class `mseqsynth.common.settings.JBSEParameters` in the source directory `src/main/java` of project MSeqSynth.
8. Unfortunately the Buildship Gradle plugin is not completely able to configure the imported JBSE project automatically. As a consequence, after the import you will see some compilation errors due to the fact that the project did not generate some source files yet. Fix the situation as follows: In the Gradle Tasks view double-click on the MSeqSynth > jbse > build > build task to build JBSE with Gradle for the first time. Then, right-click the jbse project in the Package Explorer, and in the contextual menu that pops up select Gradle > Refresh Gradle Project. After that, you should see no more errors. From this moment you can build the whole MSeqSynth project by double clicking on the MSeqSynth > build > build task in the Gradle Task view.
9. After building the project MSeqSynth, you can try to run several examples in the package `examples` (e.g., `examples.demo.NodeLauncher`) and see whether it synthesizes a method call sequence satisfying the following specification:
   - `o` is the input parameter of the method under test `@DemoTest`,
   - `o`, `o.next`, and `o.next.next` are fresh objects,
   - `o.next.next.next` is null,
   - `o.value` - `o.next.value` = 100,
   - `o.next.value` - `o.next.next.value` = 200,
   - `o.value` + `o.next.next.value` = 800

