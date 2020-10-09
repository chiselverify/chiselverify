doit:
	sbt test

clean:
	git clean -fd

jni:
# Switch to native project to access native files
# Generate headers with "sbt javah"
# Create Cmakefile with "sbt nativeInit"
# Compile shared object file with "sbt nativeCompile"
	sbt "project native; javah; nativeInit cmake chisel-uvm; nativeCompile"