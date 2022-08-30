# Installation

1. Ensure the dependencies including `Eclipse IDE for Java Developers`, `Z3 pre-built binaries` (the home directory contains two subdirectories `bin` and `include`), and `JDK 8`.
2. Add the `bin` subdirectory of Z3 pre-built binaries, which contains several DLLs including `libz3java.dll`, to the PATH environment variable.
3. Launch Eclipse IDE and select an **empty** directory (to avoid conflicts) as Eclipse workspace.
4. Be sure that Eclipse is aware of your installed JDK and JRE environments (main menu, Windows > Preferences, then select Java > Installed JREs, and add all the home directories of all the JDK and JRE environments you have installed). Note that select the **JDK of version 8** to be the default JRE.
5. Set the Java home for Gradle to be the home directory of the installed JDK 8 (main menu, Windows > Preferences, then select Gradle and fill the "Java home" in the option group "Advanced Options").
6. Clone MSeqSynth git repository into the pre-selected Eclipse workspace directory.
7. Be sure that an Z3 executable `z3.exe` is available (can be found in the `bin` subdirectory of Z3 pre-built binaries that you have downloaded). You need to modify line 60 of class `mseqsynth.common.settings.Options` in the source directory `src/main/java` of project MSeqSynth, and assign the variable `this.solverExecPath` with the path to the Z3 executable on your development machine.
8. Copy the file `com.microsoft.z3.jar` (can be found in the `bin` subdirectory of Z3 pre-built binaries) into the `libs` subdirectory of MSeqSynth's home directory, and replace the existing jar.
9. From the main menu select File > Import. In the "Select an Import Wizard" window that pops up choose the Gradle > Existing Gradle Project wizard and press the Next button twice. In the "Import Gradle Project window" that is displayed, enter in the Project root directory field the path to the MSeqSynth cloned git repository, and then press the Finish button to confirm. Now your Eclipse workspace should have a Java project named `MSeqSynth`.
10. In the Gradle Tasks view, double-click on the MSeqSynth > build > build task to build the whole MSeqSynth project.

